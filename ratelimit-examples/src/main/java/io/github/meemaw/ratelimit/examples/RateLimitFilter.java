package io.github.meemaw.ratelimit.examples;

import io.github.ratelimit.core.RateLimiter;
import io.github.ratelimit.core.RateLimiterException;
import io.github.ratelimit.core.impl.ConsumptionEntry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class RateLimitFilter implements ContainerRequestFilter {


    static final String RETRY_AFTER_HEADER = "Retry-After";
    static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    static final String RATE_LIMIT_VIOLATED_POLICY_HEADER = "X-RateLimit-ViolatedPolicy";

    @Context
    protected HttpServletResponse response;

    protected static final Optional<String> NO_IDENTIFIER = Optional.empty();

    // Inject from somethere
    abstract Optional<String> getIdentifier(ContainerRequestContext req);

    // Inject from somethere
    abstract RateLimiter getRateLimiter();

    private Response createRateLimitResponse(ConsumptionEntry entry) {
        return Response.status(429)
                .header(RATE_LIMIT_VIOLATED_POLICY_HEADER, entry.getViolatedPolicy().toString())
                .build();
    }


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
