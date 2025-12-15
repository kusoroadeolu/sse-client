package com.github.kusoroadeolu.client;

import com.github.kusoroadeolu.RetryFailedException;
import com.github.kusoroadeolu.RetryTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.github.kusoroadeolu.Retry.retry;
import static java.util.Objects.requireNonNull;

public class SseClient{
    private final HttpClient httpClient;
    private final HttpRequest.Builder cnb;
    private final URI uri;
    private final Consumer<String> doOnEvent;
    private final String prefix;
    private final AtomicBoolean isConnected;
    private final Runnable doOnComplete;
    private final Consumer<Throwable> doOnError;
    private final Queue<String> queue;
    private final RetryTemplate rt;
    private String lastEventId;


    private final char JSON_START = '{';
    private final char JSON_END = '}';

    private final static String LAST_EVENT_ID = "Last-Event-ID";


     SseClient(SseClientBuilder builder) {
        this.httpClient = HttpClient.newHttpClient();
        uri = builder.uri;
        this.cnb = this.createRequest(builder.headers);
        this.isConnected = new AtomicBoolean(false);
        this.doOnEvent = builder.doOnEvent;
        this.doOnComplete = builder.doOnComplete;
        this.doOnError = builder.doOnError;
        this.prefix = builder.prefix;
        this.queue = new ArrayBlockingQueue<>(10);
        this.rt = this.buildTemplate(builder);
        this.lastEventId = "0";
    }


    public static SseClientBuilder builder(){
         return new SseClientBuilder();
    }

    public void connect() {
         if (!isConnected.compareAndSet(false, true)) throw new RuntimeException("Already connected!");
         try {
             retry(this::connectWithoutRetry, this.rt);
         }catch (RetryFailedException e){
             if (this.doOnError != null) this.doOnError.accept(e);
             throw new SseClientException(e);
         }
    }

    private void connectWithoutRetry() throws IOException, InterruptedException {
        final var connectRequest = this.cnb.header(LAST_EVENT_ID, this.lastEventId).GET().build();
        final var response = this.httpClient.send(connectRequest, HttpResponse.BodyHandlers.ofLines());
        final var lines = response.body();
        lines.forEach(s -> {
            if (s.isBlank()) return;
            final var bIdx = s.indexOf(JSON_START);
            final var lIdx = s.lastIndexOf(JSON_END) + 1;
            s = s.substring(bIdx, lIdx);

            this.doOnEvent.accept(s);
            this.queue.add(s);
        });
    }

    private HttpRequest.Builder createRequest(Map<String, String> headers){
         final var req = HttpRequest
                 .newBuilder(this.uri)
                 .header("Content-Type", "text/event-stream")
                 .header("Cache-Control", "no-cache");
         if (!headers.isEmpty()) headers.forEach(req::header);
         return req;
    }

    private RetryTemplate buildTemplate(SseClientBuilder builder){
        return RetryTemplate
                .builder()
                .retryFor(IOException.class, InterruptedException.class)
                .backoff(builder.backoff)
                .maxNumberOfTries(builder.maxNumberOfTries)
                .delay(builder.delay)
                .maxDelay(builder.maxDelay)
                .build();
    }




    public static class SseClientBuilder{
        private URI uri;
        private Consumer<String> doOnEvent;
        private final Map<String, String> headers;
        private Runnable doOnComplete;
        private Consumer<Throwable> doOnError;
        private String prefix;
        private int delay = 2;
        private int backoff = 10;
        private int maxNumberOfTries = 3;
        private int maxDelay = 60;

        public SseClientBuilder() {
            headers = new HashMap<>();
        }

        public SseClientBuilder url(String url) {
            requireNonNull(url);
            this.uri = URI.create(url);
            return this;
        }

        public SseClientBuilder doOnEvent(Consumer<String> doOnEvent) {
            requireNonNull(doOnEvent);
            this.doOnEvent = doOnEvent;
            return this;
        }

        public SseClientBuilder addHeaders(String header, String value){
            requireNonNull(header);
            requireNonNull(value);
            this.headers.put(header, value);
            return this;
        }

        public SseClientBuilder setDoOnError(Consumer<Throwable> doOnError) {
            requireNonNull(doOnError);
            this.doOnError = doOnError;
            return this;
        }

        public SseClientBuilder bodyPrefix(String prefix){
            requireNonNull(prefix);
            this.prefix = prefix;
            return this;
        }


        public SseClientBuilder maxNumberOfTries(int numberOfTries) {
            assertPositive(numberOfTries);
            this.maxNumberOfTries = numberOfTries;
            return this;
        }

        public SseClientBuilder maxDelay(int delay) {
            assertPositive(delay);
            this.maxDelay = delay;
            return this;
        }

        public SseClientBuilder delay(int delay) {
            assertPositive(delay);
            this.delay = delay;
            return this;
        }

        public SseClientBuilder backoff(int backoff) {
            assertPositive(backoff);
            this.backoff = backoff;
            return this;
        }

        public SseClientBuilder doOnComplete(Runnable doOnComplete) {
            requireNonNull(doOnComplete);
            this.doOnComplete = doOnComplete;
            return this;
        }

        public SseClient build(){
            return new SseClient(this);
        }

        private void assertPositive(int n){
            if (n < 0) throw new IllegalArgumentException();
        }
    }
}

