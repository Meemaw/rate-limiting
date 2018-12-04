# Rate Limiting

### About

This project implements "state-of-the-art" algorithms for rate limiting:
  - Token bucket algorithm
  - Fixed window algorithm
  - Sliding window log algorithm

It is highly customizable and extensible and has no dependencies and assumptions about the environment used - it can be easily extended to be used with any key-value storage backend such as:
  - Hazelcast
  - Redis
  - ...

See [Hazelcast][hazelcast-storage] or [JCache][jcache-storage]  backend implementation example


### Usage

##### Rate Limiting

To perform Rate Limiting implement `RateLimiter` interface or use existing `RateLimiterImpl`. You can implement use your key-value database by implementing `StorageBackend` interface or use the existing [HazelcastStorage][hazelcast-storage] implementation. 


```java
StorageBackend<String> storageBackend = new InMemoryStorageBackend<>(); // in memory impl.
EntryStorage entryStorage = new DistributedEntryStorage(storageBackend); // async mode
RateLimiter rateLimiter = RateLimiting.withStorage(entryStorage);

if (rateLimiter.conformsRateLimits("userIdentifier")) {
    System.out.println("Ok!");
} else {
    System.out.println("Too many requests!");
}
```

If you need custom serialization combined with your custom storage-backend extend base classes e.g. `SimpleRefillPolicy`, `AbstractRecord` and `AbstractEntry` and implement required serialization methods.


##### Scheduling

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


[hazelcast-storage]: todo
[jcache-storage]: todo