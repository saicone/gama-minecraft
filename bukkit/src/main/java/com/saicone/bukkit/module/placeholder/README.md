# bukkit placeholder module

* only execute something when PlaceholderAPI is present
* placeholder text detection
* placeholder text replacement
* placeholder registration

## dependencies

* PlaceholderAPI: https://github.com/PlaceholderAPI/PlaceholderAPI

## usage - registering a placeholder

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
        Placeholders.register(this, this.myPlaceholder.getNames(), this.myPlaceholder);
        // If your placeholder use OfflinePlayer
        Placeholders.registerOffline(this, this.myPlaceholder.getNames(), this.myPlaceholder);
    }

    @Override
    public void onDisable() {
        Placeholders.unregister(this.myPlaceholder.getNames());
    }
}
```
