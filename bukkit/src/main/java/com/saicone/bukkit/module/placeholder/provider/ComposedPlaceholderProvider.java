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
import com.saicone.bukkit.module.placeholder.PlaceholderProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class ComposedPlaceholderProvider<T> implements BridgePlaceholderProvider<T> {

    private final PlaceholderProvider<T> parent;
    private final List<MappedPlaceholderProvider<?>> children;

    public ComposedPlaceholderProvider(@NotNull PlaceholderProvider<T> parent, @NotNull List<MappedPlaceholderProvider<?>> children) {
        this.parent = parent;
        this.children = children;
    }

    @NotNull
    public PlaceholderProvider<T> parent() {
        return parent;
    }

    @NotNull
    public List<MappedPlaceholderProvider<?>> children() {
        return children;
    }

    @Override
    public @Nullable Function<T, Object> getStatic(@NotNull String key) {
        Function<T, Object> function = this.parent.getStatic(key);
        if (function == null) {
            for (MappedPlaceholderProvider<?> child : this.children) {
                function = getStatic(child, key);
                if (function != null) {
                    break;
                }
            }
        }
        return function;
    }

    @Override
    public @Nullable Object getStatic(T t, @NotNull String key) {
        Object result = this.parent.getStatic(t, key);
        if (result == null) {
            for (MappedPlaceholderProvider<?> child : this.children) {
                result = getStatic(child, t, key);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public @Nullable Object getDynamic(T t, @NotNull String context) {
        Object result = this.parent.getDynamic(t, context);
        if (result == null) {
            for (MappedPlaceholderProvider<?> child : this.children) {
                result = getDynamic(child, t, context);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public @Nullable Object parseParameters(T t, @NotNull String parameters) {
        Object result = this.parent.parseParameters(t, parameters);
        if (result == null) {
            for (MappedPlaceholderProvider<?> child : this.children) {
                result = parseParameters(child, t, parameters);
                if (result != null) {
                    break;
                }
            }
        }
        return result;
    }
}
