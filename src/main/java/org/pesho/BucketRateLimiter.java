package org.pesho;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BucketRateLimiter implements RateLimiter {
    private static final int maxTokens = 10;
    private static final int tokensPerSec = 10;
    private final ConcurrentMap<String, AtomicInteger> tokenBuckets = new ConcurrentHashMap<>();

    public boolean canProcess(String ip) {
        AtomicInteger bucket = tokenBuckets.putIfAbsent(ip, new AtomicInteger(10));
        if (bucket == null) {
            bucket = tokenBuckets.get(ip);
            synchronized (bucket) {
                setUpRefiller(bucket);
            }
        }
        synchronized (bucket) {
            int tokens = bucket.get();
            if (tokens <= 0) {
                return false;
            }
            bucket.decrementAndGet();
        }

        return true;
    }

    private void setUpRefiller(AtomicInteger bucket) {
        Runnable refiller = () -> {
            while(true) {
                try {
                    Thread.sleep(1000);
                    synchronized (bucket) {
                        int tokens = bucket.get();
                        bucket.set(Math.min(maxTokens, tokens + tokensPerSec));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        new Thread(refiller).start();
    }
}
