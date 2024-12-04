package org.pesho.ratelimiters;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class SlidingWindowCounter {
    private final int requestsPerWindow;
    private final long windowSizeMillis;

    private final AtomicInteger currentWindow;
    private final AtomicInteger prevWindow;
    private long windowStartMillis;

    public SlidingWindowCounter(int requestsPerWindow, Duration windowSize) {
        this.currentWindow = new AtomicInteger();
        this.prevWindow = new AtomicInteger();
        this.requestsPerWindow = requestsPerWindow;
        this.windowSizeMillis = windowSize.toMillis();
        this.windowStartMillis = Instant.now().toEpochMilli();
    }

    public synchronized boolean process() {
        Instant now = Instant.now();
        updateWindow(now);
        int count = currentWindow.get();
        int prevCount = Math.min(prevWindow.get(), requestsPerWindow);
        long nowMillis = now.toEpochMilli();
        double currWeight =
                (double) (nowMillis - windowStartMillis) / windowSizeMillis;
        double prevWeight = 1 - currWeight; //1 - ((now - currWindowStart)/windowSize)

        boolean canProcess =
                count < requestsPerWindow
                && (count * currWeight + prevCount * prevWeight) < requestsPerWindow;
        if (canProcess) {
            currentWindow.incrementAndGet();
        }
        return canProcess;
    }

    private synchronized void updateWindow(Instant time) {
        long now = time.toEpochMilli();
        int windowsToAdd = (int) ((now - windowStartMillis) / windowSizeMillis);
        if(windowsToAdd == 1) {
            prevWindow.set(currentWindow.get());
            currentWindow.set(0);
            windowStartMillis += windowsToAdd*windowSizeMillis;
        } else if (windowsToAdd > 1) {
            prevWindow.set(0);
            currentWindow.set(0);
            windowStartMillis = now;
        }
    }
}
