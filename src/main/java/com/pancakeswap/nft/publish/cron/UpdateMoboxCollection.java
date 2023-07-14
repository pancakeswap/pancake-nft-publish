package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.collection.ListCollectionTokenParams;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import com.pancakeswap.nft.publish.repository.CollectionInfoRepository;
import com.pancakeswap.nft.publish.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateMoboxCollection {
    private static final String MOBOX_COLLECTION_ADDRESS = "0x9F0A9654F84141B02a759Bea02B7Df49AB0CE0a0";

    private final DBService dbService;
    private final BlockChainService blockChainService;
    private final CollectionInfoRepository collectionInfoRepository;
    private final MoboxTokenService moboxTokenService;
    private final NFTService nftService;

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
    public void updateNewMintedAndLevel() {
        Collection moboxCollection = dbService.getCollection(MOBOX_COLLECTION_ADDRESS);
        if (moboxCollection != null) {
            try {
                CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(moboxCollection.getId()));
                int onChainLastTokenId = blockChainService.getLastTokenId(MOBOX_COLLECTION_ADDRESS).intValue();
                if (info.getLastTokenId() != null && info.getLastTokenId() < onChainLastTokenId) {
                    int totalSupply = blockChainService.getTotalSupply(MOBOX_COLLECTION_ADDRESS).intValue();

                    FutureConfig config = FutureConfig.init();
                    for (int i = info.getLastTokenId() + 1; i <= onChainLastTokenId; i++) {
                        String url = null;
                        try {
                            url = getIpfsFormattedUrl(blockChainService.getTokenURI(MOBOX_COLLECTION_ADDRESS, BigInteger.valueOf(i)));
                            ListCollectionTokenParams params = new ListCollectionTokenParams(moboxCollection.getId(), MOBOX_COLLECTION_ADDRESS);
                            params.setIsModifiedTokenName(info.getIsModifiedTokenName());
                            params.setOnlyGif(info.getOnlyGif());
                            params.setTokenId(String.valueOf(i));
                            params.setTokenUrl(url);

                            nftService.loadAndStoreTokenDataAsync(config, params, new AtomicInteger(0));
                        } catch (Exception e) {
                            log.error("Failed to store token index/id: {}, url: {}, collectionId: {}. Error message: {}", i, url, moboxCollection.getId(), e.getMessage());
                        }
                    }
                    nftService.postListActions(config, moboxCollection.getId());

                    info.setLastTokenId(onChainLastTokenId);
                    collectionInfoRepository.save(info);

                    moboxCollection.setTotalSupply(totalSupply);
                    dbService.storeCollection(moboxCollection);
                    log.info("Mobox new minted updated. Address : {}", MOBOX_COLLECTION_ADDRESS);
                }

                moboxTokenService.updateLevels(moboxCollection);
                log.info("Mobox tokens lvl updated");
            } catch (Exception ex) {
                log.error("Failed to update Mobox tokens lvl, due to: {}", ex.getMessage());
            }
        }
    }
}

