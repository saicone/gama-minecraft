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
package com.saicone.bukkit.module.command;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TabFilter {

    @NotNull
    public static TabFilter of(@NotNull String... values) {
        return of(List.of(values));
    }

    @NotNull
    public static TabFilter of(@NotNull Collection<String> values) {
        if (isAscii(values)) {
            return new Ascii(values);
        }

        return new Utf(values);
    }

    @NotNull
    public abstract List<String> get(@NotNull String argument);

    private static boolean isAscii(@NotNull Collection<String> values) {
        for (String value : values) {
            for (int i = 0; i < value.length(); i++) {
                if (value.charAt(i) > 0x7F) {
                    return false;
                }
            }
        }

        return true;
    }

    private static final class Utf extends TabFilter {

        private final Node root = new Node();

        Utf(@NotNull Collection<String> values) {
            for (String value : values) {
                add(value);
            }
        }

        private void add(@NotNull String value) {
            root.matches.add(value);

            Node node = root;

            for (int i = 0; i < value.length(); i++) {
                char c = Character.toLowerCase(value.charAt(i));

                node = node.children.computeIfAbsent(c, k -> new Node());
                node.matches.add(value);
            }
        }

        @Override
        public @NotNull List<String> get(@NotNull String argument) {
            Node node = root;

            for (int i = 0; i < argument.length(); i++) {
                node = node.children.get(Character.toLowerCase(argument.charAt(i)));

                if (node == null) {
                    return Collections.emptyList();
                }
            }

            return Collections.unmodifiableList(node.matches);
        }

        private static final class Node {
            final Map<Character, Node> children = new HashMap<>();
            final List<String> matches = new ArrayList<>();
        }
    }

    private static final class Ascii extends TabFilter {

        private final Node root = new Node();

        Ascii(@NotNull Collection<String> values) {
            for (String value : values) {
                add(value);
            }
        }

        private void add(@NotNull String value) {
            root.matches.add(value);

            Node node = root;

            for (int i = 0; i < value.length(); i++) {
                char c = lowercase(value.charAt(i));

                Node child = node.children[c];

                if (child == null) {
                    child = new Node();
                    node.children[c] = child;
                }

                node = child;
                node.matches.add(value);
            }
        }

        @Override
        public @NotNull List<String> get(@NotNull String argument) {
            Node node = root;

            for (int i = 0; i < argument.length(); i++) {
                char c = argument.charAt(i);

                if (c > 0x7F) {
                    return Collections.emptyList();
                }

                node = node.children[lowercase(c)];

                if (node == null) {
                    return Collections.emptyList();
                }
            }

            return Collections.unmodifiableList(node.matches);
        }

        private static char lowercase(char c) {
            return c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c;
        }

        private static final class Node {
            final Node[] children = new Node[128];
            final List<String> matches = new ArrayList<>();

        }
    }
}
