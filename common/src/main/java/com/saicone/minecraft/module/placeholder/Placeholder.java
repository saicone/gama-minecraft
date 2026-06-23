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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public interface Placeholder<T> {

    @NotNull
    @SuppressWarnings("unchecked")
    static <T> Placeholder<T> value(@Nullable Object value) {
        if (value == null) {
            return (Placeholder<T>) Value.EMPTY;
        }
        return new Value<>(value);
    }

    @Nullable
    default Object get(T t) {
        return null;
    }

    @Nullable
    default Object get(T t, @NotNull String parameters) {
        return get(t);
    }

    @Nullable
    default Object get(T t, @NotNull Iterator<String> args) {
        if (args.hasNext()) {
            return get(t, args.next());
        } else {
            return get(t);
        }
    }

    @Nullable
    @Contract("_, !null, _, _ -> !null")
    @ApiStatus.NonExtendable
    default String replaceInside(@Nullable T t, @Nullable String s, char open, char close) {
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
                    final Object replaced = this.get(t, parameters.toString());
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

    class Value<T> implements Placeholder<T> {

        private static final Value<?> EMPTY = new Value<>(null);

        private final Object value;

        public Value(@Nullable Object value) {
            this.value = value;
        }

        @Nullable
        public Object value() {
            return value;
        }

        @Override
        public @Nullable Object get(T t) {
            return value;
        }
    }
}
