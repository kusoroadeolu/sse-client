package com.github.kusoroadeolu.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.github.kusoroadeolu.client.Retry.retry;
import static java.util.Objects.requireNonNull;

public class SseClient implements AutoCloseable{
    private final HttpClient httpClient;
    private final HttpRequest.Builder cnb;
    private final URI uri;
    private final Consumer<String> doOnEvent;
    private SseClientStatus sseClientStatus;
    private final Runnable doOnComplete;
    private final Consumer<Throwable> doOnError;
    private final ThreadOwningQueue<String> queue;
    private final RetryTemplate rt;
    private String lastEventId;
    private final Lock lock;
    private final String prefix;
    private final int maxNumOfEvents;

    private final static String LAST_EVENT_ID = "Last-Event-ID";
    private final static String NEWLINE = "\n";
    private final static String ID = "id";
    private final static String COLON = ":";


     SseClient(SseClientBuilder builder) {
        this.httpClient = HttpClient.newHttpClient();
        uri = builder.uri;
        this.cnb = this.createRequest(builder.headers);
        this.sseClientStatus = SseClientStatus.IDLE;
        this.doOnEvent = builder.doOnEvent;
        this.doOnComplete = builder.doOnClose;
        this.doOnError = builder.doOnError;
        this.rt = this.buildTemplate(builder);
        this.lastEventId = "0";
        this.lock = new ReentrantLock();
        this.prefix = builder.prefix;
        this.maxNumOfEvents = builder.maxNumOfEvents;
         this.queue = new ThreadOwningQueue<>(this.maxNumOfEvents,true);
    }


    public static SseClientBuilder builder(){
         return new SseClientBuilder();
    }

    public void connect() {
         this.setStatus(SseClientStatus.CONNECTED, true);
         try {
             retry(this::connectWithoutRetry, this.rt);
         }catch (RetryFailedException e){
             if (this.doOnError != null) Thread.startVirtualThread(() -> this.doOnError.accept(e));
             throw new SseClientException(e);
         }
    }

    public void close(){
        try {
            this.setStatus(SseClientStatus.CLOSED, false);
        }catch (SseClientException ignored){return;} //Just return here
        if (this.doOnComplete != null) Thread.startVirtualThread(this.doOnComplete);
        this.queue.clear();
        this.httpClient.close();
    }

    public ThreadOwningQueue<String> queue(){
        return this.queue;
    }

    public String lastEventId(){
         return this.lastEventId;
    }

    public SseClientStatus status(){
         this.lock.lock();
         try {
             return this.sseClientStatus;
         }finally {
             this.lock.unlock();
         }
    }

    private void connectWithoutRetry() throws IOException, InterruptedException {
        this.queue.setOwningThread(Thread.currentThread());
        final var connectRequest = this.cnb.header(LAST_EVENT_ID, this.lastEventId).GET().build();
        final var response = this.httpClient.send(connectRequest, HttpResponse.BodyHandlers.ofLines());
        final var lines = response.body();
        lines.forEach(s -> {
            if (s.isBlank()) return;
            final var arr = s.split(NEWLINE);
            final var data = this.getData(arr);
            this.setLastEventId(arr);
            Thread.startVirtualThread(() -> this.doOnEvent.accept(data));
            this.queue.add(data);
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


    private void setStatus(SseClientStatus sseClientStatus, boolean fromConnectMethod){
         if (this.sseClientStatus == SseClientStatus.CLOSED) throw new SseClientException("Client has been closed");
         this.lock.lock();
         try {
             if (this.sseClientStatus == SseClientStatus.CLOSED) throw new SseClientException("Client has been closed");
             if (fromConnectMethod && this.sseClientStatus == SseClientStatus.CONNECTED) throw new SseClientException("Client is already connected");
             this.sseClientStatus = sseClientStatus;
         }finally {
             this.lock.unlock();
         }
    }


    private void setLastEventId(String[] arr){
        final var id = getId(arr);
        if (!id.isBlank()) this.lastEventId = id;
    }

    private String getData(String[] arr){
        return Arrays.stream(arr)
                .filter(str -> str.startsWith(this.prefix))
                .map(str -> str.substring(str.indexOf(COLON) + 1).trim())
                .findFirst()
                .orElse("");
    }

    private String getId(String[] arr){
        return Arrays.stream(arr)
                .filter(str -> str.startsWith(ID))
                .map(str -> str.split(COLON)[1].trim())
                .findFirst()
                .orElse("");
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

    public enum SseClientStatus {
         IDLE,
         CONNECTED,
         CLOSED
    }


    public static class SseClientBuilder{
        private URI uri;
        private Consumer<String> doOnEvent;
        private String prefix = "data";
        private final Map<String, String> headers;
        private Runnable doOnClose;
        private Consumer<Throwable> doOnError;
        private int delay = 2;
        private int backoff = 10;
        private int maxNumberOfTries = 3;
        private int maxDelay = 60;
        private int maxNumOfEvents = 100;

        public SseClientBuilder() {
            headers = new HashMap<>();
        }

        public SseClientBuilder prefix(String prefix) {
            requireNonNull(prefix);
            this.prefix = prefix;
            return this;
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

        public SseClientBuilder maxNumOfEvents(int maxNumOfEvents) {
            assertPositive(maxNumOfEvents);
            this.maxNumOfEvents = maxNumOfEvents;
            return this;
        }

        public SseClientBuilder addHeaders(String header, String value){
            requireNonNull(header);
            requireNonNull(value);
            this.headers.put(header, value);
            return this;
        }

        public SseClientBuilder doOnError(Consumer<Throwable> doOnError) {
            requireNonNull(doOnError);
            this.doOnError = doOnError;
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

        public SseClientBuilder doOnClose(Runnable doOnClose) {
            requireNonNull(doOnClose);
            this.doOnClose = doOnClose;
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

