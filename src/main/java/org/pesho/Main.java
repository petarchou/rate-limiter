import org.pesho.servers.EmbeddedLimiterServer;
import org.pesho.config.RedisClient;


static void main() {
    RedisClient.connect();
    new EmbeddedLimiterServer().start();
}