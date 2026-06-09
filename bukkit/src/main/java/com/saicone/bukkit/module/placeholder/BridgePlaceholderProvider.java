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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface BridgePlaceholderProvider<T> extends PlaceholderProvider<T> {

    @Nullable
    default <A> Function<T, Object> getStatic(@NotNull MappedPlaceholderProvider<A> provider, @NotNull String key) {
        final Function<A, Object> function = provider.getStatic(key);
        if (function != null) {
            return t -> {
                final A a = provider.map(t);
                if (a == null && !provider.acceptGlobal()) {
                    return null;
                }
                return function.apply(a);
            };
        }
        return null;
    }

    @Nullable
    default <A> Object getStatic(@NotNull MappedPlaceholderProvider<A> provider, T t, @NotNull String key) {
        final A a = provider.map(t);
        if (a == null && !provider.acceptGlobal()) {
            return null;
        }
        return provider.getStatic(a, key);
    }

    @Nullable
    default <A> Object getDynamic(@NotNull MappedPlaceholderProvider<A> provider, T t, @NotNull String context) {
        final A a = provider.map(t);
        if (a == null && !provider.acceptGlobal()) {
            return null;
        }
        return provider.getDynamic(a, context);
    }

    @Nullable
    default <A> Object parseParameters(@NotNull MappedPlaceholderProvider<A> provider, T t, @NotNull String parameters) {
        final A a = provider.map(t);
        if (a == null && !provider.acceptGlobal()) {
            return null;
        }
        return provider.parseParameters(a, parameters);
    }
}
