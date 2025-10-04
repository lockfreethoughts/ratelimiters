package com.abworks.structures.ratelimiters.strategy;

import com.abworks.structures.ratelimiters.IRateLimiter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The token bucket strategy allows for a user to accumulate tokens, against which they can make input requests.
 * Tokens are used as requests come in.
 * Tokens are added at a configured fill rate.
 * This allows for bursty traffic if there's enough token available.
 * To limit this, we limit the MAX_TOKENS by a configuration
 */
@Builder
public class TokenBucketStrategy implements IRateLimiter {
    @AllArgsConstructor
    private static class UserTokens{
        private long lastRequestTime;
        private int tokensRemaining;
    }

    private static int DEFAULT_MAX_TOKENS = 20;
    private static int DEFAULT_FILL_RATE_IN_MILLIS = 1000; // every 1 second add a token for the user.

    @Builder.Default
    private int maxTokens = DEFAULT_MAX_TOKENS;
    @Builder.Default
    private long fillRateInMillis = DEFAULT_FILL_RATE_IN_MILLIS; // 1 token per fillRate milliseconds

    private final Map<String, UserTokens> clientToTokensRemaining = new ConcurrentHashMap<>();

    @Override
    public boolean isRequestAllowed(String clientID) {
        if (clientID == null || clientID.isEmpty())
            throw new IllegalArgumentException("Client ID can not be null or empty");
        long requestTime = System.currentTimeMillis();
        clientToTokensRemaining.putIfAbsent(clientID, new UserTokens(requestTime, maxTokens));

        UserTokens userInfo = clientToTokensRemaining.get(clientID);

        synchronized (userInfo) {

            int tokensToAdd = getTokensToAdd(requestTime, userInfo.lastRequestTime);
            userInfo.tokensRemaining = Math.min(maxTokens, tokensToAdd + userInfo.tokensRemaining);
            userInfo.lastRequestTime = requestTime;
            if (clientToTokensRemaining.get(clientID).tokensRemaining <= 0)
                return false;
            userInfo.tokensRemaining --;
            return true;
        }
    }

    private int getTokensToAdd(long requestTime, long lastRequestTime) {
        return (int)((requestTime - lastRequestTime) / fillRateInMillis);
    }


}
