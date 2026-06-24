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
package com.saicone.minecraft.module.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComposedPlaceholder<T> implements Placeholder<T> {

    private ComposedPlaceholder<T> parent;
    private final Map<String, Placeholder<T>> children;

    public ComposedPlaceholder() {
        this(null, new LinkedHashMap<>());
    }

    public ComposedPlaceholder(@Nullable ComposedPlaceholder<T> parent) {
        this(parent, new LinkedHashMap<>());
    }

    public ComposedPlaceholder(@NotNull Map<String, Placeholder<T>> children) {
        this(null, children);
    }

    public ComposedPlaceholder(@Nullable ComposedPlaceholder<T> parent, @NotNull Map<String, Placeholder<T>> children) {
        this.parent = parent;
        this.children = children;
    }

    @Nullable
    public ComposedPlaceholder<T> parent() {
        return parent;
    }

    @NotNull
    public Map<String, Placeholder<T>> children() {
        return children;
    }

    @Nullable
    public Placeholder<T> put(@NotNull String key, @Nullable Object value) {
        return put(key, Placeholder.value(value));
    }

    @Nullable
    public Placeholder<T> put(@NotNull String key, @NotNull Supplier<Object> function) {
        return put(key, t -> function.get());
    }

    @Nullable
    public Placeholder<T> put(@NotNull String key, @NotNull Function<T, Object> function) {
        return put(key, new Placeholder<T>() {
            @Override
            public @Nullable Object get(T t) {
                return function.apply(t);
            }
        });
    }

    @Nullable
    public Placeholder<T> put(@NotNull String key, @NotNull Placeholder<T> placeholder) {
        if (placeholder instanceof ComposedPlaceholder<?>) {
            ((ComposedPlaceholder<T>) placeholder).parent = this;
        }
        return this.children.put(key, placeholder);
    }

    @Override
    public @Nullable Object get(T t, @NotNull String parameters) {
        final Placeholder<T> placeholder = this.children.get(parameters);
        if (placeholder != null) {
            return placeholder.get(t);
        } else {
            for (Map.Entry<String, Placeholder<T>> entry : this.children.entrySet()) {
                final String key = entry.getKey();
                if (parameters.startsWith(key)) {
                    final char separator = parameters.charAt(key.length());
                    if (separator == '_' || separator == ':') {
                        return entry.getValue().get(t, parameters.substring(key.length() + 1));
                    }
                }
            }
            return null;
        }
    }

    @Override
    public @Nullable Object get(T t, @NotNull Iterator<String> args) {
        if (!args.hasNext()) {
            return null;
        }

        return get(args.next(), t, args);
    }

    @Nullable
    public Object get(@NotNull String key, @Nullable T t) {
        final Placeholder<T> placeholder = this.children.get(key);
        if (placeholder != null) {
            return placeholder.get(t);
        }

        return null;
    }

    @Nullable
    public Object get(@NotNull String key, @Nullable T t, @NotNull String parameters) {
        final Placeholder<T> placeholder = this.children.get(key);
        if (placeholder != null) {
            return placeholder.get(t, parameters);
        }

        return null;
    }

    @Nullable
    public Object get(@NotNull String key, @Nullable T t, @NotNull Iterator<String> args) {
        final Placeholder<T> placeholder = this.children.get(key);
        if (placeholder != null) {
            return placeholder.get(t, args);
        }

        return null;
    }
}
