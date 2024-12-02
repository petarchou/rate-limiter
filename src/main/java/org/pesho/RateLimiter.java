package org.pesho;

public interface RateLimiter {
    public boolean canProcess(String ip);
}
