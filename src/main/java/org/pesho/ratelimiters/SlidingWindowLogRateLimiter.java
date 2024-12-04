package org.pesho.ratelimiters;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.*;

public class SlidingWindowLogRateLimiter implements RateLimiter {

    private final Duration windowSize;
    private final int requestsPerWindow;
    private final ConcurrentMap<String, BlockingQueue<LocalDateTime>> ipMap;

    public SlidingWindowLogRateLimiter(Duration windowSize, int requestsPerWindow) {
        this.windowSize = windowSize;
        this.requestsPerWindow = requestsPerWindow;
        this.ipMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean canProcess(String ip) {
        LocalDateTime now = LocalDateTime.now();
        ipMap.putIfAbsent(ip, new PriorityBlockingQueue<>(requestsPerWindow*2,
                Comparator.naturalOrder()));
        BlockingQueue<LocalDateTime> queue = ipMap.get(ip);

        //potential optimization - remove synchronized block
        synchronized (queue) {
            while(!queue.isEmpty() && queue.peek().isBefore(now.minus(windowSize))) {
                queue.poll();
            }
            if (queue.size() >= requestsPerWindow) {
                return false;
            }
        }

        queue.add(now);
        return true;
    }
}
