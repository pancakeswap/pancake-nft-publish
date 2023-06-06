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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final static int PAGE_SIZE = 1000;

    public void updateLevels(Collection collection) {
        String collectionAddress = collection.getAddress();
        String collectionId = collection.getId();
        ObjectId collectionObj = new ObjectId(collectionId);

        PageRequest pageRequest = PageRequest.ofSize(PAGE_SIZE);
        Page<Token> onePage = tokenRepository.findAllByParentCollection(collectionObj, pageRequest);
        while (!onePage.isLast()) {
            pageRequest = pageRequest.next();
            processPage(collectionAddress, onePage);
            onePage = tokenRepository.findAllByParentCollection(collectionObj, pageRequest);
        }
    }

    private void processPage(String collectionAddress, Page<Token> onePage) {
        onePage.stream().forEach(token -> {
            List<String> attributesId = getAttributesId(token);
            Optional<Attribute> levelAttributeFromDB = attributeRepository.findFirstByIdInAndTraitType(attributesId, LEVEL_ATTRIBUTE);
            if (levelAttributeFromDB.isPresent()) {
                try {
                    NftInfo nftInfo = blockChainService.getNftInfo(collectionAddress, new BigInteger(token.getTokenId()));
                    String tokenLvlFromDB = levelAttributeFromDB.get().getValue();
                    String tokenLvlFromChain = nftInfo.getLv().toString();
                    if (!tokenLvlFromDB.equals(tokenLvlFromChain)) {
                        dbService.storeTokenAttribute(levelAttributeFromDB.get(), tokenLvlFromChain, LEVEL_ATTRIBUTE);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Missing attribute: '%s' for token: %s", LEVEL_ATTRIBUTE, token));
            }
        });
    }

    private static List<String> getAttributesId(Token token) {
        return token.getAttributes().stream()
                .map(ObjectId::toString)
                .collect(Collectors.toList());
    }
}
