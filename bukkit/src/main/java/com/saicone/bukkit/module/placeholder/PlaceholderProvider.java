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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface PlaceholderProvider<T> {

    @Nullable
    Function<T, Object> getStatic(@NotNull String key);

    @Nullable
    Object getStatic(T t, @NotNull String key);

    @Nullable
    Object getDynamic(T t, @NotNull String context);

    @Nullable
    Object parseParameters(T t, @NotNull String parameters);

    @Nullable
    @Contract("_, !null, _, _ -> !null")
    default String replaceInside(T t, @Nullable String s, char open, char close) {
        if (s == null) {
            return null;
        }

        final StringBuilder result = new StringBuilder();

        boolean inside = false;
        final StringBuilder parameters = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (inside) {
                if (i + 1 < s.length() && c == '\\' && s.charAt(i + 1) == close) {
                    parameters.append(close);
                    i++;
                } else if (c == close) {
                    inside = false;
                    final Object replaced = parseParameters(t, parameters.toString());
                    if (replaced != null) {
                        result.append(replaced);
                    } else {
                        result.append(open).append(parameters).append(close);
                    }
                    parameters.setLength(0);
                } else {
                    parameters.append(c);
                }
            } else if (c == open) {
                inside = true;
            } else {
                if (i + 1 < s.length() && c == '\\' && s.charAt(i + 1) == open) {
                    result.append(open);
                    i++;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }
}
