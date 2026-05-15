# bukkit settings module

* flexible settings system
* extensive path lookup options

## dependencies

* types: https://github.com/saicone/types

## usage

```java
public class MyPlugin extends JavaPlugin {

    private final SettingsFile settings = new SettingsFile("settings.yml", true);

    @Override
    public void onLoad() {
        this.settings.loadFrom(getDataFolder(), true);
    }

    @NotNull
    public SettingsFile getSettings() {
        return settings;
    }
}
```
