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
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholders {

    // lazy init var
    private static final Supplier<Boolean> ENABLED = new Supplier<>() {
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

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static boolean contains(@NotNull String s) {
        return contains(PlaceholderAPI.getPlaceholderPattern(), s);
    }

    public static boolean containsBracket(@NotNull String s) {
        return contains(PlaceholderAPI.getBracketPlaceholderPattern(), s);
    }

    private static boolean contains(@NotNull Pattern pattern, @NotNull String s) {
        if (isEnabled()) {
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
        }
        return false;
    }

    @Nullable
    @Contract("_, !null -> !null")
    public static String parse(@Nullable OfflinePlayer player, @Nullable String s) {
        if (s != null && isEnabled()) {
            return PlaceholderAPI.setPlaceholders(player, s);
        }
        return s;
    }

    @NotNull
    public static List<String> parse(@Nullable OfflinePlayer player, @NotNull Collection<String> collection) {
        if (!collection.isEmpty() && isEnabled()) {
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
    public static String parseBracket(@Nullable OfflinePlayer player, @Nullable String s) {
        if (s != null && isEnabled()) {
            return PlaceholderAPI.setBracketPlaceholders(player, s);
        }
        return s;
    }

    @NotNull
    public static List<String> parseBracket(@Nullable OfflinePlayer player, @NotNull Collection<String> collection) {
        if (!collection.isEmpty() && isEnabled()) {
            final List<String> list = new ArrayList<>();
            for (String s : collection) {
                list.add(PlaceholderAPI.setBracketPlaceholders(player, s));
            }
            return list;
        }
        return new ArrayList<>(collection);
    }

    public static <T extends Collection<String>> T register(@NotNull Plugin plugin, @NotNull T names, @NotNull BiFunction<Player, String, Object> onPlaceholderRequest) {
        if (isEnabled()) {
            for (String name : names) {
                new Expansion(name, plugin) {

                    @Override
                    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
                        final Object obj = onPlaceholderRequest.apply(player, params);
                        return obj == null ? null : obj.toString();
                    }
                }.register();
            }
        }
        return names;
    }

    public static <T extends Collection<String>> T registerOffline(@NotNull Plugin plugin, @NotNull T names, @NotNull BiFunction<OfflinePlayer, String, Object> function) {
        if (isEnabled()) {
            for (String name : names) {
                new Expansion(name, plugin) {
                    @Override
                    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
                        final Object obj = function.apply(player, params);
                        return obj == null ? null : obj.toString();
                    }
                }.register();
            }
        }
        return names;
    }

    public static <T extends Collection<String>> T unregister(@NotNull T names) {
        if (isEnabled()) {
            for (String name : names) {
                final PlaceholderExpansion expansion = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().getExpansion(name);
                if (expansion != null) {
                    expansion.unregister();
                }
            }
        }
        return names;
    }

    public static class Expansion extends PlaceholderExpansion {

        private final String name;
        private final String author;
        private final String version;

        public Expansion(@NotNull String name, @NotNull Plugin plugin) {
            this.name = name;
            this.author = String.join(", ", plugin.getDescription().getAuthors());
            this.version = plugin.getDescription().getVersion();
        }

        public Expansion(@NotNull String name, @NotNull String author, @NotNull String version) {
            this.name = name;
            this.author = author;
            this.version = version;
        }

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
        public boolean persist() {
            return true;
        }
    }
}
