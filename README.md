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
        .setPrefix("event") //The prefix for the attribute key to the JSON payload
        .delay(1) //Delay in seconds to retry when an http ex occurs
        .backoff(1) //Backoff in seconds when an http ex occurs. 
        .maxNumberOfTries(5) //Max number of times to retry on http exceptions before we throw an error
        .maxDelay(5) // Max delay in seconds
        .build(); //Build the client
```
- Backoff: The formula used for back off is `delay * sqrt(backoff)`

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
String jsonData = queue.poll();
String jsonData = queue.peek();
```

## Some other stuff
- The sse client intentionally doesn't parse the given data body, to allow users to be as flexible as they want when dealing with the JSON payload. Though this might incur some boilerplate
- The sse client is inherently thread safe. No need to worry about any threading gotchas
- I actually spent more time on the retry template than the client itself lol
- Built this in a day just for fun lol

## License
MIT License: Feel free to use this as you wish