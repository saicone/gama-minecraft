# bukkit scheduler module

* full java async scheduler, no ticks time unit
* later tasks
* repeating tasks
* linked tasks, a repeating task but the delay of the next execution is calculated from the previous execution
* locked tasks, a repeating task that will not execute if the previous execution is still running

## initialization

```java
public class MyPlugin extends JavaPlugin {

    private final PluginScheduler scheduler;
    
    public MyPlugin() {
        // using 10 as parallelism, but you can use any number you want
        // or even 0 to use the number of available processors
        this.scheduler = new PluginScheduler(this, 10);
    }
    
    @Override
    public void onDisable() {
        this.scheduler.shutdown();
    }
}
```
