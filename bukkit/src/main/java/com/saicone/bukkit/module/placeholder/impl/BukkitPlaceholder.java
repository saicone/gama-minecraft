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
package com.saicone.bukkit.module.placeholder.impl;

import com.saicone.minecraft.module.placeholder.ComposedPlaceholder;
import com.saicone.minecraft.module.placeholder.NamedPlaceholder;
import com.saicone.minecraft.module.placeholder.PlaceholderProcessor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class BukkitPlaceholder<T> extends ComposedPlaceholder<T> implements NamedPlaceholder<T> {

    private final Plugin plugin;

    protected Set<String> names;
    protected String author;
    protected String version;

    private boolean registered;
    private final Set<PlaceholderProcessor> processors = new HashSet<>();

    @SuppressWarnings("deprecation")
    public BukkitPlaceholder(@NotNull Plugin plugin) {
        this.plugin = plugin;

        this.names = Set.of(plugin.getName().toLowerCase());
        this.author = String.join(", ", plugin.getDescription().getAuthors());
        this.version = plugin.getDescription().getVersion();
    }

    @NotNull
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public @NotNull Set<String> names() {
        return names;
    }

    @Override
    public @NotNull String author() {
        return author;
    }

    @Override
    public @NotNull String version() {
        return version;
    }

    public boolean registered() {
        return registered;
    }

    @NotNull
    public Set<PlaceholderProcessor> processors() {
        return processors;
    }

    public void register() {
        register(names());
    }

    public void register(@NotNull String... names) {
        register(Set.of(names));
    }

    public void register(@NotNull Collection<String> names) {
        register(names instanceof Set ? (Set<String>) names: new HashSet<>(names));
    }

    public void register(@NotNull Set<String> names) {
        if (this.registered) {
            unregister();
        }

        this.names = names;
        for (PlaceholderProcessor processor : this.processors) {
            processor.register(this);
        }
        this.registered = true;
    }

    public void unregister() {
        this.registered = false;
        for (PlaceholderProcessor processor : this.processors) {
            processor.unregister(this);
        }
    }
}
