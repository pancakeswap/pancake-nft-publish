package com.pancakeswap.nft.publish.model.dto;

import lombok.Data;

@Data
public abstract class AbstractTokenDto {
    private String tokenId;
    private String name;
    private String description;
    private String image;
    private String imagePng;

    private Boolean mp4;
    private Boolean webm;
    private Boolean gif;

}
