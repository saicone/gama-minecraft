/*
 * MIT License.
 *
 * Copyright (c) 2026 Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.gama.minecraft.module.data.client;

import com.saicone.gama.minecraft.module.data.DataClient;
import com.saicone.gama.minecraft.module.data.sql.SqlConnection;
import com.saicone.gama.minecraft.module.data.sql.SqlType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class SimpleSqlClient implements DataClient {

    // config
    protected SqlType defaultType = SqlType.MYSQL;
    private SqlType type;
    private String url;
    private String username;
    private String password;
    // connection
    private SqlConnection connection;
    // replacements
    protected String prefix = "";

    @NotNull
    protected abstract Path directory();

    @NotNull
    public SqlType type() {
        return type;
    }

    @NotNull
    public SqlConnection connection() {
        return connection;
    }

    @Override
    public void load(@NotNull Map<String, Object> config) {
        this.type = string(config, "type")
                .map(s -> SqlType.of(s, null))
                .orElseThrow(() -> new IllegalArgumentException("The key 'type' in configuration is missing or invalid"));

        this.prefix = string(config, "table-prefix").orElseGet(() -> string(config, "prefix").orElse(""));

        if (this.type.isExternal()) {
            // url parameters
            final String address = string(config, "address").orElseGet(() -> {
                final String host = string(config, "host").orElse(null);
                final Integer port = integer(config, "port").orElse(null);
                if (host == null) {
                    return "localhost:3306";
                } else if (port == null) {
                    return host + ":3306";
                } else {
                    return host + ":" + port;
                }
            });
            final String database = string(config, "database").orElseThrow(() -> new IllegalArgumentException("The key 'database' in configuration is missing"));
            final Set<String> flags = flags(config, "flags").orElseThrow(() -> new IllegalArgumentException("The key 'flags' in configuration is missing"));
            // connection information
            this.url = this.type.getUrl(address, database, flags);
            this.username = string(config, "username").orElse("root");
            this.password = string(config, "password").orElse("");
        } else {
            // url parameters
            final String strPath = string(config, "path").orElse(directory().resolve(this.type.name().toLowerCase()).toString());
            final Path path = Path.of(strPath);
            if (strPath.contains(File.separator) && !Files.exists(path.getParent())) {
                try {
                    Files.createDirectories(path.getParent());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create directories for database path: " + path.getParent(), e);
                }
            }
            // connection information
            this.url = this.type.getUrl(strPath);
            this.username = null;
            this.password = null;
        }

        onLoad();
    }

    @Override
    public void start() {
        boolean useHikari = false;
        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
            useHikari = true;
        } catch (Throwable ignored) { }

        if (useHikari) {
            final HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(this.type.getDriver());
            hikariConfig.setJdbcUrl(this.url);
            if (this.username != null && this.password != null) {
                hikariConfig.setUsername(this.username);
                hikariConfig.setPassword(this.password);
            }

            this.connection = SqlConnection.valueOf(new HikariDataSource(hikariConfig));
        } else {
            try {
                if (this.username != null && this.password != null) {
                    this.connection = SqlConnection.valueOf(DriverManager.getConnection(this.url, this.username, this.password));
                } else {
                    this.connection = SqlConnection.valueOf(DriverManager.getConnection(this.url));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to establish SQL connection on client start", e);
            }
        }

        onStart();
    }

    @Override
    public void close() {
        onClose();

        try {
            this.connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close SQL connection on client close", e);
        }
    }

    protected void onLoad() {
        // empty method
    }

    protected void onStart() {
        // empty method
    }

    protected void onClose() {
        // empty method
    }

    @NotNull
    protected String schema(@NotNull Map<SqlType, String> map, @NotNull Object... args) {
        String s = map.get(this.type);
        if (s == null) {
            s = map.get(this.defaultType);
            // PostgreSQL compatibility
            if (this.type == SqlType.POSTGRESQL && this.defaultType != SqlType.POSTGRESQL) {
                s = s.replace("`", "\"");
            }
        }
        if (s == null) {
            throw new IllegalArgumentException("The schema for type " + this.type.name() + " is missing");
        }

        return parse(s, args);
    }

    @NotNull
    protected String parse(@NotNull String s, @NotNull Object... args) {
        s = s.replace("{prefix}", this.prefix);
        for (int i = 0; i + 1 < args.length; i += 2) {
            s = s.replace(String.valueOf(args[i]), String.valueOf(args[i + 1]));
        }
        return s;
    }

    protected void connect(@NotNull SqlConnection.SqlConsumer consumer) {
        this.connection.connect(consumer);
    }

    protected <R> R connect(@NotNull SqlConnection.SqlFunction<R> consumer) {
        return this.connection.connect(consumer);
    }

    protected boolean isTablePresent(@NotNull String tableName) {
        return connect(con -> {
            return isTablePresent(con, tableName);
        });
    }

    protected boolean isTablePresent(@NotNull Connection con, @NotNull String tableName) throws SQLException {
        try (ResultSet set = this.type == SqlType.POSTGRESQL
                ? con.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })
                : con.getMetaData().getTables(con.getCatalog(), null, "%", null)) {
            while (set.next()) {
                if (set.getString(3).equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    private static Optional<Object> any(@NotNull Map<String, Object> config, @NotNull String key) {
        final String s = key.replace("_", "").replace("-", "");
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getKey().replace("_", "").replace("-", "").equalsIgnoreCase(s)) {
                return Optional.ofNullable(entry.getValue());
            }
        }

        return Optional.empty();
    }

    @NotNull
    private static Optional<String> string(@NotNull Map<String, Object> config, @NotNull String key) {
        return any(config, key).map(Object::toString);
    }

    @NotNull
    private static Optional<Integer> integer(@NotNull Map<String, Object> config, @NotNull String key) {
        return any(config, key).map(object -> Integer.parseInt(object.toString()));
    }

    @NotNull
    private static Optional<Set<String>> flags(@NotNull Map<String, Object> config, @NotNull String key) {
        return any(config, key).map(object -> {
            final Set<String> flags = new HashSet<>();

            if (object instanceof Iterable<?>) {
                for (Object element : (Iterable<?>) object) {
                    flags.add(element.toString());
                }
            } else if (object.getClass().isArray()) {
                final int size = Array.getLength(object);
                for (int i = 0; i < size; i++) {
                    flags.add(Array.get(object, i).toString());
                }
            } else {
                flags.add(object.toString());
            }

            return flags;
        });
    }
}
