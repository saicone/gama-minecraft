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
package com.saicone.gama.bukkit.module.placeholder.impl;

import com.saicone.gama.minecraft.module.placeholder.TypedPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface OfflinePlayerPlaceholder extends TypedPlaceholder<OfflinePlayer> {

    @Override
    default @NotNull Class<OfflinePlayer> type() {
        return OfflinePlayer.class;
    }

    @Override
    @SuppressWarnings("deprecation")
    default @Nullable OfflinePlayer parse(@Nullable Object object) {
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
