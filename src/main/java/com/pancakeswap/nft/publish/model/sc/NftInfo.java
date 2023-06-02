package com.pancakeswap.nft.publish.model.sc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NftInfo {

    private BigInteger prototype;
    private BigInteger quality;
    private BigInteger lv;

}
