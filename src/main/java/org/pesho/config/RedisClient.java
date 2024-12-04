package org.pesho.config;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;

public class RedisClient {
    public static UnifiedJedis JEDIS;

    public static synchronized void connect() {
        if (JEDIS != null) return;

        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .user(Environment.getJedisUser())
                .password(Environment.getJedisPassword())
                .build();

        JEDIS = new UnifiedJedis(
                new HostAndPort(Environment.getJedisUrl(), Environment.getJedisPort()),
                config
        );
    }
}

