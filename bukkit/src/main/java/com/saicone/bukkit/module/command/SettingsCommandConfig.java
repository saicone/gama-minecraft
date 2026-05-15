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

import com.saicone.settings.node.MapNode;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SettingsCommandConfig {

    @NotNull
    public static BukkitCommandConfig valueOf(@NotNull MapNode config) {
        return new BukkitCommandConfig() {
            @Override
            public boolean register() {
                return config.get("register").asBoolean(true);
            }

            @Override
            public @NotNull Optional<String> name() {
                return Optional.ofNullable(config.get("name").asString());
            }

            @Override
            public @NotNull Set<String> aliases() {
                return config.get("aliases").asList(Types.STRING).stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            }

            @Override
            public @NotNull Optional<Object> permission() {
                return Optional.ofNullable(config.get("permission").getValue());
            }

            @Override
            public @NotNull Optional<String> delay() {
                return Optional.ofNullable(config.get("delay").asString());
            }

            @Override
            public @NotNull BukkitCommandConfig command(@NotNull String name) {
                return valueOf(config.get("sub", name).asMapNode());
            }
        };
    }

    SettingsCommandConfig() {
    }
}
