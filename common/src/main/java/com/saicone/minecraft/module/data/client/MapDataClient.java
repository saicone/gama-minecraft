package com.saicone.minecraft.module.data.client;

import com.saicone.minecraft.module.data.DataClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface MapDataClient extends DataClient {

    @NotNull
    Map<String, Object> loadMap(@NotNull UUID user);

    @Nullable
    Object loadMapValue(@NotNull UUID user, @NotNull String key);

    void saveMap(@NotNull UUID user, @NotNull Map<String, Object> map);

    void saveMapEntry(@NotNull UUID user, @NotNull String key, @Nullable Object value);
}
