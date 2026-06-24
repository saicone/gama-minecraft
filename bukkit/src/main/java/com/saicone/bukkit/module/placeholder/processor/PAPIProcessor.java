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
package com.saicone.bukkit.module.placeholder.processor;

import com.saicone.bukkit.module.placeholder.impl.BukkitPlaceholder;
import com.saicone.minecraft.module.placeholder.NamedPlaceholder;
import com.saicone.minecraft.module.placeholder.Placeholder;
import com.saicone.minecraft.module.placeholder.PlaceholderProcessor;
import com.saicone.minecraft.module.placeholder.TypedPlaceholder;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PAPIProcessor implements PlaceholderProcessor {

    public static final PAPIProcessor INSTANCE = new PAPIProcessor();
    // lazy init var
    private final Supplier<Boolean> enabled = new Supplier<>() {
        private volatile Boolean value;

        @Override
        public Boolean get() {
            if (value == null) {
                synchronized (this) {
                    if (value == null) {
                        value = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
                    }
                }
            }
            return value;
        }
    };

    private final Map<String, Object> registered = new HashMap<>();

    public boolean enabled() {
        return enabled.get();
    }

    public boolean contains(@NotNull String s) {
        if (enabled()) {
            return contains(PlaceholderAPI.getPlaceholderPattern(), s);
        } else {
            return false;
        }
    }

    public boolean containsBracket(@NotNull String s) {
        if (enabled()) {
            return contains(PlaceholderAPI.getBracketPlaceholderPattern(), s);
        } else {
            return false;
        }
    }

    private boolean contains(@NotNull Pattern pattern, @NotNull String s) {
        final Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            String match = matcher.group(1);
            final int index = match.indexOf('_');
            if (index == 0) {
                continue;
            } else if (index > 0) {
                match = match.substring(0, index);
            }
            if (PlaceholderAPI.isRegistered(match)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Contract("_, !null -> !null")
    public String parse(@Nullable OfflinePlayer player, @Nullable String s) {
        if (s != null && enabled()) {
            return PlaceholderAPI.setPlaceholders(player, s);
        }
        return s;
    }

    @NotNull
    public List<String> parse(@Nullable OfflinePlayer player, @NotNull Collection<String> collection) {
        if (!collection.isEmpty() && enabled()) {
            final List<String> list = new ArrayList<>();
            for (String s : collection) {
                list.add(PlaceholderAPI.setPlaceholders(player, s));
            }
            return list;
        }
        return new ArrayList<>(collection);
    }

    @Nullable
    @Contract("_, !null -> !null")
    public String parseBracket(@Nullable OfflinePlayer player, @Nullable String s) {
        if (s != null && enabled()) {
            return PlaceholderAPI.setBracketPlaceholders(player, s);
        }
        return s;
    }

    @NotNull
    public List<String> parseBracket(@Nullable OfflinePlayer player, @NotNull Collection<String> collection) {
        if (!collection.isEmpty() && enabled()) {
            final List<String> list = new ArrayList<>();
            for (String s : collection) {
                list.add(PlaceholderAPI.setBracketPlaceholders(player, s));
            }
            return list;
        }
        return new ArrayList<>(collection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void register(@NotNull NamedPlaceholder<?> placeholder) {
        if (!enabled()) {
            return;
        }

        final Plugin plugin;
        if (placeholder instanceof BukkitPlaceholder<?>) {
            plugin = ((BukkitPlaceholder<?>) placeholder).plugin();
        } else {
            plugin = null;
        }

        if (placeholder instanceof TypedPlaceholder<?>) {
            final TypedPlaceholder<?> typed = (TypedPlaceholder<?>) placeholder;

            final boolean online;
            if (typed.type().equals(Player.class)) {
                online = true;
            } else if (typed.type().equals(OfflinePlayer.class)) {
                online = false;
            } else {
                online = !typed.acceptNull();
            }

            if (online) {
                final Placeholder<Player> onlinePlaceholder = typed.as(Player.class);
                for (String name : placeholder.names()) {
                    registerOnline(plugin, name, placeholder.author(), placeholder.version(), onlinePlaceholder);
                }
            } else {
                final Placeholder<OfflinePlayer> offlinePlaceholder = typed.as(OfflinePlayer.class);
                for (String name : placeholder.names()) {
                    registerOffline(plugin, name, placeholder.author(), placeholder.version(), offlinePlaceholder);
                }
            }
        } else {
            final Placeholder<Object> objectPlaceholder = (Placeholder<Object>) placeholder;
            for (String name : placeholder.names()) {
                registerHeadless(plugin, name, placeholder.author(), placeholder.version(), objectPlaceholder);
            }
        }
    }

    private void registerOnline(@Nullable Plugin plugin, @NotNull String name, @NotNull String author, @NotNull String version, @NotNull Placeholder<Player> placeholder) {
        final PlaceholderExpansion expansion = new PlaceholderExpansion() {
            @Override
            public @NotNull String getIdentifier() {
                return name;
            }

            @Override
            public @NotNull String getAuthor() {
                return author;
            }

            @Override
            public @NotNull String getVersion() {
                return version;
            }

            @Override
            public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
                final Object result = placeholder.get(player, params);
                return result != null ? result.toString() : null;
            }
        };
        registerExpansion(plugin, expansion);
    }

    private void registerOffline(@Nullable Plugin plugin, @NotNull String name, @NotNull String author, @NotNull String version, @NotNull Placeholder<OfflinePlayer> placeholder) {
        final PlaceholderExpansion expansion = new PlaceholderExpansion() {
            @Override
            public @NotNull String getIdentifier() {
                return name;
            }

            @Override
            public @NotNull String getAuthor() {
                return author;
            }

            @Override
            public @NotNull String getVersion() {
                return version;
            }

            @Override
            public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
                final Object result = placeholder.get(player, params);
                return result != null ? result.toString() : null;
            }
        };
        registerExpansion(plugin, expansion);
    }

    private void registerHeadless(@Nullable Plugin plugin, @NotNull String name, @NotNull String author, @NotNull String version, @NotNull Placeholder<Object> placeholder) {
        final PlaceholderExpansion expansion = new PlaceholderExpansion() {
            @Override
            public @NotNull String getIdentifier() {
                return name;
            }

            @Override
            public @NotNull String getAuthor() {
                return author;
            }

            @Override
            public @NotNull String getVersion() {
                return version;
            }

            @Override
            public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
                try {
                    final Object result = placeholder.get(player, params);
                    return result != null ? result.toString() : null;
                } catch (ClassCastException e) {
                    return super.onRequest(player, params);
                }
            }

            @Override
            public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
                try {
                    final Object result = placeholder.get(player, params);
                    return result != null ? result.toString() : null;
                } catch (ClassCastException e) {
                    return super.onPlaceholderRequest(player, params);
                }
            }
        };
        registerExpansion(plugin, expansion);
    }

    @NotNull
    public <T extends Collection<String>> T register(@NotNull Plugin plugin, @NotNull T names, @NotNull BiFunction<Player, String, Object> onPlaceholderRequest) {
        if (!enabled()) {
            return names;
        }

        final String author = String.join(", ", plugin.getDescription().getAuthors());
        final String version = plugin.getDescription().getVersion();

        for (String name : names) {
            registerExpansion(plugin, new PlaceholderExpansion() {
                @Override
                public @NotNull String getIdentifier() {
                    return name;
                }

                @Override
                public @NotNull String getAuthor() {
                    return author;
                }

                @Override
                public @NotNull String getVersion() {
                    return version;
                }

                @Override
                public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
                    final Object obj = onPlaceholderRequest.apply(player, params);
                    return obj == null ? null : obj.toString();
                }
            });
        }

        return names;
    }

    @NotNull
    public <T extends Collection<String>> T registerOffline(@NotNull Plugin plugin, @NotNull T names, @NotNull BiFunction<OfflinePlayer, String, Object> function) {
        if (!enabled()) {
            return names;
        }

        final String author = String.join(", ", plugin.getDescription().getAuthors());
        final String version = plugin.getDescription().getVersion();

        for (String name : names) {
            registerExpansion(plugin, new PlaceholderExpansion() {
                @Override
                public @NotNull String getIdentifier() {
                    return name;
                }

                @Override
                public @NotNull String getAuthor() {
                    return author;
                }

                @Override
                public @NotNull String getVersion() {
                    return version;
                }

                @Override
                public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
                    final Object obj = function.apply(player, params);
                    return obj == null ? null : obj.toString();
                }
            });
        }

        return names;
    }

    private void registerExpansion(@Nullable Plugin plugin, @NotNull Object expansion) {
        this.registered.put(((PlaceholderExpansion) expansion).getIdentifier(), expansion);
        if (Bukkit.isPrimaryThread()) {
            ((PlaceholderExpansion) expansion).register();
        } else if (plugin != null) {
            Bukkit.getScheduler().runTask(plugin, () -> ((PlaceholderExpansion) expansion).register());
        } else {
            throw new IllegalStateException("Cannot register placeholder from non-primary thread without a plugin reference.");
        }
    }

    @Override
    public void unregister(@NotNull NamedPlaceholder<?> placeholder) {
        if (!enabled()) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            unregister0(placeholder);
        } else if (placeholder instanceof BukkitPlaceholder<?>) {
            final Plugin plugin = ((BukkitPlaceholder<?>) placeholder).plugin();
            Bukkit.getScheduler().runTask(plugin, () -> unregister0(placeholder));
        } else {
            throw new IllegalStateException("Cannot unregister placeholder from non-primary thread without a plugin reference.");
        }
    }

    private void unregister0(@NotNull NamedPlaceholder<?> placeholder) {
        for (String name : placeholder.names()) {
            final Object expansion = this.registered.remove(name);
            if (expansion != null) {
                ((PlaceholderExpansion) expansion).unregister();
            }
        }
    }

    @NotNull
    public <T extends Collection<String>> T unregister(@NotNull T names) {
        if (enabled()) {
            for (String name : names) {
                final PlaceholderExpansion expansion = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().getExpansion(name);
                if (expansion != null) {
                    expansion.unregister();
                }
            }
        }
        return names;
    }
}
