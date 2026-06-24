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
package com.saicone.minecraft.module.data.client;

import com.saicone.minecraft.module.data.sql.SqlType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public abstract class MapSqlClient<T> extends SimpleSqlClient implements MapDataClient {

    private static final Map<SqlType, String> CREATE_TABLE = Map.of(
            SqlType.MYSQL, """
                    CREATE TABLE `{prefix}{table_name}` (
                      `owner` VARCHAR(36)  NOT NULL,
                      `key`   VARCHAR(255) NOT NULL,
                      `value` {value_type} NOT NULL,
                      PRIMARY KEY (`owner`, `key`)
                    ) DEFAULT CHARSET = utf8mb4;
                    """,
            SqlType.POSTGRESQL, """
                    CREATE TABLE "{prefix}{table_name}" (
                      "owner" VARCHAR(36)  NOT NULL,
                      "key"   VARCHAR(255) NOT NULL,
                      "value" {value_type} NOT NULL,
                      PRIMARY KEY ("owner", "key")
                    );
                    """,
            SqlType.SQLITE, """
                    CREATE TABLE `{prefix}{table_name}` (
                      `owner` VARCHAR(36)  NOT NULL,
                      `key`   VARCHAR(255) NOT NULL,
                      `value` {value_type} NOT NULL,
                      PRIMARY KEY (`owner`, `key`)
                    );
                    """,
            SqlType.H2, """
                    CREATE TABLE `{prefix}{table_name}` (
                      `owner` VARCHAR(36)  NOT NULL,
                      `key`   VARCHAR(255) NOT NULL,
                      `value` {value_type} NOT NULL,
                      PRIMARY KEY (`owner`, `key`)
                    );
                    """
    );
    private static final Map<SqlType, String> SELECT_MAP = Map.of(
            SqlType.MYSQL, """
                    SELECT `key`, `value` FROM `{prefix}{table_name}` WHERE `owner` = ?;
                    """
    );
    private static final Map<SqlType, String> SELECT_MAP_VALUE = Map.of(
            SqlType.MYSQL, """
                    SELECT `value` FROM `{prefix}{table_name}` WHERE `owner` = ? AND `key` = ?;
                    """
    );
    private static final Map<SqlType, String> INSERT_MAP_ENTRY = Map.of(
            SqlType.MYSQL, """
                    INSERT INTO `{prefix}{table_name}` (
                      `owner`,
                      `key`,
                      `value`
                    ) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      `value` = VALUES(`value`);
                    """,
            SqlType.POSTGRESQL, """
                    INSERT INTO "{prefix}{table_name}" (
                      "owner",
                      "key",
                      "value"
                    ) VALUES (?, ?, ?)
                    ON CONFLICT ("owner", "key") DO UPDATE SET
                      "value" = EXCLUDED."value";
                    """,
            SqlType.SQLITE, """
                    INSERT OR REPLACE INTO `{prefix}{table_name}` (
                      `owner`,
                      `key`,
                      `value`
                    ) VALUES (?, ?, ?);
                    """,
            SqlType.H2, """
                    MERGE INTO `{prefix}{table_name}` (
                      `owner`,
                      `key`,
                      `value`
                    ) KEY (`owner`, `key`) VALUES (?, ?, ?);
                    """
    );
    private static final Map<SqlType, String> DELETE_MAP_ENTRY = Map.of(
            SqlType.MYSQL, """
                    DELETE FROM `{prefix}{table_name}` WHERE `owner` = ? AND `key` = ?;
                    """
    );

    private transient String createTable;
    private transient String selectMap;
    private transient String selectMapValue;
    private transient String insertMapEntry;
    private transient String deleteMapEntry;

    @NotNull
    protected String tableName() {
        return "metadata";
    }

    @NotNull
    protected abstract String valueColumnType(@NotNull SqlType type);

    @Nullable
    protected abstract T valueColumnGet(@NotNull ResultSet result, @NotNull String columnLabel) throws SQLException;

    protected abstract void valueColumnSet(@NotNull PreparedStatement statement, int index, @NotNull T value) throws SQLException;

    @NotNull
    public abstract T serialize(@NotNull String key, @NotNull Object value) throws IOException;

    @NotNull
    public abstract Object deserialize(@NotNull String key, @NotNull T value) throws IOException;

    @Override
    protected void onLoad() {
        final String tableName = tableName();
        final String valueType = valueColumnType(this.type());

        this.createTable = schema(CREATE_TABLE, "{table_name}", tableName, "{value_type}", valueType);
        this.selectMap = schema(SELECT_MAP, "{table_name}", tableName);
        this.selectMapValue = schema(SELECT_MAP_VALUE, "{table_name}", tableName);
        this.insertMapEntry = schema(INSERT_MAP_ENTRY, "{table_name}", tableName);
        this.deleteMapEntry = schema(DELETE_MAP_ENTRY, "{table_name}", tableName);
    }

    @Override
    protected void onStart() {
        connect(con -> {
            if (isTablePresent(con, this.prefix + tableName())) {
                return;
            }

            try (PreparedStatement stmt = con.prepareStatement(this.createTable)) {
                stmt.executeUpdate();
            }
        });
    }

    @Override
    public @NotNull Map<String, Object> loadMap(@NotNull UUID user) {
        return connect(con -> {
            final Map<String, Object> map = new LinkedHashMap<>();
            try (PreparedStatement stmt = con.prepareStatement(this.selectMap)) {
                stmt.setString(1, user.toString());

                try (ResultSet result = stmt.executeQuery()) {
                    while (result.next()) {
                        final String key = result.getString("key");
                        final T value = valueColumnGet(result, "value");
                        if (value != null) {
                            try {
                                map.put(key, deserialize(key, value));
                            } catch (IOException e) {
                                throw new SQLException("Failed to deserialize value for key " + key, e);
                            }
                        }
                    }
                }
            }
            return map;
        });
    }

    @Override
    public @Nullable Object loadMapValue(@NotNull UUID user, @NotNull String key) {
        return connect(con -> {
            try (PreparedStatement stmt = con.prepareStatement(this.selectMapValue)) {
                stmt.setString(1, user.toString());
                stmt.setString(2, key);

                try (ResultSet result = stmt.executeQuery()) {
                    if (result.next()) {
                        final T value = valueColumnGet(result, "value");
                        if (value != null) {
                            try {
                                return deserialize(key, value);
                            } catch (IOException e) {
                                throw new SQLException("Failed to deserialize value for key " + key, e);
                            }
                        }
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void saveMap(@NotNull UUID user, @NotNull Map<String, Object> map) {
        connect(con -> {
            try (PreparedStatement insertStmt = con.prepareStatement(this.insertMapEntry); PreparedStatement deleteStmt = con.prepareStatement(this.deleteMapEntry)) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    final String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value == null) {
                        deleteStmt.setString(1, user.toString());
                        deleteStmt.setString(2, key);

                        deleteStmt.addBatch();
                    } else {
                        final T serialized;
                        try {
                            serialized = serialize(key, value);
                        } catch (IOException e) {
                            throw new SQLException("Failed to serialize value for key " + key, e);
                        }

                        insertStmt.setString(1, user.toString());
                        insertStmt.setString(2, key);
                        valueColumnSet(insertStmt, 3, serialized);

                        insertStmt.addBatch();
                    }
                }

                insertStmt.executeBatch();
                deleteStmt.executeBatch();
            }
        });
    }

    @Override
    public void saveMapEntry(@NotNull UUID user, @NotNull String key, @Nullable Object value) {
        connect(con -> {
            if (value == null) {
                try (PreparedStatement stmt = con.prepareStatement(this.deleteMapEntry)) {
                    stmt.setString(1, user.toString());
                    stmt.setString(2, key);

                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = con.prepareStatement(this.insertMapEntry)) {
                    final T serialized;
                    try {
                        serialized = serialize(key, value);
                    } catch (IOException e) {
                        throw new SQLException("Failed to serialize value for key " + key, e);
                    }

                    stmt.setString(1, user.toString());
                    stmt.setString(2, key);
                    valueColumnSet(stmt, 3, serialized);

                    stmt.executeUpdate();
                }
            }
        });
    }

    public abstract static class Bytes extends MapSqlClient<byte[]> {
        @Override
        protected @NotNull String valueColumnType(@NotNull SqlType type) {
            switch (type) {
                case MYSQL:
                case MARIADB:
                    return "MEDIUMBLOB";
                case POSTGRESQL:
                    return "BYTEA";
                case SQLITE:
                case H2:
                    return "BLOB";
                default:
                    throw new IllegalArgumentException("Unsupported SQL type: " + type.name());
            }
        }

        @Override
        protected byte[] valueColumnGet(@NotNull ResultSet result, @NotNull String columnLabel) throws SQLException {
            return result.getBytes(columnLabel);
        }

        @Override
        protected void valueColumnSet(@NotNull PreparedStatement statement, int index, byte @NotNull [] value) throws SQLException {
            statement.setBytes(index, value);
        }

        @Override
        public byte @NotNull [] serialize(@NotNull String key, @NotNull Object value) throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ObjectOutputStream output = new ObjectOutputStream(out)) {
                output.writeObject(value);

                return out.toByteArray();
            }
        }

        @Override
        public @NotNull Object deserialize(@NotNull String key, byte @NotNull [] value) throws IOException {
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(value))) {
                return input.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    }

    public abstract static class Text extends MapSqlClient<String> {
        @Override
        protected @NotNull String valueColumnType(@NotNull SqlType type) {
            switch (type) {
                case MYSQL:
                case MARIADB:
                    return "MEDIUMTEXT";
                case POSTGRESQL:
                case SQLITE:
                    return "TEXT";
                case H2:
                    return "VARCHAR";
                default:
                    throw new IllegalArgumentException("Unsupported SQL type: " + type.name());
            }
        }

        @Override
        protected @Nullable String valueColumnGet(@NotNull ResultSet result, @NotNull String columnLabel) throws SQLException {
            return result.getString(columnLabel);
        }

        @Override
        protected void valueColumnSet(@NotNull PreparedStatement statement, int index, @NotNull String value) throws SQLException {
            statement.setString(index, value);
        }

        @Override
        public @NotNull String serialize(@NotNull String key, @NotNull Object value) throws IOException {
            return value.toString();
        }

        @Override
        public @NotNull Object deserialize(@NotNull String key, @NotNull String value) throws IOException {
            return value;
        }
    }
}
