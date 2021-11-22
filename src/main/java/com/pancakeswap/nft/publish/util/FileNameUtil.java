package com.pancakeswap.nft.publish.util;

import java.util.Locale;

public class FileNameUtil {

    public static String formattedTokenName(String name) {
        return name
                .replaceAll("([a-z])([A-Z]+)", "$1-$2")
                .replaceAll("[^a-zA-Z0-9]", " ")
                .trim().replaceAll(" +", " ")
                .replaceAll(" ", "-")
                .toLowerCase(Locale.ROOT);
    }

}
