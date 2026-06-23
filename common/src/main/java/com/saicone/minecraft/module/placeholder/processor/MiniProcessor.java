/*
 * This file is part of PixelBuy, licensed under the MIT License
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
package com.saicone.minecraft.module.placeholder.processor;

import com.saicone.minecraft.module.placeholder.ComposedPlaceholder;
import com.saicone.minecraft.module.placeholder.NamedPlaceholder;
import com.saicone.minecraft.module.placeholder.Placeholder;
import com.saicone.minecraft.module.placeholder.PlaceholderProcessor;
import com.saicone.minecraft.module.placeholder.TypedPlaceholder;
import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.utils.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MiniProcessor implements PlaceholderProcessor {

    public static final MiniProcessor INSTANCE = new MiniProcessor();
    private static final Pattern PATTERN = Pattern.compile("^<([a-zA-Z0-9_]+)(?::[^>]*)?>$");

    // lazy init var
    private final Supplier<Boolean> enabled = new Supplier<>() {
        private volatile Boolean value;

        @Override
        public Boolean get() {
            if (value == null) {
                synchronized (this) {
                    if (value == null) {
                        try {
                            Class.forName("io.github.miniplaceholders.api.MiniPlaceholders");
                            value = true;
                        } catch (Throwable t) {
                            value = false;
                        }
                    }
                }
            }
            return value;
        }
    };
    private final Map<String, Object> expansions = new HashMap<>();

    public boolean enabled() {
        return enabled.get();
    }

    public boolean contains(@NotNull String s) {
        if (!enabled()) {
            return false;
        }

        // TODO: Optimize this
        final Set<String> names = MiniPlaceholders.expansionsAvailable().stream().map(Expansion::name).collect(Collectors.toSet());

        final Matcher matcher = PATTERN.matcher(s);
        while (matcher.find()) {
            final String id = matcher.group(1);
            if (names.contains(id)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public TagResolver globalPlaceholders() {
        if (enabled()) {
            return MiniPlaceholders.globalPlaceholders();
        } else {
            return TagResolver.empty();
        }
    }

    @NotNull
    public TagResolver audiencePlaceholders() {
        if (enabled()) {
            return MiniPlaceholders.audiencePlaceholders();
        } else {
            return TagResolver.empty();
        }
    }

    @NotNull
    public TagResolver audienceGlobalPlaceholders() {
        if (enabled()) {
            return MiniPlaceholders.audienceGlobalPlaceholders();
        } else {
            return TagResolver.empty();
        }
    }

    @Override
    public void register(@NotNull NamedPlaceholder<?> placeholder) {
        if (!enabled()) {
            return;
        }

        for (String name : placeholder.names()) {
            register(name, placeholder);
        }
    }

    private void register(@NotNull String name, @NotNull NamedPlaceholder<?> placeholder) {
        final Expansion.Builder builder = Expansion.builder(name)
                .author(placeholder.author())
                .version(placeholder.version());
        register(builder, "", placeholder);

        final Expansion expansion = builder.build();
        expansion.register();
        this.expansions.put(name, expansion);
    }

    @SuppressWarnings("unchecked")
    private void register(@NotNull Object builder, @NotNull String key, @NotNull Placeholder<?> placeholder) {
        if (placeholder instanceof ComposedPlaceholder<?>) {
            if (!key.isEmpty()) {
                key = key + "_";
            }

            final ComposedPlaceholder<?> composed = (ComposedPlaceholder<?>) placeholder;
            for (Map.Entry<String, ? extends Placeholder<?>> entry : composed.children().entrySet()) {
                register(builder, key + entry.getKey(), entry.getValue());
            }
        } else if (placeholder instanceof TypedPlaceholder<?>) {
            final TypedPlaceholder<Object> typed = (TypedPlaceholder<Object>) placeholder;
            final Expansion.Builder expansion = (Expansion.Builder) builder;
            // accept audience
            expansion.audiencePlaceholder(key, (audience, queue, ctx) -> {
                final Object t = typed.parse(audience);
                if (t == null && !typed.acceptNull()) {
                    return null;
                }

                final Object result = typed.get(t, queueToIterator(queue));
                return (Tag) objectToTag(result);
            });
            // accept global
            if (typed.acceptNull()) {
                expansion.globalPlaceholder(key, (queue, ctx) -> {
                    final Object result = typed.get(null, queueToIterator(queue));
                    return (Tag) objectToTag(result);
                });
            }
        } else {
            final Placeholder<Object> objPlaceholder = (Placeholder<Object>) placeholder;
            final Expansion.Builder expansion = (Expansion.Builder) builder;
            // headless audience placeholder
            expansion.audiencePlaceholder(key, (audience, queue, ctx) -> {
                try {
                    final Object result = objPlaceholder.get(audience, queueToIterator(queue));
                    return (Tag) objectToTag(result);
                } catch (ClassCastException e) {
                    return null;
                }
            });
            // global placeholder
            expansion.globalPlaceholder(key, (queue, ctx) -> {
                final Object result = objPlaceholder.get(null, queueToIterator(queue));
                return (Tag) objectToTag(result);
            });
        }
    }

    @NotNull
    private Iterator<String> queueToIterator(@NotNull Object object) {
        return new Iterator<>() {
            private final ArgumentQueue queue = (ArgumentQueue) object;

            @Override
            public boolean hasNext() {
                return queue.hasNext();
            }

            @Override
            public String next() {
                return queue.pop().value();
            }
        };
    }

    @NotNull
    private Object objectToTag(@Nullable Object object) {
        final Tag tag;
        if (object == null) {
            tag = Tags.EMPTY_TAG;
        } else if (object instanceof Tag) {
            tag = (Tag) object;
        } else if (object instanceof Component) {
            tag = Tag.selfClosingInserting((Component) object);
        } else if (object instanceof ComponentLike) {
            tag = Tag.selfClosingInserting((ComponentLike) object);
        } else if (object instanceof String) {
            tag = Tag.preProcessParsed((String) object);
        } else {
            tag = Tag.preProcessParsed(object.toString());
        }
        return tag;
    }

    @Override
    public void unregister(@NotNull NamedPlaceholder<?> placeholder) {
        unregister(placeholder.names());
    }

    @NotNull
    public <T extends Collection<String>> T unregister(@NotNull T names) {
        if (!enabled()) {
            return names;
        }

        for (String name : names) {
            final Expansion expansion = (Expansion) this.expansions.remove(name);
            if (expansion != null && expansion.registered()) {
                expansion.unregister();
            }
        }

        return names;
    }
}
