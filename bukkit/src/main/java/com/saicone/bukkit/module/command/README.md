# bukkit command module

* use Bukkit API, so tab completion may be limited
* subcommands
* command delay
* dynamic minimum arguments and subcommand start index
* command configuration reader

## dependencies

* The class `SettingsCommandConfig` requires the settings library: https://github.com/saicone/settings

## initialization

```java
public class MyPlugin extends JavaPlugin {
    public MyPlugin() {
        BukkitCommand.PLUGIN = this;
        // optional
        BukkitCommandNode.PERMISSION_PREFIX = "myplugin.command.";
    }
}
```

## usage

Command declaration
```java
public class MyCommand extends BukkitCommandNode {

    public MyCommand() {
        super("mycommand"); // command id

        // optional properties
        this.name = "mycommand"; // command name, by default is the command id
        this.aliases = Set.of("mycmd");
        this.permission = "myplugin.command.mycommand";
        this.delayTime = 0L; // in milliseconds
        
        // register subcommands
        //   - another BukkitCommandNode instance
        this.subCommand(new MySubCommand());
        //   - subcommand argument and executor
        this.subCommand("mysub", (sender, cmd, args) -> {
            // do something
        });
        //   - subcommand argument, minimum arguments and executor
        this.subCommand("mysub", 2, (sender, cmd, args) -> {
            // do something
        });
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
        // cmd - the arguments before, like the command label and subcommand labels
        // args - the current arguments

        // by default this method send the usage message
        super.sendUsage(sender, cmd, args);
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        return null;
    }
    
    // additional methods

    @Override
    public @NotNull String getUsage(@NotNull CommandSender sender) {
        // {0} is the command label
        return "{0} <arg1> [arg2]";
    }

    @Override
    public @NotNull String getDescription(@NotNull CommandSender sender) {
        return "My command description";
    }

    @Override
    public int getMinArgs() {
        // static minimum arguments, default is 0
        return 0;
    }

    @Override
    public int getMinArgs(@NotNull CommandSender sender) {
        // dynamic minimum arguments, for example, based on the sender's permissions
        if (sender.hasPermission("some.permission")) {
            return 2;
        } else {
            return getMinArgs();
        }
    }

    @Override
    public int getSubStart() {
        // the argument index where subcommand identifier start, default is 0
        return 0;
    }

    @Override
    public int getSubStart(@NotNull CommandSender sender) {
        // dynamic subcommand start index, for example, based on the sender's permissions
        if (sender.hasPermission("some.permission")) {
            return 1;
        } else {
            return getSubStart();
        }
    }

    @Override
    public void sendPermissionMessage(@NotNull CommandSender sender) {
        // Send a custom permission message instead of the default one
        super.sendPermissionMessage(sender);
    }

    @Override
    public void sendDelayMessage(@NotNull CommandSender sender, float seconds) {
        // Send a custom delay message instead of the default one
        super.sendDelayMessage(sender, seconds);
    }

    @Override
    public void sendUsage(@NotNull CommandSender sender, @NotNull String[] cmd, @NotNull String[] args) {
        // Send a custom usage message instead of the default one
        super.sendUsage(sender, cmd, args);
    }
}
```

Command registration
```java
public class MyPlugin extends JavaPlugin {

    private final MyCommand myCommand = new MyCommand();
    
    @Override
    public void onEnable() {
        this.myCommand.load();
    }
    
    @Override
    public void onDisable() {
        this.myCommand.unload();
    }
}
```

You can also create a command configuration
```yaml
commands:
  # command id
  mycommand:
    register: true
    name: mycommand
    aliases:
      - mycmd
    permission: myplugin.command.mycommand
    delay: 0 SECONDS # You can use other time units like MILLISECONDS, MINUTES, etc.
    sub:
      mysub:
        register: false # Set to true to register as main command
        aliases: [subcmd]
```

And register the command with the configuration
```java
public class MyPlugin extends JavaPlugin {

    private final MyCommand myCommand = new MyCommand();
    
    @Override
    public void onEnable() {
        ConfigurationSection pluginConfig = ...;

        BukkitCommandConfig commandConfig = BukkitCommandConfig.valueOf(pluginConfig.getConfigurationSection("commands.mycommand"));
        this.myCommand.load(commandConfig);
    }

    @Override
    public void onDisable() {
        this.myCommand.unload();
    }
}
```
