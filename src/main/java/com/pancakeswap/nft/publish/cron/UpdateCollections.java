package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.CollectionInfo;
import com.pancakeswap.nft.publish.repository.CollectionInfoRepository;
import com.pancakeswap.nft.publish.repository.CollectionRepository;
import com.pancakeswap.nft.publish.service.BlockChainService;
import com.pancakeswap.nft.publish.service.NFTService;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

@Component
public class UpdateCollections {

    private final NFTService nftService;
    private final CollectionRepository collectionRepository;
    private final CollectionInfoRepository collectionInfoRepository;
    private final BlockChainService blockChainService;

    public UpdateCollections(NFTService nftService, CollectionRepository collectionRepository, CollectionInfoRepository collectionInfoRepository, BlockChainService blockChainService) {
        this.nftService = nftService;
        this.collectionRepository = collectionRepository;
        this.collectionInfoRepository = collectionInfoRepository;
        this.blockChainService = blockChainService;
    }

    @Scheduled(fixedDelay = 1000 * 60, initialDelay = 0)
    public void updateCollections() throws ExecutionException, InterruptedException {
        for (Collection collection :  collectionRepository.findAll()) {
            CollectionInfo info = collectionInfoRepository.findByCollectionId(new ObjectId(collection.getId()));
            FutureConfig config = FutureConfig.init();
            BigInteger totalSupply = blockChainService.getTotalSupply(collection.getAddress());

            nftService.listNFT(from(collection));
            System.out.println("updateCollections cron");
        }
    }

    private CollectionDataDto from(Collection collection) {
        CollectionDataDto dataDto = new CollectionDataDto();
        dataDto.setAddress(collection.getAddress());
        dataDto.setName(collection.getName());
        dataDto.setDescription(collection.getDescription());
        dataDto.setSymbol(collection.getSymbol());
        dataDto.setOwner(collection.getOwner());

        return dataDto;
    }
}
