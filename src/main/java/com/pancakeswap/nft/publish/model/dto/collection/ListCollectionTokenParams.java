package com.pancakeswap.nft.publish.model.dto.collection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class ListCollectionTokenParams {
    private final String collectionId;
    private final String collectionAddress;
    private String tokenId;
    private String tokenUrl;
    private Boolean onlyGif;
    private Boolean isModifiedTokenName;
}
