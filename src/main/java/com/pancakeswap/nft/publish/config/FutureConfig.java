package com.pancakeswap.nft.publish.config;

import lombok.Getter;

import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

@Getter
public class FutureConfig {

    private final Deque<CompletableFuture<?>> futureRequests;
    private final Set<String> tokenIdsFailed;

    private FutureConfig(Deque<CompletableFuture<?>> futureRequests, Set<String> tokenIdsFailed) {
        this.futureRequests = futureRequests;
        this.tokenIdsFailed = tokenIdsFailed;
    }

    public void removeIfDone() {
        this.futureRequests.removeIf(CompletableFuture::isDone);
    }

    public void addFuture(CompletableFuture<?> completableFuture) {
        this.futureRequests.offerLast(completableFuture);
    }

    public void addFailedTokenId(String id) {
        this.tokenIdsFailed.add(id);
    }

    public static FutureConfig init() {
        return new FutureConfig(new ConcurrentLinkedDeque<>(), Collections.synchronizedSet(new HashSet<>()));
    }
}
