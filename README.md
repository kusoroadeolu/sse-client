# Sse Client
Sse client is a simple to use but feature rich sse client that I made for fun. Sse client is thread safe.

## How to use
Using the sse client is very honestly simple. 

1. Creating a new sse client

```java
var client = SseClient.builder()
        .url("some-url")
        .doOnEvent(IO::println) //Print when we receive an event
        .doOnError(e -> IO.println(e)) //Print the error
        .doOnClose(() -> doSomething()) //Do something when the sse client closes;
        .maxNumOfEvents(50) //Max number of events the client will hold before throwing exceptions. 
        .addHeaders("Authentication", "SomeAuthHeader")  //Add custom headers
        .prefix("event") //The prefix for the attribute key to the JSON payload
        .delay(1) //Delay in seconds to retry when an http ex occurs
        .backoff(1) //Backoff in seconds when an http ex occurs. 
        .maxNumberOfTries(5) //Max number of times to retry on http exceptions before we throw an error
        .maxDelay(5) // Max delay in seconds
        .build(); //Build the client
```
- Backoff: The formula used for back off is `delay * sqrt(backoff)`
- Prefix: Per-say you have an event payload like
> id: 1234
> event: {"name" : "victor", "age":"100"} 

The sse client strips out `event` and returns the json only payload

2. Connecting to the given url
```java
client.connect(); //Connects to the given url
```

3. Closing the client

```java
client.close(); //Once a client is closed, it cannot be reopened

try(var client = SseClient.builder().build()){
client.connect();
} //Closes here
```

4. Getting the client sseClientStatus
The sse client can be in three modes.
- IDLE: The client has not connected yet but isn't closed
- CONNECTED: The client has connected to the url
- CLOSED: The client has closed and cannot be reopened
```java
var status = client.sseClientStatus();
if(status == CLOSED) doSomething();
```

5. Getting the last event id from the sse client
```java
String lastEventId = client.lastEventId();
doSomething(lastEventId);
```
6. Client Polling
```java
var jsonData = client.poll();
var jsonData = client.poll(5, TimeUnit.SECONDS); //Poll with a timeout
var jsonData = client.peek();
```

### Choosing Between Callbacks and Polling
- **Callbacks**: Best for fast, async processing. Events are delivered on virtual threads.
- **Polling**: Best when you need control over consumption rate. Poll when ready.
- **Both**: Events go to both. Useful if callbacks handle logging while queue handles business logic.

### Thread safety
The clients internal synchronization prevents corruption. 
</br> Queue modifications from `add()` and `clear()` are only allowed from the connecting thread otherwise an `IllegalStateEx` is thrown.
 
### Poll semantics
- The client buffer is bounded by `maxNumOfEvents`
- When full, the client throws
- `poll()` is non-blocking
- Closing the client clears the queue and unblocks pollers

## Some other stuff
- The client handles SSE framing and delivery, but leaves the payload (JSON, text, domain objects) entirely to the user
- All callbacks from the sse client are fired off async on a separate virtual thread
- I actually spent more time on the retry template than the client itself lol
- Designed this as a lightweight but simple to use sse client

## Requirements
Java 25+

## License
MIT License: Feel free to use this as you wish