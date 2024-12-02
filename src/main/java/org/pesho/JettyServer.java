package org.pesho;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class JettyServer {
    public void start() {
        Server server = new Server();
        try (ServerConnector c = new ServerConnector(server)) {
            c.setIdleTimeout(1000);
            c.setAcceptQueueSize(10);
            c.setPort(8080);
            c.setHost("localhost");
            ServletContextHandler handler = new ServletContextHandler("", true, false);
            ServletHolder limitedHolder = new ServletHolder(Main.LimitedServlet.class);
            ServletHolder unlimitedHolder = new ServletHolder(Main.UnlimitedServlet.class);
            handler.addServlet(limitedHolder, "/limited");
            handler.addServlet(unlimitedHolder, "/unlimited");
            server.setHandler(handler);
            server.addConnector(c);
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
