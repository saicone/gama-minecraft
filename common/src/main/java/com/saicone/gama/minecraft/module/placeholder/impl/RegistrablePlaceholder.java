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
package com.saicone.gama.minecraft.module.placeholder.impl;

import com.saicone.gama.minecraft.module.placeholder.ComposedPlaceholder;
import com.saicone.gama.minecraft.module.placeholder.NamedPlaceholder;
import com.saicone.gama.minecraft.module.placeholder.Placeholder;
import com.saicone.gama.minecraft.module.placeholder.PlaceholderProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class RegistrablePlaceholder<T> extends ComposedPlaceholder<T> implements NamedPlaceholder<T> {

    protected Set<String> names = Set.of();
    protected String author = UUID.randomUUID().toString();
    protected String version = "1.0";

    private boolean registered;
    private final Set<PlaceholderProcessor> processors = new HashSet<>();

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
        if (registered()) {
            unregister();
        }

        this.names = names;
        register(this);
        for (Map.Entry<String, Placeholder<T>> entry : children().entrySet()) {
            if (entry.getValue() instanceof NamedPlaceholder<?>) {
                register((NamedPlaceholder<T>) entry.getValue());
            }
        }
        this.registered = true;
    }

    private void register(@NotNull NamedPlaceholder<T> placeholder) {
        if (placeholder.names().isEmpty()) {
            return;
        }
        for (PlaceholderProcessor processor : this.processors) {
            processor.register(placeholder);
        }
    }

    public void unregister() {
        this.registered = false;
        for (Map.Entry<String, Placeholder<T>> entry : children().entrySet()) {
            if (entry.getValue() instanceof NamedPlaceholder<?>) {
                unregister((NamedPlaceholder<T>) entry.getValue());
            }
        }
        unregister(this);
    }

    private void unregister(@NotNull NamedPlaceholder<T> placeholder) {
        for (PlaceholderProcessor processor : this.processors) {
            processor.unregister(placeholder);
        }
    }

    @Override
    public @Nullable Placeholder<T> remove(@NotNull String key) {
        final Placeholder<T> result = super.remove(key);
        if (registered() && result instanceof NamedPlaceholder<T>) {
            unregister((NamedPlaceholder<T>) result);
        }
        return result;
    }

    @Override
    public @Nullable Placeholder<T> put(@NotNull String key, @NotNull Placeholder<T> placeholder) {
        final Placeholder<T> result = super.put(key, placeholder);
        if (registered()) {
            if (result instanceof NamedPlaceholder<T>) {
                unregister((NamedPlaceholder<T>) result);
            }
            if (placeholder instanceof NamedPlaceholder<T>) {
                register((NamedPlaceholder<T>) placeholder);
            }
        }
        return result;
    }
}
