# bukkit placeholder module

* multiple placeholder processor support
* placeholder text detection
* placeholder text replacement
* static and dynamic placeholder registration

## dependencies

* PlaceholderAPI: https://github.com/PlaceholderAPI/PlaceholderAPI

## usage - simple PlaceholderAPI placeholder

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
