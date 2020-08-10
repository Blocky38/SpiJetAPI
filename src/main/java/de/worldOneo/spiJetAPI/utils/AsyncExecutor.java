package de.worldOneo.spiJetAPI.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AsyncExecutor {
    private final ThreadPoolExecutor threadPoolExecutor;

    public AsyncExecutor() {
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public AsyncExecutor(int corePoolSize) {
        threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(corePoolSize);
    }

    protected ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }
}