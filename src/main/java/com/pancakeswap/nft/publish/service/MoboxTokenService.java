package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.entity.Attribute;
import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.model.entity.Token;
import com.pancakeswap.nft.publish.model.sc.NftInfo;
import com.pancakeswap.nft.publish.repository.AttributeRepository;
import com.pancakeswap.nft.publish.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoboxTokenService {

    private final DBService dbService;
    private final BlockChainService blockChainService;
    private final AttributeRepository attributeRepository;
    private final TokenRepository tokenRepository;
    private final static String LEVEL_ATTRIBUTE = "lv";

    public void updateLevels(Collection collection) {
        String collectionAddress = collection.getAddress();

        // TODO: consider using pagination
        String collectionId = collection.getId();
        List<Token> tokens = tokenRepository.findAllByParentCollection(new ObjectId(collectionId));

        tokens.forEach(token -> {
            List<String> attributesId = token.getAttributes().stream()
                    .map(ObjectId::toString)
                    .collect(Collectors.toList());
            Optional<Attribute> levelAttributeFromDB = attributeRepository.findFirstByIdInAndTraitType(attributesId, LEVEL_ATTRIBUTE);
            if (levelAttributeFromDB.isPresent()) {
                try {
                    NftInfo nftInfo = blockChainService.getNftInfo(collectionAddress, new BigInteger(token.getTokenId()));
                    Attribute tokenLevelAttribute = levelAttributeFromDB.get();
                    String tokenLvlFromDB = tokenLevelAttribute.getValue();
                    log.info(String.format("TMP: tokenLvlFromChain: %s, tokenLvlFromDB: %s", nftInfo.getLv().toString(), tokenLvlFromDB));
                    if (!tokenLvlFromDB.equals(nftInfo.getLv().toString())) {
                        dbService.storeTokenAttribute(tokenLevelAttribute, nftInfo.getLv().toString(), LEVEL_ATTRIBUTE);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Missing attribute: '%s' for token: %s", LEVEL_ATTRIBUTE, token));
            }
        });
    }
}
