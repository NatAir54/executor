package com.nataliia.koval.executor;

import lombok.extern.slf4j.Slf4j;
import java.util.stream.IntStream;


@Slf4j
public class Main {
    private static final int TASK_COUNT = 9;


    public static void main(String[] args) {
        MyFixedThreadPool myFixedThreadPool = new MyFixedThreadPool(3);
        log.info("Submitting {} tasks to MyFixedThreadPool", TASK_COUNT);

        IntStream.range(1, TASK_COUNT + 1)
                .mapToObj(number -> (Runnable) () -> {
                    log.info("Task {} started by {}", number, Thread.currentThread().getName());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Thread {} interrupted while running task {}", Thread.currentThread().getName(), number);
                    }
                    log.info("Task {} completed by {}", number, Thread.currentThread().getName());
                })
                .forEach(myFixedThreadPool::execute);

        myFixedThreadPool.shutdown();
    }
}
