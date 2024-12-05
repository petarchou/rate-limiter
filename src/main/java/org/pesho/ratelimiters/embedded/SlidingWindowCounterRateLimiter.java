package org.pesho.ratelimiters.embedded;

import org.pesho.ratelimiters.RateLimiter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SlidingWindowCounterRateLimiter implements RateLimiter {

    private final Duration windowSize;
    private final int requestsPerWindow;
    private final ConcurrentMap<String, SlidingWindowCounter> ipMap;

    public SlidingWindowCounterRateLimiter(Duration windowSize, int requestsPerWindow) {
        this.windowSize = windowSize;
        this.requestsPerWindow = requestsPerWindow;
        this.ipMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean canProcess(String ip) {
        ipMap.putIfAbsent(ip, new SlidingWindowCounter(requestsPerWindow, windowSize));
        var counter = ipMap.get(ip);
        return counter.process();
    }
}
