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
package com.saicone.bukkit.module.command;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface BukkitCommandConfig {

    BukkitCommandConfig EMPTY = new BukkitCommandConfig() { };

    @NotNull
    static BukkitCommandConfig valueOf(boolean register, @NotNull String name) {
        return new BukkitCommandConfig() {
            @Override
            public boolean register() {
                return register;
            }

            @Override
            public @NotNull Optional<String> name() {
                return Optional.of(name);
            }
        };
    }

    @NotNull
    static BukkitCommandConfig valueOf(@NotNull ConfigurationSection config) {
        return new BukkitCommandConfig() {
            @Override
            public boolean register() {
                return config.getBoolean("register", true);
            }

            @Override
            public @NotNull Optional<String> name() {
                return Optional.ofNullable(config.getString("name"));
            }

            @Override
            public @NotNull Set<String> aliases() {
                return config.getStringList("aliases").stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            }

            @Override
            public @NotNull Optional<Object> permission() {
                return Optional.ofNullable(config.get("permission"));
            }

            @Override
            public @NotNull Optional<String> delay() {
                return Optional.ofNullable(config.getString("delay"));
            }

            @Override
            public @NotNull BukkitCommandConfig command(@NotNull String name) {
                return valueOf(config.getConfigurationSection("sub." + name));
            }
        };
    }

    default boolean register() {
        return false;
    }

    @NotNull
    default Optional<String> name() {
        return Optional.empty();
    }

    @NotNull
    default Set<String> aliases() {
        return Set.of();
    }

    @NotNull
    default Optional<Object> permission() {
        return Optional.empty();
    }

    @NotNull
    default Optional<String> delay() {
        return Optional.empty();
    }

    @NotNull
    default BukkitCommandConfig command(@NotNull String name) {
        return EMPTY;
    }
}
