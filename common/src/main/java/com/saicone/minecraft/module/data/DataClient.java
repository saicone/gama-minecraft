package com.saicone.minecraft.module.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface DataClient {

    default void load(@NotNull Map<String, Object> config) {
        // empty default method
    }

    default void start() {
        // empty default method
    }

    default void close() {
        // empty default method
    }
}
