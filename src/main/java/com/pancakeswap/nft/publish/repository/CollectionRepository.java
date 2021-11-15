package com.pancakeswap.nft.publish.repository;

import com.pancakeswap.nft.publish.model.entity.Collection;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CollectionRepository extends MongoRepository<Collection, String> {
    Collection findByAddress(String address);
}
