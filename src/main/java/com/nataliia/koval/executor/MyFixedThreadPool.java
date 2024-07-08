package com.nataliia.koval.executor;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.IntStream;


@Slf4j
public class MyFixedThreadPool implements ExecutorService {
    private volatile boolean isShutdown = false;
    private final Queue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final List<Thread> threads = new CopyOnWriteArrayList<>();


    public MyFixedThreadPool(int threadsCount) {
        log.info("Initializing MyFixedThreadPool with {} threads", threadsCount);
        IntStream.range(0, threadsCount).mapToObj(i -> new Thread(new TaskExecutor(tasks), "my-pool-" + (i + 1)))
                .peek(threads::add)
                .forEach(Thread::start);
    }


    @Override
    public void execute(Runnable task) {
        if (isShutdown) {
            throw new RejectedExecutionException("Cannot accept new tasks, the pool is shutting down");
        }
        tasks.add(task);

        synchronized (tasks) {
            tasks.notifyAll();
        }
    }

    @Override
    public void shutdown() {
        synchronized (tasks) {
            tasks.notifyAll();
        }
        isShutdown = true;
    }


    @Override
    public boolean isShutdown() {
        return isShutdown;
    }


    @Override
    public boolean isTerminated() {
        return threads.stream().noneMatch(Thread::isAlive);
    }



    class TaskExecutor implements Runnable {
        private final Queue<Runnable> tasks;

        public TaskExecutor(Queue<Runnable> tasks) {
            this.tasks = tasks;
        }

        @Override
        public void run() {
            try {
                while (!isShutdown || !tasks.isEmpty()) {
                    Runnable task;
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                    }
                    if (task != null) {
                        task.run();
                    }
                }
            } catch (InterruptedException e) {
                log.error("Thread {} interrupted. Error: {}", Thread.currentThread().getName(), e.getMessage());
            } catch (Exception e) {
                log.error("Exception occurred in thread {}: Error: {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
    }


    @Override
    public List<Runnable> shutdownNow() {
        log.info("Force shutting down MyFixedThreadPool");

        isShutdown = true;

        for (Thread thread : threads) {
            thread.interrupt();
        }

        List<Runnable> remainingTasks = new CopyOnWriteArrayList<>();
        synchronized (tasks) {
            while (!tasks.isEmpty()) {
                remainingTasks.add(tasks.poll());
            }
        }
        return remainingTasks;
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


// ---------------------------- not done


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
