package com.abworks.structures.ratelimiters.strategy;

import com.abworks.structures.ratelimiters.IRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketStrategyTests {

	private IRateLimiter rateLimiter;

	@Test
	public void isRequestAllowed_FirstRequestTest() {
	    IRateLimiter rateLimiter = TokenBucketStrategy.builder().build();
	    String clientId = "testClient";
	    assertTrue(rateLimiter.isRequestAllowed(clientId));
	}

	@Test
	public void isRequestAllowed_MultipleRequestsWithinWindowTest() {
		rateLimiter = TokenBucketStrategy.builder().build();
		String clientId = "testClient";
	    assertTrue(rateLimiter.isRequestAllowed(clientId));
	    assertTrue(rateLimiter.isRequestAllowed(clientId));
	    assertTrue(rateLimiter.isRequestAllowed(clientId));
	    assertTrue(rateLimiter.isRequestAllowed(clientId));
	    assertTrue(rateLimiter.isRequestAllowed(clientId));
	}

	@Test
	public void testRequestFailsAfterMaxRequestsFromSingleUser(){
		String cid = "c123";
		int max = 10;
		rateLimiter = TokenBucketStrategy.builder().maxTokens(max).build();

		// Make exactly max requests - all should succeed
		for (int i = 0; i < max; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid),
					"Request " + (i + 1) + " should be allowed");
		}

		// The (max + 1)th request should fail
		assertFalse(rateLimiter.isRequestAllowed(cid),
				"Request " + (max + 1) + " should be rejected");
	}

	@Test
	public void testRequestSucceedsIfTwoUserCombinedMakeMoreThanMaxCalls(){
		String cid1 = "c123";
		String cid2 = "c456";

		int max = 10;
		rateLimiter = TokenBucketStrategy.builder().maxTokens(max).build();
		for (int i = 0; i < max - 2; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}

		for (int i = 0; i < max - 2; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid2),
					"Request " + (i + 1) + " should be allowed");
		}
	}


	@Test
	public void testRequestsSucceedIfMaxCallsAfterFullTokenFill() throws InterruptedException {
		String cid1 = "c123";
		int max = 10;
		long fillRate = 100; // in ms
		long timeToWait = max * fillRate;
		rateLimiter = TokenBucketStrategy.builder().maxTokens(max).fillRateInMillis(fillRate).build();
		for (int i = 0; i < max; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}
		assertFalse(rateLimiter.isRequestAllowed(cid1),
				"Request " + (max + 1) + " should be rejected");
		Thread.sleep(timeToWait);
		for (int i = 0; i < max; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}

	}

	@Test
	public void someTestRequestsSucceedAsTokensAreAdded() throws InterruptedException {
		String cid1 = "c123";
		int newTokensExpected = 2;
		int max = 10;
		long fillRate = 100; // in ms
		long timeToWait = newTokensExpected * fillRate;
		rateLimiter = TokenBucketStrategy.builder().maxTokens(max).fillRateInMillis(fillRate).build();
		for (int i = 0; i < max; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}
		assertFalse(rateLimiter.isRequestAllowed(cid1),
				"Request " + (max + 1) + " should be rejected");
		Thread.sleep(timeToWait);
		for (int i = 0; i < newTokensExpected; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}
	}

	@Test
	public void ensureOnlyMaxTokensAddedEvenAfterLongTimeGap() throws InterruptedException {
		String cid1 = "c123";
		int max = 10;
		long fillRate = 100; // in ms
		long timeToWait = 2 * (max) * fillRate;
		rateLimiter = TokenBucketStrategy.builder().maxTokens(max).fillRateInMillis(fillRate).build();
		for (int i = 0; i < max; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}
		assertFalse(rateLimiter.isRequestAllowed(cid1),
				"Request " + (max + 1) + " should be rejected");
		Thread.sleep(timeToWait);
		for (int i = 0; i < max; i++){
			assertTrue(rateLimiter.isRequestAllowed(cid1),
					"Request " + (i + 1) + " should be allowed");
		}
		assertFalse(rateLimiter.isRequestAllowed(cid1),
				"Request " + (max + 1) + " should be rejected");
	}
}