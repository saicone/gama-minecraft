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
package com.saicone.gama.bukkit.module.command;

import com.saicone.settings.SettingsNode;
import com.saicone.settings.node.MapNode;
import com.saicone.types.Types;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public class SettingsCommandConfig {

    @NotNull
    public static BukkitCommandConfig valueOf(@NotNull MapNode config) {
        return new BukkitCommandConfig() {
            @Override
            public Optional<Boolean> register() {
                return Types.BOOLEAN.optional(config.get("register").getValue());
            }

            @Override
            public @NotNull Optional<String> name() {
                return Optional.ofNullable(config.get("name").asString());
            }

            @Override
            public @NotNull Optional<Set<String>> aliases() {
                return Types.STRING.set().optional(config.get("aliases").getValue());
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
            public @NotNull Optional<BukkitCommandConfig> command(@NotNull String name) {
                final SettingsNode node = config.get("sub", name);
                if (node.isMap()) {
                    return Optional.of(valueOf(node.asMapNode()));
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    SettingsCommandConfig() {
    }
}
