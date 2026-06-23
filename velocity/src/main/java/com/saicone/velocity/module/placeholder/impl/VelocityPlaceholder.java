/*
 * This file is part of PixelBuy, licensed under the MIT License
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
package com.saicone.velocity.module.placeholder.impl;

import com.saicone.minecraft.module.placeholder.impl.RegistrablePlaceholder;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class VelocityPlaceholder<T> extends RegistrablePlaceholder<T> {

    private final ProxyServer proxy;
    private final Object plugin;

    public VelocityPlaceholder(@NotNull ProxyServer server, @NotNull Object plugin) {
        this.proxy = server;
        this.plugin = plugin;

        final PluginContainer container = server.getPluginManager().ensurePluginContainer(plugin);
        final PluginDescription description = container.getDescription();
        this.names = Set.of(description.getId().toLowerCase());
        this.author = String.join(", ", description.getAuthors());
        this.version = description.getVersion().orElse("1.0");
    }

    @NotNull
    public ProxyServer proxy() {
        return proxy;
    }

    @NotNull
    public Object plugin() {
        return plugin;
    }
}
