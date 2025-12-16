package com.github.kusoroadeolu.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ThreadOwningQueue<E> {
    private final ArrayBlockingQueue<E> queue;
    private Thread owningThread;

    public ThreadOwningQueue(int size, boolean fair) {
        this.queue = new ArrayBlockingQueue<>(size, fair);
    }

    void setOwningThread(Thread owningThread){
        this.owningThread = owningThread;
    }

    public boolean add(E e){
        if (!Thread.currentThread().equals(this.owningThread)) {
            throw new IllegalStateException("Queue can only be modified by connecting thread");
        }
       else return this.queue.add(e);
    }

    public E poll(){
        return this.queue.poll();
    }

    public E poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return this.queue.poll(timeout, timeUnit);
    }

    public E peek(){
        return this.queue.peek();
    }

    public void clear(){
        if (!Thread.currentThread().equals(this.owningThread)) {
            throw new IllegalStateException("Queue can only be modified by connecting thread");
        }

        this.queue.clear();
    }

}
