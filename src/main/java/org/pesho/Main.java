import org.pesho.JettyServer;
import org.pesho.config.RedisClient;


static void main() {
        RedisClient.connect();
        new JettyServer().start();
    }