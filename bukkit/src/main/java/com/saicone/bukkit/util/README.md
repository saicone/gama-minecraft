# bukkit utility code

* server version detection and comparator
* cross-platform audience for Adventure API
* plugin source lookup

## initialization - Audiences

Requires Adventure API: https://github.com/PaperMC/adventure

```java
public class MyPlugin extends JavaPlugin {
    public MyPlugin() {
        Audiences.PLUGIN = this;
    }
}
```

## initialization - Events

```java
public class MyPlugin extends JavaPlugin {
    public MyPlugin() {
        Events.PLUGIN = this;
    }
}
```
