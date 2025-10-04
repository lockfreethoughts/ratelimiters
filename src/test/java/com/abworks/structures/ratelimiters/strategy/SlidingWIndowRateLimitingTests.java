package com.abworks.structures.ratelimiters.strategy;

import com.abworks.structures.ratelimiters.IRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWIndowRateLimitingTests {

    IRateLimiter rateLimiter;
	@Test
    public void testRequestsAreAcceptedForAClientWithDefaultParams(){
        String cid = "c123";
        rateLimiter = SlidingWindowStrategy.builder().build();
        assert rateLimiter.isRequestAllowed(cid);
        assert rateLimiter.isRequestAllowed(cid);
    }



    @Test
    public void testRequestFailsAfterMaxRequestsFromSingleUser(){
        String cid = "c123";
        int max = 10;
        rateLimiter = SlidingWindowStrategy.builder().maxRequestsPerUser(max).build();

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
        rateLimiter = SlidingWindowStrategy.builder().maxRequestsPerUser(max).build();
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
    public void testRequestsSucceedIfMaxCallsMadeAfterWindowExpires() throws InterruptedException {
        String cid1 = "c123";
        int max = 10;
        long windowSize = 2000; // in ms
        rateLimiter = SlidingWindowStrategy.builder().maxRequestsPerUser(max).build();
        for (int i = 0; i < max; i++){
            assertTrue(rateLimiter.isRequestAllowed(cid1),
                    "Request " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimiter.isRequestAllowed(cid1),
                "Request " + (max + 1) + " should be rejected");
        Thread.sleep(windowSize);
        for (int i = 0; i < max; i++){
            assertTrue(rateLimiter.isRequestAllowed(cid1),
                    "Request " + (i + 1) + " should be allowed");
        }

    }

    @Test
    public void someTestRequestsSucceedAfterPartialWindowExpiry() throws InterruptedException {
        String cid1 = "c123";
        int max = 10;
        long windowSize = 2000; // in ms
        rateLimiter = SlidingWindowStrategy.builder().maxRequestsPerUser(max).windowSize(windowSize).build();
        for (int i = 0; i < 2; i++){
            assertTrue(rateLimiter.isRequestAllowed(cid1),
                    "Request " + (i + 1) + " should be allowed");
        }
        Thread.sleep(windowSize/ 2 + 100); // half window + some delay
        for (int i = 2; i < max; i++){
            assertTrue(rateLimiter.isRequestAllowed(cid1),
                    "Request " + (i + 1) + " should be allowed");
        }
        Thread.sleep(windowSize/ 2);
        assertTrue(rateLimiter.isRequestAllowed(cid1),
                "Request should be allowed");
        assertTrue(rateLimiter.isRequestAllowed(cid1),
                "Request should be allowed");
        assertFalse(rateLimiter.isRequestAllowed(cid1),
                "Request should be rejected");

    }


    @Test
    public void testMultipleThreadsMakingRequestsWithinLimitsShouldSucceed() throws InterruptedException {
        String cid1 = "c123";
        int max = 10;
        long windowSize = 2000; // in ms
        rateLimiter = SlidingWindowStrategy.builder().maxRequestsPerUser(max).windowSize(windowSize).build();


        Thread t1= new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i< max/3; i++)
                    assertTrue(rateLimiter.isRequestAllowed(cid1),
                            "Request " + (i + 1) + " should be allowed");
            }
        });
        Thread t2=new Thread(() -> {
            for (int i = 0; i< max/3; i++)
                assertTrue(rateLimiter.isRequestAllowed(cid1),
                        "Request " + (i + 1) + " should be allowed");
        });
        t1.start();
        t2.start();

        t1.join();
        t2.join();

    }

    @Test
    public void testMultipleThreadsMakingRequestsOutsideLimitsShouldFail() throws InterruptedException {
        String cid1 = "c123";
        int max = 10;
        long windowSize = 2000; // in ms
        rateLimiter = SlidingWindowStrategy.builder().maxRequestsPerUser(max).windowSize(windowSize).build();

        int totalThreads = 5;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                startLatch.await(); // ensure all threads start together
                for (int i = 0; i < requestsPerThread; i++) {
                    if (rateLimiter.isRequestAllowed(cid1)) {
                        allowed.incrementAndGet();
                    } else {
                        denied.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };
        for (int i = 0; i < totalThreads; i++) {
            executor.submit(task);
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        assertEquals(max, allowed.get(), "Should only allow up to max requests");
        assertEquals(totalThreads * requestsPerThread - max, denied.get(),
                "Rest should be denied");    }

}