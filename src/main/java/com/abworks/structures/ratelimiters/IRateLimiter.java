package com.abworks.structures.ratelimiters;

public interface IRateLimiter {
    boolean isRequestAllowed(String clientID);
    default boolean isRequestAllowed(){
        return isRequestAllowed("GLOBAL");
    }


}
