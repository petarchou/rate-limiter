package org.pesho.ratelimiters.distributed;

import org.pesho.ratelimiters.RateLimiter;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.time.Instant;

import static org.pesho.config.RedisClient.JEDIS;

public class RedisStringRateLimiter implements RateLimiter {

    private final long windowSizeSeconds;
    private final long requestsPerWindow;

    public RedisStringRateLimiter(long windowSizeSeconds, int requestsPerWindow) {
        this.windowSizeSeconds = windowSizeSeconds;
        this.requestsPerWindow = requestsPerWindow;
    }

    @Override
    public boolean canProcess(String ip) {
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = currentTime - (currentTime % windowSizeSeconds);
        long prevWindowStart = windowStart - windowSizeSeconds;

        String currentWindowKey = String.format("rate_limit:%s:%d", ip, windowStart);
        String prevWindowKey = String.format("rate_limit:%s:%d", ip, prevWindowStart);
        long curr, prev;
        try (Pipeline pipe = (Pipeline) JEDIS.pipelined()) {
            Response<Long> currCount = pipe.incr(currentWindowKey);
//            pipe.expire(currentWindowKey, windowSizeSeconds * 2);
            Response<String> prevCount = pipe.get(prevWindowKey);
            pipe.sync();
            curr = currCount.get();
            prev = Math.min(prevCount.get() != null ? Long.parseLong(prevCount.get()) : 0, requestsPerWindow);

            double prevWindowWeight = 1 - (double) (currentTime - windowStart) / windowSizeSeconds;
            double requests = curr + prev * prevWindowWeight;

            boolean canProcess = requests <= requestsPerWindow;
            if(!canProcess) pipe.decr(currentWindowKey);
            return canProcess;
        }

    }
}
