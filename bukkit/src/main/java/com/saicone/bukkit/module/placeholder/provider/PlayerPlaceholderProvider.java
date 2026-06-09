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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class PlayerPlaceholderProvider extends MappedPlaceholderProviderImpl<Player> {

    public PlayerPlaceholderProvider() {
        this(new HashMap<>());
    }

    public PlayerPlaceholderProvider(@NotNull Map<String, Function<Player, Object>> staticPlaceholder) {
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
