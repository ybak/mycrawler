package org.ybak.crawler.web;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class BoundedExecutor {
    private final Executor exec;
    private final Semaphore semaphore;

    public BoundedExecutor(int bound) {
        this.exec = Executors.newCachedThreadPool();
        this.semaphore = new Semaphore(bound);
    }

    public void submitTask(final Runnable command) {
        try {
            semaphore.acquire();
            exec.execute(() -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            });
        } catch (Exception e) {
            semaphore.release();
            throw new RuntimeException(e);
        }
    }
}