import com.github.kusoroadeolu.Retry;
import com.github.kusoroadeolu.RetryFailedException;
import com.github.kusoroadeolu.RetryTemplate;
import com.github.kusoroadeolu.client.SseClient;

void main() throws IOException, InterruptedException {
    SseClient client = SseClient.builder().url("https://sse.dev/test?interval=10").doOnEvent(IO::println).build();
    client.connect();

//Retry.retry(this::run, RetryTemplate.builder().retryFor(IOException.class).build());
}

void run() throws IOException {
    throw new IOException();
}