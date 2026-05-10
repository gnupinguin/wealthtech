package io.gnupinguin.nevis.wealthtech.concurrent;

import org.jspecify.annotations.NonNull;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

public class BoundedVirtualThreadExecutor implements Executor {

    private final AsyncTaskExecutor delegate;
    private final Semaphore slots;
    private final int capacity;

    public BoundedVirtualThreadExecutor(@NonNull String threadNamePrefix, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Executor capacity must be positive");
        }

        var executor = new SimpleAsyncTaskExecutor(threadNamePrefix);
        executor.setVirtualThreads(true);
        this.delegate = executor;
        this.slots = new Semaphore(capacity);
        this.capacity = capacity;
    }

    @Override
    public void execute(@NonNull Runnable command) {
        delegate.execute(() -> {
            acquireSlot();
            try {
                command.run();
            } finally {
                releaseSlot();
            }
        });
    }

    public int runningTasks() {
        return capacity - availableSlots();
    }

    public int capacity() {
        return capacity;
    }

    public int availableSlots() {
        return slots.availablePermits();
    }

    private void acquireSlot() {
        try {
            slots.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while waiting for executor capacity", e);
        }
    }

    private void releaseSlot() {
        slots.release();
    }

}
