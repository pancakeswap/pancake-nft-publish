package com.pancakeswap.nft.publish.model.sc;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;

@Data
@RequiredArgsConstructor
public class NftInfo {

    private final BigInteger prototype;
    private final BigInteger quality;
    private final BigInteger lv;

}
