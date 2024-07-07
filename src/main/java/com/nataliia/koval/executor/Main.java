package com.nataliia.koval.executor;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

@Slf4j
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        log.info("Creating MyFixedThreadPool instance in {}", Thread.currentThread().getName());
        MyFixedThreadPool myFixedThreadPool = new MyFixedThreadPool(3);

        int taskCount = 9;
        CountDownLatch latch = new CountDownLatch(taskCount);

        log.info("Submitting {} tasks to MyFixedThreadPool", taskCount);
        IntStream.range(1, taskCount + 1)
                .mapToObj(num -> (Runnable) () -> {
                    log.info("Task {} started by {}", num, Thread.currentThread().getName());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Task {} interrupted", num);
                    }
                    log.info("Task {} completed by {}", num, Thread.currentThread().getName());
                    latch.countDown();
                })
                .forEach(myFixedThreadPool::execute);

        latch.await();

        log.info("All tasks completed. Shutting down MyFixedThreadPool.");
        myFixedThreadPool.shutdown();

        log.info("MyFixedThreadPool is terminated: {}", myFixedThreadPool.isTerminated());
    }
}
