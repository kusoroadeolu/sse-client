import com.github.kusoroadeolu.client.SseClient;

void main() {
    try (SseClient client = SseClient.builder().url("https://sse.dev/test?interval=10").doOnEvent(IO::println).build()) {
        client.connect();
    }
}