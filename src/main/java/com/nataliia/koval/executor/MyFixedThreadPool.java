package com.nataliia.koval.executor;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;


@Slf4j
public class MyFixedThreadPool implements ExecutorService {
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final Queue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final List<Thread> threads = new CopyOnWriteArrayList<>();


    public MyFixedThreadPool(int threadsCount) {
        log.info("Initializing MyFixedThreadPool with {} threads", threadsCount);
        IntStream.range(0, threadsCount)
                .mapToObj(i -> new Thread(new TaskExecutor(tasks, isShutdown), "my-pool-" + (i + 1)))
                .peek(threads::add)
                .forEach(Thread::start);
    }


    @Override
    public void execute(Runnable task) {
        if (isShutdown.get()) {
            throw new RejectedExecutionException("Cannot accept new tasks, the pool is shutting down");
        }
        synchronized (tasks) {
            tasks.add(task);
            tasks.notifyAll();
            log.info("Task added to the queue");
        }
    }

    @Override
    public void shutdown() {
        log.info("Shutting down MyFixedThreadPool");

        if (isShutdown.compareAndSet(false, true)) {
            for (Thread thread : threads) {
                thread.interrupt();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                    log.info("Thread {} has shutdown", thread.getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("InterruptedException during shutdown: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean isShutdown() {
        return isShutdown.get();
    }


    @Override
    public List<Runnable> shutdownNow() {
        log.info("Force shutting down MyFixedThreadPool");
        if (!isShutdown.get()) {
            shutdown();
        }

        List<Runnable> remainingTasks = new ArrayList<>();
        synchronized (tasks) {
            while (!tasks.isEmpty()) {
                remainingTasks.add(tasks.poll());
            }
        }
        return remainingTasks;
    }


    @Override
    public boolean isTerminated() {
        return threads.stream().noneMatch(Thread::isAlive);
    }


    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long endTime = System.nanoTime() + nanos;

        log.info("Waiting for MyFixedThreadPool to terminate");
        synchronized (tasks) {
            while (!isTerminated()) {
                if (nanos <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(tasks, nanos);
                nanos = endTime - System.nanoTime();
            }
        }

        return true;
    }


    static class TaskExecutor implements Runnable {

        private final Queue<Runnable> tasks;
        private final AtomicBoolean isShutdown;

        public TaskExecutor(Queue<Runnable> tasks, AtomicBoolean isShutdown) {
            this.tasks = tasks;
            this.isShutdown = isShutdown;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Runnable task;
                    synchronized (tasks) {
                        while (tasks.isEmpty() && !isShutdown.get()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                    }
                    if (task != null) {
                        log.info("{} is running the task", Thread.currentThread().getName());
                        task.run();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread {} interrupted", Thread.currentThread().getName());
            } catch (Exception e) {
                log.error("Exception occurred in thread {}: Error: {}", Thread.currentThread().getName(), e.getMessage());
            }

        }
    }


        // ---------------------------- not done yet


        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return null;
        }

        @Override
        public Future<?> submit(Runnable task) {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return List.of();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return List.of();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
