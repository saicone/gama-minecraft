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

import com.saicone.bukkit.module.placeholder.BridgePlaceholderProvider;
import com.saicone.bukkit.module.placeholder.MappedPlaceholderProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class DelegatePlaceholderProvider<T> implements BridgePlaceholderProvider<T> {

    private final MappedPlaceholderProvider<?> delegate;

    public DelegatePlaceholderProvider(@NotNull MappedPlaceholderProvider<?> delegate) {
        this.delegate = delegate;
    }

    @NotNull
    public MappedPlaceholderProvider<?> delegate() {
        return delegate;
    }

    @Override
    public @Nullable Function<T, Object> getStatic(@NotNull String key) {
        return getStatic(this.delegate, key);
    }

    @Override
    public @Nullable Object getStatic(T t, @NotNull String key) {
        return getStatic(this.delegate, t, key);
    }

    @Override
    public @Nullable Object getDynamic(T t, @NotNull String context) {
        return getDynamic(this.delegate, t, context);
    }

    @Override
    public @Nullable Object parseParameters(T t, @NotNull String parameters) {
        return parseParameters(this.delegate, t, parameters);
    }
}
