package org.pesho.servers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.pesho.ratelimiters.RateLimiter;
import org.pesho.ratelimiters.distributed.RedisSortedSetRateLimiter;
import org.pesho.ratelimiters.distributed.RedisStringRateLimiter;

import java.io.IOException;

public class EmbeddedLimiterServer {
    public void start() {
        Server server = new Server();
        try (ServerConnector c = new ServerConnector(server)) {

            c.setIdleTimeout(1000);
            c.setAcceptQueueSize(200);
            c.setPort(8080);
            c.setHost("localhost");
            ServletContextHandler handler = new ServletContextHandler("", true, false);
            ServletHolder limitedHolder = new ServletHolder(LimitedServlet.class);
            ServletHolder unlimitedHolder = new ServletHolder(UnlimitedServlet.class);
            handler.addServlet(limitedHolder, "/limited");
            handler.addServlet(unlimitedHolder, "/unlimited");
            server.setHandler(handler);
            server.addConnector(c);
            server.start();
            System.out.println("Server started");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class LimitedServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private final RateLimiter rateLimiter =
                new RedisSortedSetRateLimiter(1, 12);

        @Override
        protected void doGet(final HttpServletRequest req,
                             final HttpServletResponse res) throws ServletException, IOException {
            String ip = req.getRemoteAddr();
            if (!rateLimiter.canProcess(ip)) {
                res.setStatus(429);
                return;
            }


            res.getWriter()
                    .append("Limited Servlet at your service.")
                    .append("\nYour ip is: ")
                    .append(ip);
        }
    }

    public static class UnlimitedServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(final HttpServletRequest req,
                             final HttpServletResponse res) throws ServletException, IOException {
            res.getWriter()
                    .append("Unlimited Servlet, serving!");
        }
    }
}
