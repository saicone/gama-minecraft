# bukkit placeholder module

* multiple placeholder processor support
* placeholder text detection
* placeholder text replacement
* static and dynamic placeholder registration

## dependencies

* common placeholder module
* PlaceholderAPI: https://github.com/PlaceholderAPI/PlaceholderAPI
* MiniPlaceholders: https://github.com/MiniPlaceholders/MiniPlaceholders

## usage - register placeholder

First, create a class that extends `BukkitPlaceholder`.

If you want a placeholder for online players, implement `PlayerPlaceholder` and use `Player` as the first parameter of `BukkitPlaceholder`.

If you want a placeholder for offline players, implement `OfflinePlayerPlaceholder` and use `OfflinePlayer` as the first parameter of `BukkitPlaceholder`.

By default, the `PlayerPlaceholder` class only accept connected players, but you can override the `acceptNull` method to allow global placeholders.

```java
public class MyPlaceholder extends BukkitPlaceholder<Player> implements PlayerPlaceholder {

    public MyPlaceholder(@NotNull Plugin plugin) {
        super(plugin);

        // --- Add processors to register this placeholder on
        // PlaceholderAPI
        this.processors().add(Placeholders.papi());
        // MiniPlaceholders
        this.processors().add(Placeholders.mini());
    }
}
```

Then you can use your placeholder to parse any object by its key.

Let's assume the placeholder name is `example`, and you put the key `greeting` with a value.

The value will be resolved on:
* PlaceholderAPI as `%example_greeting%`.
* MiniPlaceholders as `<example_greeting>`.

```java
Plugin plugin = ...;
MyPlaceholder placeholder = new MyPlaceholder(plugin);

// Key "greeting" with value "Hello, world!"
placeholder.put("greeting", "Hello, world!");

// Provide value by supplier (if the value is dynamic)
placeholder.put("greeting", () -> "Random number:" + ThreadLocalRandom.current().nextInt());

// Provide value by player function
placeholder.put("greeting", player -> {
    if (player.hasPermission("example.greet")) {
        return "Hello, " + player.getName() + "!";
    } else {
        return "Hello, stranger!";
    }
});
```

You can also register placeholders that accept dynamic arguments.

The processor for:
* PlaceholderAPI provides a `String` as "parameters".
* MiniPlaceholders provides a `Iterator<String>`.

If you only accept one argument, you can only extend the method that accept the `String` "parameters".

In this example, the value will be resolved on:
* PlaceholderAPI as `%example_sum_<arg1>_<arg2>_<arg3>...etc%`.
* MiniPlaceholders as `<example_sum:<arg1>:<arg2>:<arg3>...etc>`.

```java
Plugin plugin = ...;
MyPlaceholder placeholder = new MyPlaceholder(plugin);

placeholder.put("sum", new Placeholder<Player>() {
    @Override
    public Object get(@NotNull Player player, @NotNull String parameters) {
        return get(player, Arrays.asList(parameters.split("_")).iterator());
    }

    @Override
    public Object get(@NotNull Player player, @NotNull Iterator<String> args) {
        int sum = 0;
        while (args.hasNext()) {
            String next = args.next();
            try {
                sum += Integer.parseInt(next);
            } catch (NumberFormatException e) {
                // ignore invalid number
            }
        }
        return sum;
    }
});
```

And finnally, register the placeholder with your plugin.

```java
public class MyPlugin extends JavaPlugin {

    private final MyPlaceholder placeholder;
    
    public MyPlugin() {
        this.placeholder = new MyPlaceholder(this);
    }

    @Override
    public void onLoad() {
        // Put keys on your placeholder before registering it
        this.placeholder.put("greeting", "Hello, world!");
    }

    @Override
    public void onEnable() {
        // Simple register, use plugin name as placeholder name
        this.placeholder.register();
        // Or register with a custom name (compatible with multiple names)
        this.placeholder.register("example", "test");
    }

    @Override
    public void onDisable() {
        this.placeholder.unregister();
    }
}
```

## usage - simple PlaceholderAPI placeholder

For those who simply want to register a PlaceholderAPI placeholder.

Placeholder declaration
```java
// use Player instead of OfflinePlayer if you want to only support online players
public class MyPlaceholder implements BiFunction<OfflinePlayer, String, Object> {

    @NotNull
    public Set<String> getNames() {
        return Set.of("myplaceholder");
    }
    
    @Override
    public Object apply(OfflinePlayer player, String s) {
        return "placeholder result";
    }
}
```

Placeholder registration
```java
public class MyPlugin extends JavaPlugin {

    private final MyPlaceholder myPlaceholder = new MyPlaceholder();

    @Override
    public void onEnable() {
        // If your placeholder use Player
        Placeholders.papi().register(this, this.myPlaceholder.getNames(), this.myPlaceholder);
        // If your placeholder use OfflinePlayer
        Placeholders.papi().registerOffline(this, this.myPlaceholder.getNames(), this.myPlaceholder);
    }

    @Override
    public void onDisable() {
        Placeholders.papi().unregister(this.myPlaceholder.getNames());
    }
}
```
