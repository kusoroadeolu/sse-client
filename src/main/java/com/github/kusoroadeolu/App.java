import com.github.kusoroadeolu.client.SseClient;

void main() {
    SseClient client = SseClient.builder().url("https://sse.dev/test?interval=10").doOnEvent(IO::println).build();
    client.connect();



}

void run() throws IOException {
    throw new IOException();
}