package com.pancakeswap.nft.publish.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.util.IOUtils;
import com.pancakeswap.nft.publish.exception.ImageLoadException;
import com.pancakeswap.nft.publish.model.dto.TokenDataDto;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.pancakeswap.nft.publish.util.FileNameUtil.formattedTokenName;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Service
@Slf4j
public class ImageService {

    private final String contract;
    @Value("${aws.access.key}")
    private String accessKey;
    @Value("${aws.secret.key}")
    private String secretKey;
    @Value("${aws.bucket}")
    private String bucket;

    private AmazonS3 s3client;

    @Autowired
    public ImageService(@Value("${nft.collection.address}") String contract) {
        this.contract = Keys.toChecksumAddress(contract);
    }

    @PostConstruct
    public void postConstruct() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.AP_NORTHEAST_1)
                .build();
    }

    public CompletableFuture<?> uploadAvatarImage(String imageUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                BufferedImage original = ImageIO.read(new URL(imageUrl));

                uploadSync(original, "avatar.png", TokenMetadata.PNG);
            } catch (IOException ignore) {
                log.error("failed to upload image avatar. url: {}", imageUrl);
            }
        });
    }

    public CompletableFuture<?> uploadBannerImage(String imageUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                BufferedImage original = ImageIO.read(new URL(imageUrl));

                uploadSync(original, "banner-lg.png", TokenMetadata.PNG);
                uploadSync(original, "banner-sm.png", TokenMetadata.PNG);
            } catch (IOException ignore) {
                log.error("failed to upload image banner. url: {}", imageUrl);
            }
        });
    }

    public CompletableFuture<?> s3UploadTokenImagesAsync(String imageUrl, TokenDataDto tokenData, Set<String> tokenIdsFailed, TokenMetadata metadata) {
        return CompletableFuture.runAsync(() -> {
            String tokenName = formattedTokenName(tokenData.getName());
            if (!imageExist(tokenName, metadata)) {
                int attempts = 10;
                int i = 0;
                boolean done = false;
                while (i < attempts && !done) {
                    try {
                        uploadTokenImages(imageUrl, tokenName, metadata);
                        done = true;
                    } catch (IOException ignore) {
                    }
                    i++;
                }
                if (attempts == i) {
                    tokenIdsFailed.add(tokenData.getTokenId());
                    log.error("failed to upload {}. url: {}, formattedTokenName: {}", metadata, imageUrl, tokenName);
                }
            }
        });
    }

    private void uploadTokenImages(String imageUrl, String tokenName, TokenMetadata metadata) throws IOException {
        switch (metadata) {
            case PNG:
                AbstractMap.SimpleEntry<BufferedImage, BufferedImage> images = getImages(imageUrl);
                uploadSync(images.getKey(), String.format("%s.png", tokenName), metadata);
                uploadSync(images.getValue(), String.format("%s-1000.png", tokenName), metadata);
                break;
            case GIF:
                uploadGifSync(getImage(imageUrl), String.format("%s.gif", tokenName), metadata);
                break;
        }

    }

    private AbstractMap.SimpleEntry<BufferedImage, BufferedImage> getImages(String imageUrl) {
        imageUrl = getIpfsFormattedUrl(imageUrl);
        BufferedImage original = null;
        BufferedImage resized = null;
        for (int i = 0; i < 10; i++) {
            try {
                original = ImageIO.read(new URL(imageUrl));
                resized = Scalr.resize(
                        ImageIO.read(new URL(imageUrl)),
                        Scalr.Method.AUTOMATIC,
                        Scalr.Mode.AUTOMATIC,
                        1000,
                        1000,
                        Scalr.OP_ANTIALIAS);
                break;
            } catch (Exception ignore) {
            }
        }
        if (original == null || resized == null) {
            throw new ImageLoadException("Failed to load image. Url: " + imageUrl);
        }

        return new AbstractMap.SimpleEntry<>(original, resized);
    }

    private URL getImage(String imageUrl) {
        imageUrl = getIpfsFormattedUrl(imageUrl);
        URL original = null;
        for (int i = 0; i < 10; i++) {
            try {
                original = new URL(imageUrl);
                break;
            } catch (Exception ignore) {
            }
        }
        if (original == null ) {
            throw new ImageLoadException("Failed to load image. Url: " + imageUrl);
        }

        return original;
    }

    private void uploadSync(BufferedImage image, String filename, TokenMetadata metadata) throws IOException {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        ImageIO.write(image, metadata.getType(), outstream);
        byte[] buffer = outstream.toByteArray();
        InputStream is = new ByteArrayInputStream(buffer);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setCacheControl("public, max-age= 2592000");
        meta.setContentType(metadata.getContentType());
        meta.setContentLength(buffer.length);

        PutObjectRequest putObject = new PutObjectRequest(bucket, String.format("%s/%s/%s", "mainnet", contract, filename), is, meta)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        s3client.putObject(putObject);
    }

    private void uploadGifSync(URL url, String filename, TokenMetadata metadata) throws IOException {
        try (InputStream in = url.openStream()) {

            byte[] data = IOUtils.toByteArray(in);
            InputStream is = new ByteArrayInputStream(data);

            ObjectMetadata meta = new ObjectMetadata();
            meta.setCacheControl("public, max-age= 2592000");
            meta.setContentType(metadata.getContentType());
            meta.setContentLength(data.length);

            PutObjectRequest putObject = new PutObjectRequest(bucket, String.format("%s/%s/%s", "mainnet", contract, filename), is, meta)
                    .withCannedAcl(CannedAccessControlList.PublicRead);

            s3client.putObject(putObject);
        }
    }

    private boolean imageExist(String formattedTokenName, TokenMetadata metadata) {
        String image = String.format("%s/%s/%s/.%s", "mainnet", contract, formattedTokenName, metadata.getType());
        boolean exist = s3client.doesObjectExist(bucket, image);
        if (metadata == TokenMetadata.PNG) {
            String resizedImage = String.format("%s/%s/%s-1000.%s", "mainnet", contract, formattedTokenName, metadata.getType());
            return exist && s3client.doesObjectExist(bucket, resizedImage);
        }

        return exist;
    }

}
