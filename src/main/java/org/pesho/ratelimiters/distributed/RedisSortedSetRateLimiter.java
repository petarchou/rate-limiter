package org.pesho.ratelimiters.distributed;

import org.pesho.ratelimiters.RateLimiter;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.time.Instant;

import static org.pesho.config.RedisClient.JEDIS;

public class RedisSortedSetRateLimiter implements RateLimiter {
    private final long windowSizeSeconds;
    private final long requestsPerWindow;

    public RedisSortedSetRateLimiter(long windowSizeSeconds, long requestsPerWindow) {
        this.windowSizeSeconds = windowSizeSeconds;
        this.requestsPerWindow = requestsPerWindow;
    }

    @Override
    public boolean canProcess(String ip) {
        long requests = addRequest(ip);
        return requests <= requestsPerWindow;
    }


    /**
     * Increments the request counter if it hasn't yet reached the limit, otherwise does nothing.
     * @param key the unique identifier for the remembered set.
     * @return The latest value of the counter after the potential update.
     */
    private long addRequest(String key) {
        long now = Instant.now().toEpochMilli();
        try (Pipeline pipe = (Pipeline) JEDIS.pipelined()) {
            pipe.zadd(key, now, String.valueOf(now));
            pipe.zremrangeByScore(key, 0, now - windowSizeSeconds*1000);
            Response<Long> response = pipe.zcard(key);
            pipe.sync();

            long requestsInWindow = response.get();
            //remove unprocessed requests
            if(requestsInWindow > requestsPerWindow+1) {
                pipe.zrem(key, String.valueOf(now));
                return requestsInWindow - 1; //skip waiting for sync
            }
            return requestsInWindow;
        }
    }
}
