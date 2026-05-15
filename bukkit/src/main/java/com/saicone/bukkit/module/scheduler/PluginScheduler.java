/*
 *  MIT License.
 *
 *  Copyright (c) 2026 Rubenicos
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.saicone.bukkit.module.scheduler;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PluginScheduler {

    private final Plugin plugin;
    private final String schedulerName;
    private final String workerPrefix;
    private final int parallelism;

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;

    public PluginScheduler(@NotNull Plugin plugin, int parallelism) {
        this.plugin = plugin;
        this.schedulerName = plugin.getName().toLowerCase() + "-scheduler";
        this.workerPrefix = plugin.getName().toLowerCase() + "-worker-";
        this.parallelism = parallelism == 0 ? Runtime.getRuntime().availableProcessors() : parallelism;

        this.executor = new ForkJoinPool(this.parallelism, new WorkerThreadFactory(), new ExceptionHandler(), false);

        // Taken from LuckPerms, licensed under MIT license
        final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName(this.schedulerName);
            return thread;
        });
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.scheduledExecutor = scheduler;
    }

    @NotNull
    public Plugin plugin() {
        return plugin;
    }

    public int parallelism() {
        return parallelism;
    }

    @NotNull
    public ExecutorService executor() {
        return executor;
    }

    @NotNull
    public ScheduledExecutorService scheduledExecutor() {
        return scheduledExecutor;
    }

    @NotNull
    public Future<?> run(@NotNull Runnable task) {
        return this.executor.submit(task);
    }

    @NotNull
    public ScheduledFuture<?> runLater(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        return this.scheduledExecutor.schedule(() -> this.executor.execute(command), delay, unit);
    }

    @NotNull
    public ScheduledFuture<?> runTimer(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        return this.scheduledExecutor.scheduleAtFixedRate(() -> this.executor.execute(command), initialDelay, period, unit);
    }

    @NotNull
    public ScheduledFuture<?> runLinkedTimer(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        return this.scheduledExecutor.scheduleWithFixedDelay(() -> this.executor.execute(command), initialDelay, period, unit);
    }

    @NotNull
    public ScheduledFuture<?> runLockedTimer(@NotNull Runnable command, long initialDelay, long period, @NotNull TimeUnit unit) {
        final AtomicBoolean running = new AtomicBoolean(false);
        final Runnable lockedCommand = () -> {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            try {
                command.run();
            } finally {
                running.set(false);
            }
        };
        return runTimer(lockedCommand, initialDelay, period, unit);
    }

    public void shutdown() {
        shutdownScheduler();
        shutdownExecutor();
    }

    // Taken from LuckPerms, licensed under MIT license
    public void shutdownScheduler() {
        this.scheduledExecutor.shutdown();
        try {
            if (!this.scheduledExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                this.plugin.getLogger().severe("Timed out waiting for the " + this.plugin.getName() + " scheduler to terminate");
                reportRunningTasks(thread -> thread.getName().equals(this.schedulerName));
            }
        } catch (InterruptedException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Interrupted while waiting for the " + this.plugin.getName() + " scheduler to terminate", e);
        }
    }

    // Taken from LuckPerms, licensed under MIT license
    public void shutdownExecutor() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(1, TimeUnit.MINUTES)) {
                this.plugin.getLogger().severe("Timed out waiting for the " + this.plugin.getName() + " worker thread pool to terminate");
                reportRunningTasks(thread -> thread.getName().startsWith(this.workerPrefix));
            }
        } catch (InterruptedException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Interrupted while waiting for the " + this.plugin.getName() + " worker thread pool to terminate", e);
        }
    }

    // Taken from LuckPerms, licensed under MIT license
    private void reportRunningTasks(Predicate<Thread> predicate) {
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            if (predicate.test(thread)) {
                this.plugin.getLogger().warning("Thread " + thread.getName() + " is blocked, and may be the reason for the slow shutdown!\n" +
                        Arrays.stream(stack).map(el -> "  " + el).collect(Collectors.joining("\n"))
                );
            }
        });
    }

    // Taken from LuckPerms, licensed under MIT license
    private final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setDaemon(true);
            thread.setName(PluginScheduler.this.workerPrefix + COUNT.getAndIncrement());
            return thread;
        }
    }

    // Taken from LuckPerms, licensed under MIT license
    private final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            PluginScheduler.this.plugin.getLogger().log(Level.WARNING, "Thread " + t.getName() + " threw an uncaught exception", e);
        }
    }
}
