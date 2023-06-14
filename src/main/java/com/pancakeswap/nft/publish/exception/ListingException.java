package com.pancakeswap.nft.publish.exception;

public class ListingException extends RuntimeException {
    public ListingException(String message) {
        super(message);
    }

    public ListingException(String message, Exception cause) {
        super(message, cause);
    }
}
