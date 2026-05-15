/*
 * MIT License.
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
package com.saicone.bukkit.module.command;

import com.google.common.base.Enums;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class BukkitCommandNode implements BukkitCommandExecution {

    public static String PERMISSION_PREFIX = "";

    protected BukkitCommandNode root;
    private final String id;
    protected List<BukkitCommandNode> subCommands;
    private final Supplier<String> path = Suppliers.memoize(() -> {
        if (getRoot() != null) {
            return getRoot().getPath() + "." + getId();
        } else {
            return getId();
        }
    });

    protected boolean register;
    protected String name;
    protected Set<String> aliases = Set.of();
    protected String permission = null;

    protected List<String> tab = null;
    protected Bridge bridge;
    protected Cache<String, Long> delay;
    protected long delayTime;

    public BukkitCommandNode(@NotNull String id) {
        this.id = id;
        this.subCommands = null;
        this.name = id;
    }

    public BukkitCommandNode(@NotNull String id, @NotNull BukkitCommandNode... subCommands) {
        this.id = id;
        this.subCommands = new ArrayList<>();
        Collections.addAll(this.subCommands, subCommands);
        this.name = id;
    }

    public void load() {
        load(true);
    }

    public void load(boolean register) {
        BukkitCommandConfig config = BukkitCommandConfig.EMPTY;
        if (register) {
            final String permission = PERMISSION_PREFIX.endsWith(".") ? PERMISSION_PREFIX.substring(PERMISSION_PREFIX.length() - 1) : PERMISSION_PREFIX;
            config = new BukkitCommandConfig() {
                @Override
                public boolean register() {
                    return true;
                }

                @Override
                public @NotNull Optional<Object> permission() {
                    return Optional.of(permission);
                }
            };
        }
        load(config);
    }

    public void load(@NotNull BukkitCommandConfig config) {
        if (subCommands != null) {
            for (BukkitCommandNode subCommand : subCommands) {
                if (subCommand.main()) {
                    subCommand.load(config.command(subCommand.getId()));
                }
            }
        }
        register = config.register();

        name = config.name().orElse(id);

        aliases = config.aliases();
        // Fix invalid aliases
        if (aliases.contains(name)) {
            aliases = new HashSet<>(aliases);
            aliases.remove(name);
        }

        final String permissionPath = PERMISSION_PREFIX + getPath();
        final String defaultPermission = permissionPath + ";" + permissionPath.substring(0, permissionPath.lastIndexOf('.') + 1) + "*";
        permission = config.permission().map(perm -> {
            return switch (perm) {
                case Boolean bool -> bool ? defaultPermission : "";
                case String s -> s;
                case Collection<?> collection -> collection.stream().filter(String.class::isInstance).map(String.class::cast).collect(Collectors.joining(";"));
                default -> null;
            };
        }).orElse(defaultPermission);

        final String delay = config.delay().orElse(null);
        if (delay != null && !delay.isBlank() && !delay.trim().startsWith("-")) {
            final String[] split = delay.trim().split(" ", 2);
            if (isNumber(split[0])) {
                final long duration = Long.parseLong(split[0]);
                final TimeUnit unit;
                if (split.length < 2) {
                    unit = TimeUnit.SECONDS;
                } else {
                    unit = Enums.getIfPresent(TimeUnit.class, split[1].trim().toUpperCase()).or(TimeUnit.SECONDS);
                }
                this.delay = CacheBuilder.newBuilder().expireAfterWrite(duration, unit).build();
                this.delayTime = unit.toMillis(duration);
            }
        }
        if (getSubCommands() != null) {
            tab = new ArrayList<>();
            for (BukkitCommandNode subCommand : getSubCommands()) {
                tab.add(subCommand.getName());
            }
        }
        if (isRegister()) {
            if (bridge == null) {
                bridge = new Bridge();
            } else {
                BukkitCommand.unregister(bridge);
                bridge.reload();
            }
            BukkitCommand.register(bridge);
        } else if (bridge != null) {
            BukkitCommand.unregister(bridge);
        }
    }

    public void unload() {
        if (bridge != null) {
            BukkitCommand.unregister(bridge);
        }
    }

    private static boolean isNumber(@NotNull String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected void subCommand(@NotNull BukkitCommandNode subCommand) {
        if (this.subCommands == null) {
            this.subCommands = new ArrayList<>();
        }
        subCommand.setRoot(this);
        this.subCommands.add(subCommand);
    }

    protected void subCommand(@NotNull String id, @NotNull BukkitCommandExecution execution) {
        subCommand(id, execution, false);
    }

    protected void subCommand(@NotNull String id, @NotNull BukkitCommandExecution execution, boolean main) {
        subCommand(new BukkitCommandNode(id) {
            @Override
            public boolean main() {
                return main;
            }

            @Override
            public void execute(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
                execution.execute(sender, cmd, args);
            }
        }.setRoot(this));
    }

    public void subCommand(@NotNull String id, int minArgs, @NotNull BukkitCommandExecution execution) {
        subCommand(id, minArgs, execution, false);
    }

    public void subCommand(@NotNull String id, int minArgs, @NotNull BukkitCommandExecution execution, boolean main) {
        subCommand(new BukkitCommandNode(id) {
            @Override
            public boolean main() {
                return main;
            }

            @Override
            public int getMinArgs() {
                return minArgs;
            }

            @Override
            public void execute(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
                execution.execute(sender, cmd, args);
            }
        }.setRoot(this));
    }

    public boolean match(@NotNull String s) {
        if (getName().equalsIgnoreCase(s)) {
            return true;
        }
        for (String alias : getAliases()) {
            if (alias.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean main() {
        return true;
    }

    public boolean isRegister() {
        return register;
    }

    public boolean hasPermission(@NotNull CommandSender sender) {
        final String permission = getPermission();
        if (permission == null || permission.length() == 0) {
            return true;
        }

        if (sender instanceof Player player && sender.isOp()) {
            final var data = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId()).getCachedData().getPermissionData();
            for (String p : permission.split(";")) {
                if (data.checkPermission(p).asBoolean()) {
                    return true;
                }
            }
        } else {
            for (String p : permission.split(";")) {
                if (sender.hasPermission(p)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    public BukkitCommandNode getRoot() {
        return root;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public BukkitCommandNode getSubCommand(@NotNull String id) {
        if (subCommands != null) {
            for (BukkitCommandNode subCommand : subCommands) {
                if (subCommand.getId().equals(id)) {
                    return subCommand;
                }
            }
        }
        throw new IllegalArgumentException("The sub command '" + id + "' doesn't exist");
    }

    @Nullable
    public List<BukkitCommandNode> getSubCommands() {
        return subCommands;
    }

    @NotNull
    public String getPath() {
        return path.get();
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Set<String> getAliases() {
        return aliases;
    }

    @Nullable
    public String getPermission() {
        return permission;
    }

    @Nullable
    public BukkitCommandNode.Bridge getBridge() {
        return bridge;
    }

    @Nullable
    public Cache<String, Long> getDelay() {
        return delay;
    }

    @NotNull
    public String getUsage(@NotNull CommandSender sender) {
        return "";
    }

    @NotNull
    public String getDescription(@NotNull CommandSender sender) {
        return "";
    }

    public int getMinArgs() {
        return 0;
    }

    public int getMinArgs(@NotNull CommandSender sender) {
        return getMinArgs();
    }

    public int getSubStart() {
        return 0;
    }

    public int getSubStart(@NotNull CommandSender sender) {
        return getSubStart();
    }

    @Nullable
    public Float getDelayTime(@NotNull CommandSender sender) {
        //if (!(sender instanceof Player)) {
        //    return null;
        //}
        if (delay == null) {
            return null;
        }
        final Long time = delay.getIfPresent(sender.getName());
        if (time == null) {
            delay.put(sender.getName(), System.currentTimeMillis() + delayTime);
            return null;
        }
        return (time - System.currentTimeMillis()) / 1000.00F;
    }

    public BukkitCommandNode setRoot(@Nullable BukkitCommandNode root) {
        this.root = root;
        return this;
    }

    protected void run(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
        if (!hasPermission(sender)) {
            sendPermissionMessage(sender);
            return;
        }
        if (getSubCommands() != null) {
            final int start = getSubStart(sender);
            if (args.length > start) {
                for (BukkitCommandNode subCommand : subCommands) {
                    if (subCommand.match(args[start])) {
                        final String[] array = new String[cmd.length + start + 1];
                        System.arraycopy(cmd, 0, array, 0, cmd.length);
                        System.arraycopy(args, 0, array, cmd.length, start + 1);
                        subCommand.run(sender, array, Arrays.copyOfRange(args, start + 1, args.length));
                        return;
                    }
                }
            }
        }
        if (args.length < getMinArgs(sender)) {
            sendUsage(sender, cmd, args);
        } else {
            execute(sender, cmd, args);
        }
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
        sendUsage(sender, cmd, args);
    }

    @Nullable
    protected List<String> suggestion(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        final int start = getSubStart(sender);
        if (args.length < start + 1) {
            return tab != null ? tab : tabComplete(sender, alias, args);
        } else if (getSubCommands() != null) {
            for (BukkitCommandNode subCommand : getSubCommands()) {
                if (subCommand.match(args[start])) {
                    return subCommand.suggestion(sender, args[start], Arrays.copyOfRange(args, start + 1, args.length));
                }
            }
        }
        return tabComplete(sender, alias, args);
    }

    @Nullable
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        return null;
    }

    public void sendPermissionMessage(@NotNull CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is a mistake.");
    }

    public void sendDelayMessage(@NotNull CommandSender sender, float seconds) {
        sender.sendMessage(ChatColor.RED + "You should wait " + ChatColor.GOLD + seconds + ChatColor.RED + " seconds after execute this command again.");
    }

    public void sendUsage(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
        final String usage = getUsage(sender);
        if (usage.isBlank()) {
            return;
        }
        final Object[] array = new Object[args.length + 1];
        array[0] = String.join(" ", cmd);
        System.arraycopy(args, 0, array, 1, args.length);
        sender.sendMessage(replaceArgs(usage, array));
        if (getSubCommands() != null) {
            for (BukkitCommandNode subCommand : getSubCommands()) {
                subCommand.sendSubUsage(sender);
            }
        }
    }

    public void sendSubUsage(@NotNull CommandSender sender) {
        final String description = getDescription(sender);
        sender.sendMessage(ChatColor.GOLD + "> " + ChatColor.RED + getName() + (description.isBlank() ? "" : ChatColor.GOLD + " - " + ChatColor.GRAY + description));
    }

    @NotNull
    public static String replaceArgs(@NotNull String s, @Nullable Object... args) {
        if (args.length < 1 || s.isBlank()) {
            return s.replace("{#}", "0").replace("{*}", "[]").replace("{-}", "");
        }
        final char[] chars = s.toCharArray();
        final StringBuilder builder = new StringBuilder(s.length());
        String all = null;
        for (int i = 0; i < chars.length; i++) {
            final int mark = i;
            if (chars[i] == '{') {
                int num = 0;
                while (i + 1 < chars.length) {
                    if (Character.isDigit(chars[i + 1])) {
                        i++;
                        num *= 10;
                        num += chars[i] - '0';
                        continue;
                    }
                    if (i == mark) {
                        final char c = chars[i + 1];
                        if (c == '#') {
                            i++;
                            num = -1;
                        } else if (c == '*') {
                            i++;
                            num = -2;
                        } else if (c == '-') {
                            i++;
                            num = -3;
                        }
                    }
                    break;
                }
                if (i != mark && i + 1 < chars.length && chars[i + 1] == '}') {
                    i++;
                    if (num == -1) {
                        builder.append(args.length);
                    } else if (num == -2) {
                        builder.append(Arrays.toString(args));
                    } else if (num == -3) {
                        if (all == null) {
                            all = Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(" "));
                        }
                        builder.append(all);
                    } else if (num < args.length) { // Avoid IndexOutOfBoundsException
                        builder.append(args[num]);
                    } else {
                        builder.append('{').append(num).append('}');
                    }
                } else {
                    i = mark;
                }
            }
            if (mark == i) {
                builder.append(chars[i]);
            }
        }
        return builder.toString();
    }

    public class Bridge extends Command {

        protected Bridge() {
            super(BukkitCommandNode.this.getName());
            reload();
        }

        public void reload() {
            setName(BukkitCommandNode.this.getName());
            setPermission(BukkitCommandNode.this.getPermission());
            setAliases(new ArrayList<>(BukkitCommandNode.this.getAliases()));
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            final Float seconds = getDelayTime(sender);
            if (seconds == null) {
                run(sender, new String[] {commandLabel}, args);
            } else {
                sendDelayMessage(sender, seconds);
            }
            return true;
        }

        @NotNull
        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
            final List<String> list = suggestion(sender, alias, args);
            return list != null ? list : super.tabComplete(sender, alias, args);
        }

        @Override
        public boolean testPermission(@NotNull CommandSender target) {
            if (testPermissionSilent(target)) {
                return true;
            }
            sendPermissionMessage(target);
            return false;
        }

        @Override
        public boolean testPermissionSilent(@NotNull CommandSender target) {
            return hasPermission(target);
        }
    }
}
