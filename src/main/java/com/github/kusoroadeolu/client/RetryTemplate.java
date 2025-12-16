package com.github.kusoroadeolu.client;

import java.util.Objects;

class RetryTemplate {
    private final int delay;
    private final int backoff;
    private final Class<?>[] retryFor;
    private final int maxNumberOfTries;
    private final int maxDelay;

    public RetryTemplate(RetryTemplateBuilder retryTemplateBuilder) {
        this.delay = retryTemplateBuilder.delay;
        this.backoff = retryTemplateBuilder.backoff;
        this.retryFor = retryTemplateBuilder.retryFor;
        this.maxNumberOfTries = retryTemplateBuilder.maxNumberOfTries;
        this.maxDelay = retryTemplateBuilder.maxDelay;
    }

    public static RetryTemplateBuilder builder(){
        return new RetryTemplateBuilder();
    }

    public int getDelay() {
        return delay;
    }

    public int getBackoff() {
        return backoff;
    }

    public Class<?>[] getRetryFor() {
        return retryFor;
    }

    public int getMaxNumberOfTries() {
        return maxNumberOfTries;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public static class RetryTemplateBuilder{
        private int delay;
        private int backoff;
        private Class<?>[] retryFor;
        private int maxNumberOfTries;
        private int maxDelay;

        public RetryTemplateBuilder(){
            this.delay = 10;
            this.backoff = 3;
            this.retryFor = new Class[0];
            this.maxNumberOfTries = 3;
            this.maxDelay = 60;
        }


        public RetryTemplateBuilder delay(int delay) {
            assertPositive(delay);
            this.delay = delay;
            return this;
        }

        public RetryTemplateBuilder backoff(int backoff) {
            assertPositive(backoff);
            this.backoff = backoff;
            return this;
        }

        public RetryTemplateBuilder retryFor(Class<?>... retryFor) {
            Objects.requireNonNull(retryFor);
            this.retryFor = retryFor;
            return this;
        }

        public RetryTemplateBuilder maxNumberOfTries(int numberOfTries) {
            assertPositive(numberOfTries);
            this.maxNumberOfTries = numberOfTries;
            return this;
        }

        public RetryTemplateBuilder maxDelay(int delay) {
            assertPositive(delay);
            this.maxDelay = delay;
            return this;
        }

        public RetryTemplate build(){
            return new RetryTemplate(this);
        }

        private void assertPositive(int n){
            if (n < 0) throw new IllegalArgumentException();
        }
    }

}
