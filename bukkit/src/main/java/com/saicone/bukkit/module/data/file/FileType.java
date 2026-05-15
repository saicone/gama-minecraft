/*
 * MIT License.
 *
 * Copyright (c) 2025-2026 Rubenicos
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
package com.saicone.bukkit.module.data.file;

import com.saicone.settings.SettingsSource;
import com.saicone.bukkit.module.data.ClientType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public enum FileType implements ClientType {

    HOCON(
            "conf",
            "com.saicone.settings.source.HoconSettingsSource",
            "com{}saicone:settings-hocon:1.0.6",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    ),
    JSON(
            "json",
            "com.saicone.settings.source.GsonSettingsSource",
            "com{}saicone:settings-gson:1.0.6",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    ),
    TOML(
            "toml",
            "com.saicone.settings.source.TomlSettingsSource",
            "com{}saicone:settings-toml:1.0.6",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    ),
    YAML(
            "yml",
            "com.saicone.settings.source.YamlSettingsSource",
            "com{}saicone:settings-yaml:1.0.6",
            Map.of("com{}saicone{}types", "com.saicone.types",
                    "com{}saicone{}settings", "com.saicone.settings")
    );

    public static final FileType[] VALUES = values();

    private final String extension;
    private final String source;
    private final String dependency;
    private final Map<String, String> relocations;

    FileType(@NotNull String extension, @NotNull String source, @NotNull String dependency, @NotNull Map<String, String> relocations) {
        this.extension = extension;
        this.source = source;

        this.dependency = dependency.replace("{}", ".");

        this.relocations = new HashMap<>();
        relocations.forEach((key, value) -> {
            final String s = key.replace("{}", ".");
            if (!s.equals(value)) {
                this.relocations.put(s, value);
            }
        });
    }

    @Override
    public boolean isDependencyPresent() {
        try {
            Class.forName(source);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public @NotNull String getName() {
        return name();
    }

    @NotNull
    public String getExtension() {
        return extension;
    }

    @NotNull
    public String getSource() {
        return source;
    }

    @Override
    public @NotNull String getDependency() {
        return dependency;
    }

    @Override
    public @NotNull Map<String, String> getRelocations() {
        return relocations;
    }

    @NotNull
    public Path getFolder(@NotNull Path parent) {
        return parent.resolve(name().toLowerCase());
    }

    @NotNull
    public SettingsSource createSource() {
        try {
            return (SettingsSource) Class.forName(source).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException("Cannot initialize the SettingsSource " + source + " with reflection", e);
        }
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static FileType of(@NotNull String name, @Nullable FileType def) {
        for (FileType value : VALUES) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return def;
    }
}
