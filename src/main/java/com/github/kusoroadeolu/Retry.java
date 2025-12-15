package com.github.kusoroadeolu;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Math.sqrt;

@FunctionalInterface
public interface Retry {
    void run() throws Exception;

    final static ScheduledExecutorService EXEC = Executors.newScheduledThreadPool(10000, Thread.ofVirtual().factory());

    static void retry(Retry retry ,RetryTemplate rt) throws RetryFailedException {
        final var exceptionSet = Arrays.stream(rt.getRetryFor()).collect(Collectors.toSet());
        var delay = 0L;
        Exception throwable = null;

        for (int numOfTries = 0; numOfTries < rt.getMaxNumberOfTries(); numOfTries++){
            final var future = EXEC.schedule(() -> {
                try {
                    retry.run();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }, delay, TimeUnit.SECONDS);


            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                if (numOfTries == 0) delay = rt.getDelay();
                var cause = e.getCause().getCause();
                if (exceptionSet.contains(cause.getClass())){
                    delay = Math.min(rt.getMaxDelay(), delay * (long) sqrt(rt.getBackoff()));
                    throwable = e;
                    continue;
                }

                throw new RetryFailedException(e);
            }

            return;
        }

        throw new RetryFailedException(throwable);
    }
}
