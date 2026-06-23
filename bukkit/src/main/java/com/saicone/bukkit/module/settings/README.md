# bukkit settings module

* flexible settings system
* extensive path lookup options
* yaml walker with config inheritance

## dependencies

* types: https://github.com/saicone/types

## usage - SettingsFile

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

## usage - BukkitYamlWalker

The files inside the folder can inherit the configuration from `.root.yml` file, and the subfolders can also have their own `.root.yml` file to be inherited by the files inside it.

```java
File folder = ...;

BukkitYamlWalker walker = new BukkitYamlWalker(folder);

// for loop, can handle folders with less than 1000 files, more than that may cause performance issues
for (YamlConfiguration config : walker) {
    // do something
}

// walk through the folder, can read any size of files
try {
    walker.walk(config -> {
        // do something
    });
} catch (IOException e) {
    // handle exception
}
```

```java
YamlConfiguration config = ...;

// set a String parser for any configuration that is read
BukkitYamlWalker.parse(config, s -> {
    // do something with the string, for example, replace placeholders
    return s.replace("[player]", "Steve");
});

// then loop or walk through the folder, every configuration will have this String values parsed

for (YamlConfiguration config : walker) {
    // do something
}

try {
    walker.walk(config -> {
        // do something
    });
} catch (IOException e) {
    // handle exception
}
```
