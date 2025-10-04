package com.abworks.structures.ratelimiters.strategy;

import com.abworks.structures.Main;
import com.abworks.structures.ratelimiters.IRateLimiter;
import lombok.Builder;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window algorithm
 * Ensures that only a fixed amount of requests go through in a given window of time.
 * Any more than that, would be rejected.
 * A window per clientId needs to be created.
 */
@Builder
@Getter
public class SlidingWindowStrategy implements IRateLimiter {
    private static long DEFAULT_WINDOW_SIZE_IN_MILLIS = 1000;
    private static int MAX_REQUESTS_PER_USER = 5;

    @Builder.Default
    private long windowSize = DEFAULT_WINDOW_SIZE_IN_MILLIS;

    @Builder.Default
    private int maxRequestsPerUser = MAX_REQUESTS_PER_USER;

    private final Map<String, Deque<Long>> userToRequestTimeMap = new ConcurrentHashMap<>();

    @Override
    public boolean isRequestAllowed(String clientID) {
        long currentTime = System.currentTimeMillis();
        userToRequestTimeMap.putIfAbsent(clientID, new ArrayDeque<>());

        synchronized (userToRequestTimeMap.get(clientID)) {
            removeRequestsOutsideCurrentWindow(clientID, currentTime);
            userToRequestTimeMap.get(clientID).add(currentTime);
            return userToRequestTimeMap.get(clientID).size() <= maxRequestsPerUser;
        }
    }

    /**
     * Brings the windowHead to currentTime - windowSize.  Any requests prior to that have no bearing on request being allowed.
     * @param clientID identifier
     * @param currentTime the request time, hence best taken in as a parameter.
     */
    private void removeRequestsOutsideCurrentWindow(String clientID,  long currentTime) {
        long windowStart = currentTime - windowSize;
        Deque<Long> clientRequestsList= userToRequestTimeMap.get(clientID);
        while (!clientRequestsList.isEmpty() && windowStart > clientRequestsList.peekFirst()){
            clientRequestsList.removeFirst();
        }
    }

}
