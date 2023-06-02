package com.pancakeswap.nft.publish.repository;

import com.pancakeswap.nft.publish.model.entity.Token;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TokenRepository extends MongoRepository<Token, String> {
    void deleteAllByParentCollection(ObjectId parentId);

    Token findByParentCollectionAndTokenId(ObjectId parentId, String tokenId);

    List<Token> findAllByParentCollection(ObjectId parentId);

    boolean existsByParentCollectionAndTokenId(ObjectId parentId, String tokenId);
}
