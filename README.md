# Rate Limiting

### [About][about]

This project implements "state-of-the-art" algorithms for rate limiting:
  - [Token bucket algorithm][token-bucket]
  - Fixed window algorithm
  - Sliding window log algorithm

It is highly customizable and extensible and has no dependencies and assumptions about the environment used - it can be easily extended to be used with any key-value storage backend such as:
  - [Hazelcast][hazelcast]
  - [Redis][redis]
  - ...

See [Hazelcast][hazelcast-storage] or [JCache][jcache-storage] example storage implementation


### [Features][features]

- Multiple policies per user
- Blazing speed
- Multiple algorithms per user
- Support for distributed environments
- Pluggable storage backend system

### [Usage][usage]

To perform Rate Limiting implement `RateLimiter` interface or use existing `RateLimiterImpl`. You can implement use your key-value database by implementing `StorageBackend` interface or use the existing [HazelcastStorage][hazelcast-storage] implementation. 

### [Examples][examples]

##### [Simple example][simple-example]

[Source][simple-example-source]

```java
StorageBackend<String> storageBackend = new InMemoryStorageBackend<>(); // in memory impl.
EntryStorage entryStorage = new DistributedEntryStorage(storageBackend); // async mode
RateLimiter rateLimiter = RateLimiting.withStorage(entryStorage);

if (rateLimiter.conformsRateLimits("userIdentifier")) {
    System.out.println("User has no policies so this will be printed!");
} else {
    System.out.println("Too many requests!");
}
```

##### Advanced rate limit filter example (javax)

[Full source][rate-limit-filter-source]

```java
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        try {
            Optional<String> identifier = getIdentifier(req);
            if (!identifier.isPresent()) {
                return;
            }

            ConsumptionEntry consumptionEntry = getRateLimiter().conformRateLimitsWithConsumption(identifier.get());
            long retryAfter = TimeUnit.NANOSECONDS.toMillis(consumptionEntry.getNanosUntilConsumption());

            // Inject headers
            response.addHeader(RATE_LIMIT_REMAINING_HEADER,
                    String.valueOf(consumptionEntry.getRemainingTokens()));
            response.addHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfter));

            if (!consumptionEntry.doesConform()) {
                req.abortWith(createRateLimitResponse(consumptionEntry));
            }
        } catch (RateLimiterException ex) {
        }
    }

}
```


If you need custom serialization combined with your custom storage-backend extend base classes e.g. `SimpleRefillPolicy`, `AbstractRecord` and `AbstractEntry` and implement required serialization methods.

##### [Configuration][configuration]

###### Env variables
- `ratelimit.map.users.limits`: Hazelcast IMap name (default `ratelimit.map.users.limits)`
- `distributedStorageBackendTimeout`: Timeout for rate limiter pass-through mode in ms (default `500ms`). You should decrease this in production to avoid long latencies in case of StorageBackend failures.

##### [Scheduling][scheduling]

[Source][scheduling-example-source]

```java
EntryBuilder builder = RateLimiting.schedulerBuilder().withAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
RefillPolicy policy = SimpleRefillPolicy.perSecond(2);
RateLimitEntry record = builder.withRefillPolicy(policy).build();

long start = System.currentTimeMillis();
while (record.tryConsume(1)) {
	double secondsPassed = (System.currentTimeMillis() - start) / 1000.0;
	System.out.println(secondsPassed); // or someVeryExpensiveTask();
}
```


```sh
Output:
0.502
1.004
1.504
2.006
2.508
3.011
...
```


[hazelcast-storage]: https://github.com/Meemaw/rate-limiting/blob/master/ratelimit-hazelcast/src/main/java/io/github/meemaw/ratelimit/hazelcast/HazelcastStorage.java
[jcache-storage]: https://github.com/Meemaw/rate-limiting/blob/master/ratelimit-jcache/src/main/java/io/github/meemaw/ratelimit/jcache/JCacheStorage.java
[about]: https://github.com/Meemaw/rate-limiting#about
[features]: https://github.com/Meemaw/rate-limiting#features
[usage]: https://github.com/Meemaw/rate-limiting#usage
[configuration]: https://github.com/Meemaw/rate-limiting#configuration
[scheduling]: https://github.com/Meemaw/rate-limiting#scheduling
[hazelcast]: https://hazelcast.com/
[redis]: https://redis.io/
[token-bucket]: https://en.wikipedia.org/wiki/Token_bucket
[simple-example]: https://github.com/Meemaw/rate-limiting#simple-example
[simple-example-source]: https://github.com/Meemaw/rate-limiting/blob/master/ratelimit-examples/src/main/java/io/github/meemaw/ratelimit/examples/SimpleRateLimitingExample.java
[scheduling-example-source]: https://github.com/Meemaw/rate-limiting/blob/master/ratelimit-examples/src/main/java/io/github/meemaw/ratelimit/examples/SchedulingExample.java
[examples]: https://github.com/Meemaw/rate-limiting/tree/master/ratelimit-examples/src/main/java/io/github/meemaw/ratelimit/examples
[rate-limit-filter-source]: https://github.com/Meemaw/rate-limiting/blob/master/ratelimit-examples/src/main/java/io/github/meemaw/ratelimit/examples/RateLimitFilter.java