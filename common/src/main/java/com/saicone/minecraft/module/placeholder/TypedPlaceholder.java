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

public interface TypedPlaceholder<T> extends Placeholder<T> {

    @NotNull
    Class<T> type();

    default boolean acceptNull() {
        return true;
    }

    @Nullable
    default T parse(@Nullable Object object) {
        if (object == null) {
            return null;
        }

        return type().cast(object);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    default <A> TypedPlaceholder<A> as(@NotNull Class<A> type) {
        if (type.equals(this.type()) || type.isAssignableFrom(this.type())) {
            return (TypedPlaceholder<A>) this;
        }

        return new Delegate<>(type, this);
    }

    interface Bridge<T> extends Placeholder<T> {

        @Nullable
        default <A> Object get(@NotNull TypedPlaceholder<A> placeholder, @Nullable T t) {
            final A a = placeholder.parse(t);
            if (a == null && !placeholder.acceptNull()) {
                return null;
            }

            return placeholder.get(a);
        }

        @Nullable
        default <A> Object get(@NotNull TypedPlaceholder<A> placeholder, @Nullable T t, @NotNull String parameters) {
            final A a = placeholder.parse(t);
            if (a == null && !placeholder.acceptNull()) {
                return null;
            }

            return placeholder.get(a, parameters);
        }

        @Nullable
        default <A> Object get(@NotNull TypedPlaceholder<A> placeholder, @Nullable T t, @NotNull Iterator<String> args) {
            final A a = placeholder.parse(t);
            if (a == null && !placeholder.acceptNull()) {
                return null;
            }

            return placeholder.get(a, args);
        }
    }

    class Delegate<T> implements TypedPlaceholder<T>, Bridge<T> {

        private final Class<T> type;
        private final TypedPlaceholder<?> delegate;

        public Delegate(@NotNull Class<T> type, @NotNull TypedPlaceholder<?> delegate) {
            this.type = type;
            this.delegate = delegate;
        }

        @Override
        public @NotNull Class<T> type() {
            return type;
        }

        @Override
        public boolean acceptNull() {
            return delegate.acceptNull();
        }

        @NotNull
        public TypedPlaceholder<?> delegate() {
            return delegate;
        }

        @Override
        public @Nullable Object get(T t) {
            return get(this.delegate, t);
        }

        @Override
        public @Nullable Object get(T t, @NotNull String parameters) {
            return get(this.delegate, t, parameters);
        }

        @Override
        public @Nullable Object get(T t, @NotNull Iterator<String> args) {
            return get(this.delegate, t, args);
        }
    }
}
