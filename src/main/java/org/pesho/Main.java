package org.pesho;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        new JettyServer().start();
    }

    public static class LimitedServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private final RateLimiter rateLimiter = new FixedWindowRateLimiter();

        @Override
        protected void doGet(final HttpServletRequest req,
                             final HttpServletResponse res) throws ServletException, IOException {
            String ip = req.getRemoteAddr();
            if(!rateLimiter.canProcess(ip)) {
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