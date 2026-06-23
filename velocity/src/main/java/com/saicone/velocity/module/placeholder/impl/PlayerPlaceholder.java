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
package com.saicone.velocity.module.placeholder.impl;

import com.saicone.minecraft.module.placeholder.TypedPlaceholder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PlayerPlaceholder extends TypedPlaceholder<Player> {

    @NotNull
    ProxyServer proxy();

    @Override
    default @NotNull Class<Player> type() {
        return Player.class;
    }

    @Override
    @Nullable
    default Player parse(@Nullable Object object) {
        if (object instanceof Player) {
            return (Player) object;
        } else if (object instanceof UUID) {
            return proxy().getPlayer((UUID) object).orElse(null);
        } else if (object instanceof String) {
            return proxy().getPlayer((String) object).orElse(null);
        } else {
            return null;
        }
    }
}
