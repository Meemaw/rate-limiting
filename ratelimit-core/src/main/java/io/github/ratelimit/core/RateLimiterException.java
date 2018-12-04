package io.github.ratelimit.core;

public class RateLimiterException extends Exception {

    private static final long serialVersionUID = -1442370029505943151L;

    public RateLimiterException(String message, Throwable cause) {
        super(message, cause);
    }

}
