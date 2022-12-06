package com.pancakeswap.nft.publish.util;

import com.pancakeswap.nft.publish.config.FutureConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FutureUtils {

    public static void waitFutureRequestFinished(FutureConfig config) {
        boolean allAttemptsFinished = false;
        while (!allAttemptsFinished) {
            try {
                CompletableFuture<?> item = config.getFutureRequests().pop();
                item.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.trace("Failed blocked waiting", e);
            }

            //Dirty fix for race condition on last item
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }

            config.getFutureRequests().removeIf(CompletableFuture::isDone);
            allAttemptsFinished = config.getFutureRequests().size() == 0;
        }
    }

}
