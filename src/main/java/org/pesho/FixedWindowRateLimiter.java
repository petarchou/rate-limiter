package org.pesho;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter implements RateLimiter {
    private static final int requestsPerWindow = 11;
    ConcurrentMap<LocalDateTime, AtomicInteger> timeWindows = new ConcurrentHashMap<>();
    public FixedWindowRateLimiter() {
        setUpWindowCleaner();
    }
    @Override
    public boolean canProcess(String ip) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        timeWindows.putIfAbsent(now, new AtomicInteger(0));

        int requests = timeWindows.get(now).incrementAndGet();
        return requests <= requestsPerWindow;
    }

    private void setUpWindowCleaner() {
        Runnable cleaner = () -> {
            while(true) {
             try {
                 Thread.sleep(Duration.ofMinutes(1));
                 LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
                 for (LocalDateTime timestamp : timeWindows.keySet()) {
                     if(timestamp.isBefore(now)) {
                         timeWindows.remove(timestamp);
                     }
                 }
             } catch(InterruptedException e) {
                 throw new RuntimeException();
             }
            }
        };
        new Thread(cleaner).start();
    }
}
