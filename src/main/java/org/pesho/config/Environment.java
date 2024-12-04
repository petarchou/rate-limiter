package org.pesho.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Environment {
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")  // path to .env file
            .ignoreIfMissing()
            .load();

    public static String getJedisUser() {
        return dotenv.get("JEDIS_USER");
    }

    public static String getJedisPassword() {
        return dotenv.get("JEDIS_PASSWORD");
    }

    public static String getJedisUrl() {
        return dotenv.get("JEDIS_URL");
    }
    public static Integer getJedisPort() {
        return Integer.parseInt(dotenv.get("JEDIS_PORT"));
    }
}
