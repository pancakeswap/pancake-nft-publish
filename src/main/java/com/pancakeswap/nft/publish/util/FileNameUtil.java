package com.pancakeswap.nft.publish.util;

import java.util.Locale;

public class FileNameUtil {

    //CameCase
    private static final String REGEXP_CAMELCASE_1 = "([a-z0-9])([A-Z])";
    private static final String REGEXP_CAMELCASE_2 = "([A-Z])([A-Z][a-z])";

    // Remove all non-word characters.
    private static final String DEFAULT_STRIP_REGEXP = "[^A-Za-z0-9]+";

    public static String formattedTokenName(String name) {
        String res = name
                .replaceAll(REGEXP_CAMELCASE_1, "$1\0$2")
                .replaceAll(REGEXP_CAMELCASE_2, "$1\0$2")
                .replaceAll(DEFAULT_STRIP_REGEXP, "\0");
        if (res.startsWith("\0")) {
            res = res.substring(res.indexOf("\0"));
        }
        if (res.endsWith("\0")) {
            res = res.substring(0, res.lastIndexOf("\0"));
        }
        res = res.replaceAll("\0", "-")
                .toLowerCase(Locale.ROOT);

        return res;
    }

}
