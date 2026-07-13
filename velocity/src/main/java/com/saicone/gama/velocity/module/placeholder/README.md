# velocity placeholder module

* multiple placeholder processor support
* placeholder text detection
* placeholder text replacement
* static and dynamic placeholder registration

## dependencies

* common placeholder module
* MiniPlaceholders: https://github.com/MiniPlaceholders/MiniPlaceholders

## usage - register placeholder

First, create a class that extends `VelocityPlaceholder`.

If you want a placeholder for online players, implement `PlayerPlaceholder` and use `Player` as the first parameter of `VelocityPlaceholder`.

Take in count that the provided `Player` can be null due the compatibility with global placeholders, if you online want to accept only connected players, you can override the `acceptNull` method to return false.

```java
public class MyPlaceholder extends VelocityPlaceholder<Player> implements PlayerPlaceholder {

    public MyPlaceholder(@NotNull ProxyServer server, @NotNull Object plugin) {
        super(server, plugin);

        // --- Add processors to register this placeholder on
        // MiniPlaceholders
        this.processors().add(Placeholders.mini());
    }
}
```

Then you can use your placeholder to parse any object by its key.

Let's assume the placeholder name is `example`, and you put the key `greeting` with a value.

The value will be resolved on:
* MiniPlaceholders as `<example_greeting>`.

```java
ProxyServer server = ...;
Object plugin = ...;
MyPlaceholder placeholder = new MyPlaceholder(server, plugin);

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
* MiniPlaceholders provides a `Iterator<String>`.

If you only accept one argument, you can only extend the method that accept the `String` "parameters".

In this example, the value will be resolved on:
* MiniPlaceholders as `<example_sum:<arg1>:<arg2>:<arg3>...etc>`.

```java
ProxyServer server = ...;
Object plugin = ...;
MyPlaceholder placeholder = new MyPlaceholder(server, plugin);

placeholder.put("sum", new Placeholder<Player>() {
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
public class MyPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private final MyPlaceholder placeholder;

    @Inject
    public MyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        this.placeholder = new MyPlaceholder(server, this);
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        // Put keys on your placeholder before registering it
        this.placeholder.put("greeting", "Hello, world!");

        // Simple register, use plugin name as placeholder name
        this.placeholder.register();
        // Or register with a custom name (compatible with multiple names)
        this.placeholder.register("example", "test");
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        this.placeholder.unregister();
    }
}
```
