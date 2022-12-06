package com.pancakeswap.nft.publish.cron;

import com.pancakeswap.nft.publish.config.FutureConfig;
import com.pancakeswap.nft.publish.model.dto.collection.CollectionDataDto;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.repository.CollectionRepository;
import com.pancakeswap.nft.publish.service.NFTService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class UpdateCollections {

    private final NFTService nftService;
    private final CollectionRepository collectionRepository;

    public UpdateCollections(NFTService nftService, CollectionRepository collectionRepository) {
        this.nftService = nftService;
        this.collectionRepository = collectionRepository;
    }

//    @Scheduled(fixedDelay = 1000 * 60, initialDelay = 0)
//    public void updateCollections() throws ExecutionException, InterruptedException {
//        for (Collection collection :  collectionRepository.findAll()) {
//            FutureConfig config = FutureConfig.init();
////            nftService.listNFT(from(collection));
//            System.out.println("updateCollections cron");
//        }
//    }

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
