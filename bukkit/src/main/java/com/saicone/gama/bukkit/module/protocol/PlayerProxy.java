/*
 * MIT License.
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
package com.saicone.gama.bukkit.module.protocol;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class PlayerProxy implements PluginMessageListener, Listener {

    public static final String LEGACY_CHANNEL = "BungeeCord";
    public static final String MODERN_CHANNEL = "bungeecord:main";
    public static final String PROXY_SERVER = "ALL";
    public static final String ALL_PLAYERS = "ALL";

    private static final long DEFAULT_TIMEOUT = 20_000L;
    private static final Consumer<ByteArrayDataOutput> EMPTY_WRITER = out -> {};

    private final Plugin plugin;
    private final String channel;
    private final Cache<String, Request<?>> requests;
    private PluginMessageListener listener;

    public PlayerProxy(@NotNull Plugin plugin) {
        this(plugin, DEFAULT_TIMEOUT);
    }

    public PlayerProxy(@NotNull Plugin plugin, @NotNull String channel) {
        this(plugin, channel, DEFAULT_TIMEOUT);
    }

    public PlayerProxy(@NotNull Plugin plugin, long timeout) {
        this(plugin, LEGACY_CHANNEL, timeout);
    }

    public PlayerProxy(@NotNull Plugin plugin, @NotNull String channel, long timeout) {
        this.plugin = plugin;
        this.channel = channel;
        this.requests = CacheBuilder.newBuilder()
                .expireAfterWrite(timeout, TimeUnit.MILLISECONDS)
                .<String, Request<?>>removalListener(notification -> {
                    if (notification.getCause() != RemovalCause.EXPIRED) {
                        return;
                    }
                    final Request<?> request = notification.getValue();
                    if (request != null) {
                        request.future.completeExceptionally(new RuntimeException("Request timed out"));
                    }
                })
                .build();
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }

    @NotNull
    public String getChannel() {
        return channel;
    }

    @NotNull
    public PluginMessageListener getListener() {
        return listener;
    }

    @NotNull
    @Contract("_ -> this")
    public PlayerProxy listen(@NotNull PluginMessageListener listener) {
        this.listener = listener;
        return this;
    }

    public void start() {
        registerPluginChannel(channel);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        unregisterPluginChannel(channel);
        HandlerList.unregisterAll(this);
    }

    public void registerPluginChannel(@NotNull String channel) {
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, channel, this);
        if (!Bukkit.getServer().getMessenger().isOutgoingChannelRegistered(plugin, channel)) {
            Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, channel);
        }
    }

    public void unregisterPluginChannel(@NotNull String channel) {
        Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, channel, this);
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, channel);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!this.channel.equals(channel)) {
            if (listener != null) {
                listener.onPluginMessageReceived(channel, player, message);
            }
            return;
        }

        final ByteArrayDataInput in = ByteStreams.newDataInput(message);
        final String subChannel = in.readUTF();

        final String key;
        if (subChannel.equals("Forward")) {
            final String subSubChannel = in.readUTF();
            key = Request.key(subChannel, subSubChannel);
        } else if (subChannel.equals("ForwardToPlayer")) {
            final String subSubChannel = in.readUTF();
            key = Request.key(subChannel, subSubChannel, player);
        } else {
            key = Request.key(subChannel, player);
        }

        final Request<?> request = requests.asMap().remove(key);
        if (request != null) {
            request.accept(in);
        } else if (listener != null) {
            listener.onPluginMessageReceived(channel, player, message);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        final String suffix = ":" + event.getPlayer().getUniqueId();
        for (String key : requests.asMap().keySet()) {
            if (key.endsWith(suffix)) {
                final Request<?> request = requests.asMap().remove(key);
                if (request != null) {
                    request.future.cancel(true);
                }
            }
        }
    }

    public void connect(@NotNull Player player, @NotNull String server) {
        write(player, "Connect", out -> {
            out.writeUTF(server);
        });
    }

    public void connectOther(@NotNull Player player, @NotNull String otherPlayer, @NotNull String server) {
        write(player, "ConnectOther", out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(server);
        });
    }

    @NotNull
    public CompletableFuture<String> ip(@NotNull Player player) {
        return writeAndRead(player, "IP", in -> {
            final String ip = in.readUTF();
            final int port = in.readInt();

            return ip + ":" + port;
        });
    }

    @NotNull
    public CompletableFuture<String> ipOther(@NotNull Player player, @NotNull String otherPlayer) {
        return writeAndRead(player, "IPOther", out -> {
            out.writeUTF(otherPlayer);
        }, in -> {
            final String ip = in.readUTF();
            final int port = in.readInt();

            return ip + ":" + port;
        });
    }

    @NotNull
    public CompletableFuture<Integer> playerCount(@NotNull Player player) {
        return playerCount(player, PROXY_SERVER);
    }

    @NotNull
    public CompletableFuture<Integer> playerCount(@NotNull Player player, @NotNull String server) {
        return writeAndRead(player, "PlayerCount", out -> {
            out.writeUTF(server);
        }, in -> {
            in.readUTF(); // server
            final int count = in.readInt();

            return count;
        });
    }
    @NotNull
    public CompletableFuture<Set<String>> playerList(@NotNull Player player) {
        return playerList(player, PROXY_SERVER);
    }

    @NotNull
    public CompletableFuture<Set<String>> playerList(@NotNull Player player, @NotNull String server) {
        return writeAndRead(player, "PlayerList", out -> {
            out.writeUTF(server);
        }, in -> {
            in.readUTF(); // server
            final String playerList = in.readUTF();

            return split(playerList);
        });
    }

    @NotNull
    public CompletableFuture<String> getPlayerServer(@NotNull Player player) {
        return getPlayerServer(player, player.getName());
    }

    @NotNull
    public CompletableFuture<String> getPlayerServer(@NotNull Player player, @NotNull String otherPlayer) {
        return writeAndRead(player, "GetPlayerServer", out -> {
            out.writeUTF(otherPlayer);
        }, in -> {
            final String playerName = in.readUTF();
            final String serverName = in.readUTF();

            return serverName;
        });
    }

    @NotNull
    public CompletableFuture<Set<String>> getServers(@NotNull Player player) {
        return writeAndRead(player, "GetServers", in -> {
            final String serverList = in.readUTF();

            return split(serverList);
        });
    }

    public void message(@NotNull Player player, @NotNull String message) {
        message(player, player.getName(), message);
    }

    public void message(@NotNull Player player, @NotNull String otherPlayer, @NotNull String message) {
        write(player, "Message", out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(message);
        });
    }

    public void messageRaw(@NotNull Player player, @NotNull String message) {
        messageRaw(player, player.getName(), message);
    }

    public void messageRaw(@NotNull Player player, @NotNull String otherPlayer, @NotNull String message) {
        write(player, "MessageRaw", out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(message);
        });
    }

    @NotNull
    public CompletableFuture<String> getServer(@NotNull Player player) {
        return writeAndRead(player, "GetServer", in -> {
            final String serverName = in.readUTF();

            return serverName;
        });
    }

    public void forward(@NotNull Player player, @NotNull String channel, @NotNull byte[] data) {
        forward(player, PROXY_SERVER, channel, data);
    }

    public void forward(@NotNull Player player, @NotNull String server, @NotNull String channel, @NotNull byte[] data) {
        write(player, "Forward", out -> {
            out.writeUTF(server);
            out.writeUTF(channel);
            out.writeShort(data.length);
            out.write(data);
        });
    }

    @NotNull
    public CompletableFuture<byte[]> forwardAndListen(@NotNull Player player, @NotNull String server, @NotNull String channel, @NotNull byte[] data) {
        return writeAndRead(player, "Forward", Request.key("Forward", channel), out -> {
            out.writeUTF(server);
            out.writeUTF(channel);
            out.writeShort(data.length);
            out.write(data);
        }, in -> {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            return msgbytes;
        });
    }

    public void forwardToPlayer(@NotNull Player player, @NotNull String otherPlayer, @NotNull String channel, @NotNull byte[] data) {
        write(player, "ForwardToPlayer", out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(channel);
            out.writeShort(data.length);
            out.write(data);
        });
    }

    @NotNull
    public CompletableFuture<byte[]> forwardToPlayerAndListen(@NotNull Player player, @NotNull String otherPlayer, @NotNull String channel, @NotNull byte[] data) {
        return writeAndRead(player, "ForwardToPlayer", Request.key("ForwardToPlayer", channel, player), out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(channel);
            out.writeShort(data.length);
            out.write(data);
        }, in -> {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            return msgbytes;
        });
    }

    @NotNull
    public CompletableFuture<UUID> uuid(@NotNull Player player) {
        return writeAndRead(player, "UUID", in -> {
            final String uuid = in.readUTF();

            return UUID.fromString(uuid);
        });
    }

    @NotNull
    public CompletableFuture<UUID> uuidOther(@NotNull Player player, @NotNull String otherPlayer) {
        return writeAndRead(player, "UUIDOther", out -> {
            out.writeUTF(otherPlayer);
        }, in -> {
            final String uuid = in.readUTF();

            return UUID.fromString(uuid);
        });
    }

    @NotNull
    public CompletableFuture<String> serverIP(@NotNull Player player, @NotNull String server) {
        return writeAndRead(player, "ServerIP", out -> {
            out.writeUTF(server);
        }, in -> {
            in.readUTF(); // server
            final String ip = in.readUTF();
            final int port = in.readInt();

            return ip + ":" + port;
        });
    }

    public void kickPlayer(@NotNull Player player, @NotNull String message) {
        kickPlayer(player, player.getName(), message);
    }

    public void kickPlayer(@NotNull Player player, @NotNull String otherPlayer, @NotNull String message) {
        write(player, "KickPlayer", out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(message);
        });
    }

    public void kickPlayerRaw(@NotNull Player player, @NotNull String message) {
        kickPlayerRaw(player, player.getName(), message);
    }

    public void kickPlayerRaw(@NotNull Player player, @NotNull String otherPlayer, @NotNull String message) {
        write(player, "KickPlayerRaw", out -> {
            out.writeUTF(otherPlayer);
            out.writeUTF(message);
        });
    }

    protected void write(@NotNull Player player, @NotNull String subChannel) {
        write(player, subChannel, EMPTY_WRITER);
    }

    protected void write(@NotNull Player player, @NotNull String subChannel, @NotNull Consumer<ByteArrayDataOutput> writer) {
        write(player, subChannel, null, writer);
    }

    protected void write(@NotNull Player player, @NotNull String subChannel, @Nullable String requestKey, @NotNull Consumer<ByteArrayDataOutput> writer) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subChannel);
        try {
            writer.accept(out);

            player.sendPluginMessage(plugin, channel, out.toByteArray());
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to write plugin message for sub-channel: " + subChannel, t);

            // Notify the request future if it exists
            final String key = requestKey != null ? requestKey : Request.key(subChannel, player);
            final Request<?> request = requests.asMap().remove(key);
            if (request != null) {
                request.future.completeExceptionally(t);
            }
        }
    }

    @NotNull
    protected <T> CompletableFuture<T> writeAndRead(@NotNull Player player, @NotNull String subChannel, @NotNull Function<ByteArrayDataInput, T> reader) {
        return writeAndRead(player, subChannel, EMPTY_WRITER, reader);
    }

    @NotNull
    protected <T> CompletableFuture<T> writeAndRead(@NotNull Player player, @NotNull String subChannel, @NotNull Consumer<ByteArrayDataOutput> writer, @NotNull Function<ByteArrayDataInput, T> reader) {
        return writeAndRead(player, subChannel, null, writer, reader);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    protected <T> CompletableFuture<T> writeAndRead(@NotNull Player player, @NotNull String subChannel, @Nullable String requestKey, @NotNull Consumer<ByteArrayDataOutput> writer, @NotNull Function<ByteArrayDataInput, T> reader) {
        // Get or create request
        final String key = requestKey != null ? requestKey : Request.key(subChannel, player);
        final CompletableFuture<T> future;
        Request<?> request = requests.getIfPresent(key);
        if (request == null) {
            future = new CompletableFuture<>();
            request = new Request<>(future, reader);
        } else {
            future = (CompletableFuture<T>) request.future;
        }

        // Update timeout for the request
        requests.put(key, request);

        // Write the plugin message
        write(player, subChannel, requestKey, writer);

        return future;
    }

    @NotNull
    private static Set<String> split(@NotNull String s) {
        final Set<String> result = new LinkedHashSet<>();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == ',') {
                result.add(builder.toString());
                builder.setLength(0);
            } else {
                builder.append(c);
            }
        }
        if (!builder.isEmpty()) {
            result.add(builder.toString());
        }
        return result;
    }

    private record Request<T>(@NotNull CompletableFuture<T> future, @NotNull Function<ByteArrayDataInput, T> function) {

        @NotNull
        public static String key(@NotNull String channel, @NotNull Player player) {
            return channel + ":" + player.getUniqueId();
        }

        @NotNull
        public static String key(@NotNull String channel, @NotNull String subChannel) {
            return channel + ":" + subChannel;
        }

        @NotNull
        public static String key(@NotNull String channel, @NotNull String subChannel, @NotNull Player player) {
            return channel + ":" + subChannel + ":" + player.getUniqueId();
        }

        public void accept(@NotNull ByteArrayDataInput input) {
            try {
                final T result = function.apply(input);
                future.complete(result);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }
    }
}
