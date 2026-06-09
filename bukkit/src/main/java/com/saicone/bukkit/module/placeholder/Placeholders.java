/*
 * This file is part of PixelBuy, licensed under the MIT License
 *
 * Copyright (c) 2024-2026 Rubenicos
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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Placeholders {

    @NotNull
    public static PAPIProcessor papi() {
        return PAPIProcessor.INSTANCE;
    }

    public interface Provider<T> {

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

    public interface MappedProvider<T> extends Provider<T> {

        @NotNull
        Class<T> getType();

        default boolean acceptGlobal() {
            return true;
        }

        @Nullable
        T map(@NotNull Object object);
    }

    public interface BridgeProvider<T> extends Provider<T> {

        @Nullable
        default <A> Function<T, Object> getStatic(@NotNull MappedProvider<A> provider, @NotNull String key) {
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
        default <A> Object getStatic(@NotNull MappedProvider<A> provider, T t, @NotNull String key) {
            final A a = provider.map(t);
            if (a == null && !provider.acceptGlobal()) {
                return null;
            }
            return provider.getStatic(a, key);
        }

        @Nullable
        default <A> Object getDynamic(@NotNull MappedProvider<A> provider, T t, @NotNull String context) {
            final A a = provider.map(t);
            if (a == null && !provider.acceptGlobal()) {
                return null;
            }
            return provider.getDynamic(a, context);
        }

        @Nullable
        default <A> Object parseParameters(@NotNull MappedProvider<A> provider, T t, @NotNull String parameters) {
            final A a = provider.map(t);
            if (a == null && !provider.acceptGlobal()) {
                return null;
            }
            return provider.parseParameters(a, parameters);
        }
    }

    public static class ProviderImpl<T> implements Provider<T> {

        private final Map<String, Function<T, Object>> staticPlaceholder;

        public ProviderImpl() {
            this(new HashMap<>());
        }

        public ProviderImpl(@NotNull Map<String, Function<T, Object>> staticPlaceholder) {
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
            if (this instanceof MappedProvider<?> && ((MappedProvider<?>) this).acceptGlobal()) {
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

    public abstract static class MappedProviderImpl<T> extends ProviderImpl<T> implements MappedProvider<T> {

        public MappedProviderImpl() {
            this(new HashMap<>());
        }

        public MappedProviderImpl(@NotNull Map<String, Function<T, Object>> staticPlaceholder) {
            super(staticPlaceholder);
        }
    }

    public static class DelegateProvider<T> implements BridgeProvider<T> {

        private final MappedProvider<?> delegate;

        public DelegateProvider(@NotNull MappedProvider<?> delegate) {
            this.delegate = delegate;
        }

        @NotNull
        public MappedProvider<?> delegate() {
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

    public static class ComposedProvider<T> implements BridgeProvider<T> {

        private final Provider<T> parent;
        private final List<MappedProvider<?>> children;

        public ComposedProvider(@NotNull Placeholders.Provider<T> parent, @NotNull List<MappedProvider<?>> children) {
            this.parent = parent;
            this.children = children;
        }

        @NotNull
        public Provider<T> parent() {
            return parent;
        }

        @NotNull
        public List<MappedProvider<?>> children() {
            return children;
        }

        @Override
        public @Nullable Function<T, Object> getStatic(@NotNull String key) {
            Function<T, Object> function = this.parent.getStatic(key);
            if (function == null) {
                for (MappedProvider<?> child : this.children) {
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
                for (MappedProvider<?> child : this.children) {
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
                for (MappedProvider<?> child : this.children) {
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
                for (MappedProvider<?> child : this.children) {
                    result = parseParameters(child, t, parameters);
                    if (result != null) {
                        break;
                    }
                }
            }
            return result;
        }
    }

    public static class OfflineProvider extends MappedProviderImpl<OfflinePlayer> {

        public OfflineProvider() {
            this(new HashMap<>());
        }

        public OfflineProvider(@NotNull Map<String, Function<OfflinePlayer, Object>> staticPlaceholder) {
            super(staticPlaceholder);
        }

        @Override
        public @NotNull Class<OfflinePlayer> getType() {
            return OfflinePlayer.class;
        }

        @Override
        @SuppressWarnings("deprecation")
        public @Nullable OfflinePlayer map(@NotNull Object object) {
            if (object instanceof OfflinePlayer) {
                return (OfflinePlayer) object;
            } else if (object instanceof UUID) {
                return Bukkit.getOfflinePlayer((UUID) object);
            } else if (object instanceof String) {
                return Bukkit.getOfflinePlayer((String) object);
            } else {
                return null;
            }
        }
    }

    public static class OnlineProvider extends MappedProviderImpl<Player> {

        public OnlineProvider() {
            this(new HashMap<>());
        }

        public OnlineProvider(@NotNull Map<String, Function<Player, Object>> staticPlaceholder) {
            super(staticPlaceholder);
        }

        @Override
        public @NotNull Class<Player> getType() {
            return Player.class;
        }

        @Override
        public boolean acceptGlobal() {
            return false;
        }

        @Override
        public @Nullable Player map(@NotNull Object object) {
            if (object instanceof Player) {
                return (Player) object;
            } else if (object instanceof OfflinePlayer) {
                return ((OfflinePlayer) object).isOnline() ? ((OfflinePlayer) object).getPlayer() : null;
            } else if (object instanceof UUID) {
                return Bukkit.getPlayer((UUID) object);
            } else if (object instanceof String) {
                return Bukkit.getPlayerExact((String) object);
            } else {
                return null;
            }
        }
    }

    public interface Processor {

        void register(@NotNull PluginPlaceholder<?> placeholder);

        void unregister(@NotNull PluginPlaceholder<?> placeholder);
    }
}
