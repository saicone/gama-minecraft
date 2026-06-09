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
package com.saicone.bukkit.module.placeholder;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PluginPlaceholder<T> {

    private final Plugin plugin;
    private final Class<T> type;

    private Set<String> names = Set.of();
    private Placeholders.Provider<T> provider;
    private final Set<Placeholders.Processor> processors = new HashSet<>();

    public PluginPlaceholder(@NotNull Plugin plugin, @NotNull Class<T> type) {
        this(plugin, type, null);
    }

    public PluginPlaceholder(@NotNull Plugin plugin, @NotNull Class<T> type, @Nullable Placeholders.Provider<T> provider) {
        this.plugin = plugin;
        this.type = type;
        this.provider = provider;
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @NotNull
    public Class<T> getType() {
        return type;
    }

    @NotNull
    public Set<String> getNames() {
        return Collections.unmodifiableSet(names);
    }

    public Placeholders.Provider<T> getProvider() {
        return provider;
    }

    @NotNull
    public Set<Placeholders.Processor> getProcessors() {
        return processors;
    }

    public void put(@NotNull Placeholders.Provider<T> provider) {
        if (this.provider != null && this.provider instanceof Placeholders.ComposedProvider<T>) {
            this.provider = new Placeholders.ComposedProvider<>(provider, ((Placeholders.ComposedProvider<T>) this.provider).children());
        } else {
            this.provider = provider;
        }
    }

    public void putMapped(@NotNull Placeholders.MappedProvider<?> provider) {
        if (!(this.provider instanceof Placeholders.ComposedProvider<T>)) {
            this.provider = new Placeholders.ComposedProvider<>(this.provider, new ArrayList<>());
        }
        ((Placeholders.ComposedProvider<T>) this.provider).children().add(provider);
    }

    public void processors(@Nullable Placeholders.Processor... processors) {
        this.processors.clear();
        if (processors != null) {
            Collections.addAll(this.processors, processors);
        }
    }

    public void register(@NotNull String... names) {
        register(Set.of(names));
    }

    public void register(@NotNull Collection<String> names) {
        if (!this.names.isEmpty()) {
            unregister();
        }
        this.names = new HashSet<>(names);
        for (Placeholders.Processor processor : this.processors) {
            processor.register(this);
        }
    }

    public void unregister() {
        for (Placeholders.Processor processor : this.processors) {
            processor.unregister(this);
        }
    }
}
