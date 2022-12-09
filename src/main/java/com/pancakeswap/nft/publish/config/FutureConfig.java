package com.pancakeswap.nft.publish.config;

import lombok.Getter;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

@Getter
public class FutureConfig {
    private final ExecutorService executor;
    private final Deque<CompletableFuture<?>> futureRequests;
    private final Set<String> tokenIdsFailed;

    private FutureConfig(Deque<CompletableFuture<?>> futureRequests, Set<String> tokenIdsFailed) {
        executor = Executors.newFixedThreadPool(15);

        this.futureRequests = futureRequests;
        this.tokenIdsFailed = tokenIdsFailed;
    }

    public void removeIfDone() {
        this.futureRequests.removeIf(CompletableFuture::isDone);
    }

    public void addFuture(Runnable runnable) {
        this.futureRequests.offerLast(CompletableFuture.runAsync(runnable, executor));
    }

    public void addFailedTokenId(String id) {
        this.tokenIdsFailed.add(id);
    }

    public static FutureConfig init() {
        return new FutureConfig(new ConcurrentLinkedDeque<>(), Collections.synchronizedSet(new HashSet<>()));
    }
}
