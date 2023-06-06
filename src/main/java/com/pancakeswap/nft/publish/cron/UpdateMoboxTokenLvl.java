package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.exception.ListingException;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.service.DBService;
import com.pancakeswap.nft.publish.service.MoboxTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateMoboxTokenLvl {
    private static final String MOBOX_COLLECTION_ADDRESS = "0x9F0A9654F84141B02a759Bea02B7Df49AB0CE0a0";

    private final DBService dbService;
    private final MoboxTokenService moboxTokenService;

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 0)
    public void updateLvl() {
        Collection moboxCollection = dbService.getCollection(MOBOX_COLLECTION_ADDRESS);
        if (moboxCollection != null) {
            try {
                log.info("Mobox tokens lvl update started");
                moboxTokenService.updateLevels(moboxCollection);
                log.info("Mobox tokens lvl update ended");
            } catch (Exception ex) {
                log.error(ex.getMessage());
                throw new ListingException("Failed to update Mobox tokens lvl");
            }
        }
    }
}

