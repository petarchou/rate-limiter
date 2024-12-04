package org.pesho.ratelimiters;

public interface RateLimiter {
    public boolean canProcess(String ip);
}
