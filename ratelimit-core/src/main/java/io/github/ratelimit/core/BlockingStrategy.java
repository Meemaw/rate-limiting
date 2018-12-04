package io.github.ratelimit.core;

import java.util.concurrent.TimeUnit;

public interface BlockingStrategy {

    void block(long nanosToPark) throws InterruptedException;


    BlockingStrategy SLEEPING = nanosToPark -> Thread.sleep(TimeUnit.NANOSECONDS.toMillis(nanosToPark));


}
