package com.pancakeswap.nft.publish.model.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CollectionListingFailedResponse {
    COLLECTION_ALREADY_EXIST("Collection already exist"),
    FAILED_TO_LIST_COLLECTION("Failed to list collection"),
    COLLECTION_LISTING_HAS_BEEN_INITIATED("Collection listing has been initiated"),
    COLLECTION_PROCESSING_ALREADY_IN_PROGRESS("Collection processing already in progress"),
    REACHED_MAX_AMOUNT_OF_CONCURRENT_PROCESSING("Reached max amount of concurrent processing");

    private final String message;
}
