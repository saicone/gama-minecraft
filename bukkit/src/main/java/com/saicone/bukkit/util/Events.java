/*
 * MIT License.
 *
 * Copyright (c) 2026 Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class Events {

    public static Plugin PLUGIN = null;
    private static final org.bukkit.event.Listener LISTENER = new org.bukkit.event.Listener() { };
    private static final Set<Event> IGNORED_EVENTS = new HashSet<>();

    Events() {
    }

    @NotNull
    public static <E extends Event> Listener<E> register(@NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
        return register(EventPriority.NORMAL, false, eventClass, consumer);
    }

    @NotNull
    public static <E extends Event> Listener<E> register(@NotNull EventPriority priority, @NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
        return register(priority, false, eventClass, consumer);
    }

    @NotNull
    public static <E extends Event> Listener<E> register(boolean ignoreCancelled, @NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
        return register(EventPriority.NORMAL, ignoreCancelled, eventClass, consumer);
    }

    @NotNull
    public static <E extends Event> Listener<E> register(@NotNull EventPriority priority, boolean ignoreCancelled, @NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
        final Executor<E> executor = new Executor<>(eventClass, consumer);
        final Listener<E> listener = new Listener<>(getHandlers(eventClass), executor, priority, ignoreCancelled);

        listener.register();

        return listener;
    }

    @NotNull
    public static HandlerList getHandlers(@NotNull Class<? extends Event> eventClass) {
        for (Field field : eventClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && HandlerList.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (HandlerList) field.get(null);
                } catch (Throwable t) {
                    throw new IllegalStateException("Cannot access HandlerList on event class: " + eventClass, t);
                }
            }
        }
        throw new IllegalArgumentException("Cannot find HandlerList on event class: " + eventClass);
    }

    public static boolean isIgnored(@NotNull Event event) {
        return IGNORED_EVENTS.contains(event);
    }

    @NotNull
    public static <E extends Event> E ignoreAndCall(@NotNull E event) {
        IGNORED_EVENTS.add(event);
        try {
            Bukkit.getPluginManager().callEvent(event);
        } finally {
            IGNORED_EVENTS.remove(event);
        }
        return event;
    }

    public static final class Executor<E extends Event> implements EventExecutor {

        private final Class<E> eventClass;
        private final Consumer<E> consumer;

        public Executor(@NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
            this.eventClass = eventClass;
            this.consumer = consumer;
        }

        @NotNull
        public Class<E> getEventClass() {
            return eventClass;
        }

        @NotNull
        public Consumer<E> getConsumer() {
            return consumer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void execute(@NotNull org.bukkit.event.Listener listener, @NotNull Event event) throws EventException {
            if (!this.eventClass.isInstance(event) || IGNORED_EVENTS.contains(event)) {
                return;
            }
            consumer.accept((E) event);
        }
    }

    public static final class Listener<E extends Event> extends RegisteredListener {

        private final HandlerList handler;

        public Listener(
                @NotNull HandlerList handler,
                @NotNull Executor<E> executor,
                @NotNull EventPriority priority,
                boolean ignoreCancelled
        ) {
            super(Events.LISTENER, executor, priority, Events.PLUGIN, ignoreCancelled);
            this.handler = handler;
        }

        @NotNull
        public HandlerList getHandler() {
            return handler;
        }

        @Override
        @SuppressWarnings("unchecked")
        public @NotNull Executor<E> getExecutor() {
            return (Executor<E>) super.getExecutor();
        }

        public void register() {
            handler.register(this);
        }

        public void unregister() {
            handler.unregister(this);
        }
    }
}
