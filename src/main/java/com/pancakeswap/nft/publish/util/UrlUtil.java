package com.pancakeswap.nft.publish.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class UrlUtil {

    public static final String pancakeIpfsHost = "pancake.mypinata.cloud";
    public static final String pancakeIpfsNode = String.format("%s%s%s", "https://", pancakeIpfsHost,"/ipfs/");

    public static String getIpfsFormattedUrl(String ipfsUrl) {
        if (ipfsUrl.startsWith("ipfs://")) {
            return pancakeIpfsNode + ipfsUrl.replace("ipfs://", "").trim();
        } else {
            try {
                URI uri = new URI(ipfsUrl);
                if (uri.getPath().startsWith("/ipfs")) {
                    return new URI(uri.getScheme().toLowerCase(Locale.US), pancakeIpfsHost,
                            uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
                }
            } catch (URISyntaxException ignore) {
            }
            return ipfsUrl.trim();
        }
    }
}
