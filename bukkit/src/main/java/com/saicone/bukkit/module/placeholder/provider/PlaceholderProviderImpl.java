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
package com.saicone.bukkit.module.placeholder.provider;

import com.saicone.bukkit.module.placeholder.MappedPlaceholderProvider;
import com.saicone.bukkit.module.placeholder.PlaceholderProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PlaceholderProviderImpl<T> implements PlaceholderProvider<T> {

    private final Map<String, Function<T, Object>> staticPlaceholder;

    public PlaceholderProviderImpl() {
        this(new HashMap<>());
    }

    public PlaceholderProviderImpl(@NotNull Map<String, Function<T, Object>> staticPlaceholder) {
        this.staticPlaceholder = staticPlaceholder;
    }

    public void add(@NotNull String key, @NotNull Object object) {
        this.staticPlaceholder.put(key, t -> object);
    }

    public void add(@NotNull String key, @NotNull Supplier<Object> supplier) {
        this.staticPlaceholder.put(key, t -> supplier.get());
    }

    public void add(@NotNull String key, @NotNull Function<T, Object> function) {
        Function<T, Object> finalFunction = function;
        if (this instanceof MappedPlaceholderProvider<?> && ((MappedPlaceholderProvider<?>) this).acceptGlobal()) {
            finalFunction = t -> {
                if (t == null) {
                    return null;
                }
                return function.apply(t);
            };
        }
        this.staticPlaceholder.put(key, finalFunction);
    }

    public void remove(@NotNull String key) {
        this.staticPlaceholder.remove(key);
    }

    public void removeIf(@NotNull Predicate<String> predicate) {
        this.staticPlaceholder.keySet().removeIf(predicate);
    }

    @Override
    public @Nullable Function<T, Object> getStatic(@NotNull String key) {
        return staticPlaceholder.get(key);
    }

    @Override
    public @Nullable Object getStatic(T t, @NotNull String key) {
        if (!this.staticPlaceholder.isEmpty()) {
            final Function<T, Object> function = this.staticPlaceholder.get(key);
            if (function != null) {
                return function.apply(t);
            }
        }
        return null;
    }

    @Override
    public @Nullable Object getDynamic(T t, @NotNull String context) {
        return null;
    }

    @Override
    public @Nullable Object parseParameters(T t, @NotNull String parameters) {
        if (!this.staticPlaceholder.isEmpty()) {
            final Function<T, Object> function = this.staticPlaceholder.get(parameters);
            if (function != null) {
                return function.apply(t);
            }
        }
        return getDynamic(t, parameters);
    }
}
