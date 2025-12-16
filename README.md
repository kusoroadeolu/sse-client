# Sse Client
Sse client is a simple to use but feature rich sse client that I made for fun. Sse client is inherently thread safe.

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
6. Client Queue
You can access JSON events from the client's queue. 
**Note:** Only the thread which connected the client can `add()` to or `clear()` the queue i.e. the thread that called `client.connect()`
```java
ThreadOwningQueue<String> queue = client.queue();
var jsonData = queue.poll();
var jsonData = queue.peek();
```

### Queue semantics
- The queue is bounded by `maxNumOfEvents`
- When full, the client [throws]
- `poll()` is non-blocking
- Closing the client clears the queue and unblocks pollers

## Some other stuff
- The client handles SSE framing and delivery, but leaves the payload (JSON, text, domain objects) entirely to the user
- The sse client is inherently thread safe. No need to worry about any threading gotchas
- All callbacks from the sse client are fired off async on a seperate virtual thread
- I actually spent more time on the retry template than the client itself lol
- Designed this as a lightweight, pragmatic Sse client

## Requirements
Java 25+

## License
MIT License: Feel free to use this as you wish