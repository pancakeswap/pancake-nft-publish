package com.pancakeswap.nft.publish.model.dto;

import lombok.Data;

@Data
public abstract class AbstractTokenDto {
    private String tokenId;
    private String name;
    private String description;
    private String image;
    private String imagePng;
    private String gif;

    private Boolean isMp4;
    private Boolean isWebm;
    private Boolean isGif;

}
