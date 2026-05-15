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
package com.saicone.bungeecord.util;

import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class ServerPing {

    ServerPing() {
    }

    public static boolean ping(@NotNull ServerInfo server) throws IOException {
        return ping(server, 2000);
    }

    public static boolean ping(@NotNull ServerInfo server, int timeout) throws IOException {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(timeout);
            socket.connect(server.getSocketAddress(), timeout);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            InputStream in = socket.getInputStream();
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_16BE);

            out.write(new byte[] { -2, 1 });

            if (in.read() != 255) {
                return false;
            }

            final int length = reader.read();
            return length != -1 && length != 0;
        }
    }
}
