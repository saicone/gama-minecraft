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
package com.saicone.gama.minecraft.module.data.sql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public interface SqlConnection {

    @NotNull
    static SqlConnection valueOf(@NotNull Object object) {
        if (object instanceof Connection) {
            return new Java((Connection) object);
        }

        // hikari
        try {
            final Class<?> type = Class.forName("com.zaxxer.hikari.HikariDataSource");
            if (type.isInstance(object)) {
                return new Hikari((com.zaxxer.hikari.HikariDataSource) object);
            }
        } catch (Throwable ignored) { }

        throw new IllegalArgumentException("Unsupported connection type: " + object.getClass().getName());
    }

    default boolean isRunning() {
        return true;
    }

    default boolean isClosed() {
        return false;
    }

    void connect(@NotNull SqlConsumer consumer);

    <R> R connect(@NotNull SqlFunction<R> consumer);

    default void close() throws SQLException {
        // empty default method
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(@NotNull Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlFunction<R> {
        @Nullable
        R apply(@NotNull Connection connection) throws SQLException;
    }

    class Java implements SqlConnection {

        private final Connection connection;
        private final ReentrantLock lock = new ReentrantLock();

        public Java(@NotNull Connection connection) {
            this.connection = connection;
        }

        @Override
        public boolean isRunning() {
            return !isClosed();
        }

        @Override
        public boolean isClosed() {
            try {
                return this.connection.isClosed();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void connect(@NotNull SqlConsumer consumer) {
            this.lock.lock();

            try {
                if (this.connection.isClosed()) {
                    return;
                }

                consumer.accept(this.connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                this.lock.unlock();
            }
        }

        @Override
        public <R> R connect(@NotNull SqlFunction<R> consumer) {
            this.lock.lock();

            try {
                if (this.connection.isClosed()) {
                    return null;
                }

                return consumer.apply(this.connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                this.lock.unlock();
            }
        }

        @Override
        public void close() throws SQLException {
            this.connection.close();
        }
    }

    class Hikari implements SqlConnection {

        private final com.zaxxer.hikari.HikariDataSource hikari;

        public Hikari(@NotNull com.zaxxer.hikari.HikariDataSource hikari) {
            this.hikari = hikari;
        }

        @Override
        public boolean isRunning() {
            return hikari.isRunning();
        }

        @Override
        public boolean isClosed() {
            return hikari.isClosed();
        }

        @Override
        public void connect(@NotNull SqlConsumer consumer) {
            if (this.hikari.isClosed()) {
                return;
            }

            try (Connection connection = this.hikari.getConnection()) {
                consumer.accept(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <R> R connect(@NotNull SqlFunction<R> consumer) {
            if (this.hikari.isClosed()) {
                return null;
            }

            try (Connection connection = this.hikari.getConnection()) {
                return consumer.apply(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws SQLException {
            hikari.close();
        }
    }
}
