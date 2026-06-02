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
package com.saicone.bukkit.module.settings;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class BukkitYamlWalker implements Iterable<YamlConfiguration> {

    private static final String ROOT_FILE = ".root.yml";

    private static final MethodHandle SECTION_NAME;
    static {
        MethodHandle sectionName = null;
        try {
            final Field field = MemorySection.class.getDeclaredField("path");
            field.setAccessible(true);
            sectionName = MethodHandles.lookup().unreflectSetter(field);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        SECTION_NAME = sectionName;
    }

    private final BukkitYamlWalker parent;
    private final File folder;

    private Map<String, Object> root;

    public BukkitYamlWalker(@NotNull File folder) {
        this(null, folder);
    }

    protected BukkitYamlWalker(@Nullable BukkitYamlWalker parent, @NotNull File folder) {
        this.parent = parent;
        this.folder = folder;
    }

    protected void computeRoot() {
        final File file = new File(folder, ROOT_FILE);
        if (file.exists()) {
            this.root = YamlConfiguration.loadConfiguration(file).getValues(true);

            if (this.parent != null) {
                for (Map.Entry<String, Object> entry : this.parent.root.entrySet()) {
                    this.root.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @NotNull
    protected YamlConfiguration read(@NotNull File file) {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<String, Object> entry : root.entrySet()) {
            final String path = entry.getKey();
            final Object value = entry.getValue();
            if (!config.contains(path)) {
                config.set(path, value);
            }
        }

        final String name = file.getName().substring(0, file.getName().length() - 4);
        try {
            SECTION_NAME.invoke(config, name);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        return config;
    }

    @Override
    public @NotNull Iterator<YamlConfiguration> iterator() {
        computeRoot();

        final File[] files = folder.listFiles();
        if (files == null) {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public YamlConfiguration next() {
                    throw new NoSuchElementException();
                }
            };
        }

        return new Iterator<>() {

            private int index = 0;
            private File next;
            private Iterator<YamlConfiguration> iterator;

            @Override
            public boolean hasNext() {
                if (iterator != null) {
                    if (iterator.hasNext()) {
                        return true;
                    } else {
                        iterator = null;
                    }
                }

                if (this.next != null) {
                    return true;
                }

                while (index < files.length) {
                    final File file = files[index++];
                    if (file.isDirectory()) {
                        final BukkitYamlWalker child = new BukkitYamlWalker(BukkitYamlWalker.this, file);
                        this.iterator = child.iterator();

                        if (iterator.hasNext()) {
                            return true;
                        } else {
                            this.iterator = null;
                        }
                    } else if (file.getName().endsWith(".yml") && !file.getName().equals(ROOT_FILE)) {
                        this.next = file;
                        return true;
                    }
                }

                this.next = null;
                return false;
            }

            @Override
            public YamlConfiguration next() {
                if (this.iterator != null) {
                    if (this.iterator.hasNext()) {
                        return this.iterator.next();
                    } else {
                        this.iterator = null;
                    }
                }

                if (this.next == null) {
                    throw new NoSuchElementException();
                }

                final YamlConfiguration config = read(this.next);
                this.next = null;
                return config;
            }
        };
    }

    public void walk(@NotNull Consumer<YamlConfiguration> consumer) throws IOException {
        computeRoot();

        final Path folderPath = this.folder.toPath();
        try (Stream<Path> stream = Files.walk(folderPath, 1)) {
            final Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                final Path path = iterator.next();
                if (Files.isDirectory(path) && !path.equals(folderPath)) {
                    final BukkitYamlWalker child = new BukkitYamlWalker(this, path.toFile());
                    child.walk(consumer);
                } else if (Files.isRegularFile(path) && path.toString().endsWith(".yml") && !path.getFileName().toString().equals(ROOT_FILE)) {
                    consumer.accept(read(path.toFile()));
                }
            }
        }
    }

    @NotNull
    public static <T extends ConfigurationSection> T parse(@NotNull T section, @NotNull UnaryOperator<String> operator) {
        for (String key : section.getKeys(false)) {
            final Object value = section.get(key);
            if (value instanceof ConfigurationSection) {
                parse((ConfigurationSection) value, operator);
                section.set(key, value);
            } else if (value instanceof String) {
                final String oldValue = (String) value;
                final String newValue = operator.apply(oldValue);
                if (!oldValue.equals(newValue)) {
                    section.set(key, newValue);
                }
            }
        }
        return section;
    }
}
