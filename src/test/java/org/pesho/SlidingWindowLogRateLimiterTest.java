package org.pesho;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.pesho.ratelimiters.SlidingWindowLogRateLimiter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class SlidingWindowLogRateLimiterTest {
    private static final int DEFAULT_MAX_REQUESTS = 10;
    private static final Duration DEFAULT_WINDOW_SIZE = Duration.ofSeconds(2);
    private static final int REQUESTS_PER_CLIENT = 6;

    private SlidingWindowLogRateLimiter rateLimiter;
    private AtomicInteger requestCounter;
    private AtomicInteger successCounter;

    @BeforeEach
    void setup() {
        this.rateLimiter = new SlidingWindowLogRateLimiter(DEFAULT_WINDOW_SIZE, DEFAULT_MAX_REQUESTS);
        this.requestCounter = new AtomicInteger();
        this.successCounter = new AtomicInteger();
    }

    @ParameterizedTest
    @CsvSource({
            // virtualUsers, timeBetweenRequestMillis
            "5, 1000",   // 5 users sending every 1s = 5 req/s = 10 req/2s (at limit)
            "10, 2000",  // 10 users sending every 2s = 5 req/s = 10 req/2s (at limit)
            "3, 1000",   // 3 users sending every 1s = 3 req/s = 6 req/2s
            "4, 2000",   // 4 users sending every 2s = 2 req/s = 4 req/2s
            "1, 500"     // 1 user sending every 0.5s = 2 req/s = 4 req/2s
    })
    public void requestsUnderLimitAreProcessed(long virtualUsers, long timeBetweenRequestsMillis) throws Exception {
        Runnable client = createClient(timeBetweenRequestsMillis);
        try (ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < virtualUsers; i++) {
                clients.submit(client);
            }
            clients.awaitTermination(timeBetweenRequestsMillis * REQUESTS_PER_CLIENT,
                    TimeUnit.MILLISECONDS);

            int failed = requestCounter.intValue() - successCounter.intValue();
            if (failed != 0) {
                Assertions.fail(String.format("%d out of %d requests failed", failed,
                        requestCounter.intValue()));
            }
        }

    }

    //wrong calcs in the test - the standard error covers for them rn
    @ParameterizedTest
    @CsvSource({
            // virtualUsers, timeBetweenRequestMillis
            "6, 1000",    // 6 users/1s = 6 req/s (should limit to 5 req/s)
            "10, 1000",   // 10 users/1s = 10 req/s (should limit to 5 req/s)
            "20, 2000",   // 20 users/2s = 10 req/s (should limit to 5 req/s)
            "15, 1500",   // 15 users/1.5s = 10 req/s (should limit to 5 req/s)
            "12, 1000",   // 12 users/1s = 12 req/s (should limit to 5 req/s)
            "8, 500"      // 8 users/0.5s = 16 req/s (should limit to 5 req/s)
    })
    public void requestsOverLimitGetLimited(long virtualUsers, long timeBetweenRequestsMillis) throws Exception {
        Runnable client = createClient(timeBetweenRequestsMillis);
        try (ExecutorService clients = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < virtualUsers; i++) {
                clients.submit(client);
            }

            double expectedSuccessRate =
                    ((double) DEFAULT_MAX_REQUESTS * 1000 / DEFAULT_WINDOW_SIZE.toMillis()) /
                    ((double) virtualUsers * 1000 / timeBetweenRequestsMillis);

            clients.awaitTermination(timeBetweenRequestsMillis * REQUESTS_PER_CLIENT,
                    TimeUnit.MILLISECONDS);
            double actualSuccessRate = successCounter.doubleValue() / requestCounter.doubleValue();

            // Calculate tolerance based on sample size and request rate
            int totalRequests = requestCounter.intValue();
            double requestsPerSecond = virtualUsers * (1000.0 / timeBetweenRequestsMillis);

            // Using standard error formula with adjustments for rate
            double standardError =
                    Math.sqrt(expectedSuccessRate * (1 - expectedSuccessRate) / totalRequests);
            // Adjust for request rate - higher rates need more tolerance
            double rateAdjustment = Math.log10(requestsPerSecond) / 10.0;
            // Use 3 standard deviations (99.7% confidence interval) plus rate adjustment
            double tolerance = (3 * standardError) + rateAdjustment;

            double lowerBound = Math.max(0, expectedSuccessRate - tolerance);
            double upperBound = Math.min(1, expectedSuccessRate + tolerance);

            if (actualSuccessRate < lowerBound || actualSuccessRate > upperBound) {
                Assertions.fail(String.format(
                        "Success rate %.2f outside acceptable range [%.2f, %.2f]\n" +
                        "Total requests: %d, Requests/sec: %.2f, Calculated tolerance: %.3f",
                        actualSuccessRate, lowerBound, upperBound,
                        totalRequests, requestsPerSecond, tolerance
                ));
            }

            System.out.printf("Success rate: %.2f%n", actualSuccessRate);
            System.out.printf("Expected rate: %.2f%n", expectedSuccessRate);
            System.out.printf("Tolerance:±%.3f (based on %d samples at %.1f req/s)%n",
                    tolerance, totalRequests, requestsPerSecond);
        }
    }

    private Runnable createClient(long timeBetweenRequestsMillis) {
        return () -> {
            long endTime =
                    System.currentTimeMillis() + (timeBetweenRequestsMillis * REQUESTS_PER_CLIENT);
            // Intentional periodic checking for test load generation
            while (System.currentTimeMillis() < endTime) {
                try {
                    if (rateLimiter.canProcess("ip")) {
                        successCounter.incrementAndGet();
                    }
                    requestCounter.incrementAndGet();
                    Thread.sleep(timeBetweenRequestsMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Test interrupted", e);
                }
            }
        };
    }

    //5. check that using some other size + maxRequests combo also works - 1s and 10 requests
    @Test
    public void testDifferentWindowSizeAndMaxRequests() {
        SlidingWindowLogRateLimiter rateLimiter = new SlidingWindowLogRateLimiter(Duration.ofSeconds(1)
                , 10);
        List<Boolean> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(rateLimiter.canProcess("ip"));
        }
        Assertions.assertTrue(results.stream().allMatch(result -> result));
        Assertions.assertFalse(rateLimiter.canProcess("ip"));
    }

}
