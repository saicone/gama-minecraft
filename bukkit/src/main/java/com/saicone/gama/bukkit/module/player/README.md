# bukkit player module

* player name lookup
* player uuid lookup

## dependencies

* EssentialsX: https://github.com/essentialsx/essentials
* LuckPerms: https://luckperms.net/wiki/Developer-API

## initialization

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        PlayerProvider.compute();
    }
}
```
