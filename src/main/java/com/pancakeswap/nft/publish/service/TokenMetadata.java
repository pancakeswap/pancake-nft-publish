package com.pancakeswap.nft.publish.service;

import lombok.Getter;

@Getter
public enum TokenMetadata {

    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    MP4("mp4", "video/mp4"),
    WEBM("webm", "video/webm");

    private final String type;
    private final String contentType;

    TokenMetadata(String type, String contentType) {
        this.type = type;
        this.contentType = contentType;
    }
}
