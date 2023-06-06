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
import com.pancakeswap.nft.publish.model.dto.AbstractTokenDto;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Set;

import static com.pancakeswap.nft.publish.util.FileNameUtil.formattedTokenName;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Slf4j
@Service
@Profile("!local")
public class ImageUploadingService implements ImageService {

    @Value("${aws.access.key}")
    private String accessKey;
    @Value("${aws.secret.key}")
    private String secretKey;
    @Value("${aws.bucket}")
    private String bucket;

    private AmazonS3 s3client;

    @PostConstruct
    public void postConstruct() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.AP_NORTHEAST_1)
                .build();
    }

    @Override
    public void uploadAvatarImage(String collectionAddress, String imageUrl) {
        try {
            BufferedImage original = ImageIO.read(new URL(imageUrl));
            uploadSync(original, "avatar.png", collectionAddress, TokenMetadata.PNG);
        } catch (IOException ignore) {
            log.error("failed to upload image avatar. url: {}", imageUrl);
        }
    }

    @Override
    public void uploadBannerImage(String collectionAddress, String imageUrl) {
        try {
            BufferedImage original = ImageIO.read(new URL(imageUrl));

            uploadSync(original, "banner-lg.png", collectionAddress, TokenMetadata.PNG);
            uploadSync(original, "banner-sm.png", collectionAddress, TokenMetadata.PNG);
        } catch (IOException ignore) {
            log.error("failed to upload image banner. url: {}", imageUrl);
        }
    }

    @Override
    public void s3UploadTokenImagesAsync(String collectionAddress, String imageUrl, AbstractTokenDto tokenData, Set<String> tokenIdsFailed, TokenMetadata metadata) {
        String tokenName = formattedTokenName(tokenData.getName());
        if (!imageExist(collectionAddress, tokenName, metadata)) {
            int attempts = 10;
            int i = 0;
            boolean done = false;
            while (i < attempts && !done) {
                try {
                    uploadTokenImages(collectionAddress, imageUrl, tokenName, metadata);
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
    }

    private void uploadTokenImages(String collectionAddress, String imageUrl, String tokenName, TokenMetadata metadata) throws IOException {
        switch (metadata) {
            case PNG -> {
                AbstractMap.SimpleEntry<BufferedImage, BufferedImage> images = getImages(imageUrl);
                uploadSync(images.getKey(), String.format("%s.png", tokenName), collectionAddress, metadata);
                uploadSync(images.getValue(), String.format("%s-1000.png", tokenName), collectionAddress, metadata);
            }
            case GIF ->
                    uploadAnimatedSync(getImage(imageUrl), String.format("%s.gif", tokenName), collectionAddress, metadata);
            case MP4 ->
                    uploadAnimatedSync(getImage(imageUrl), String.format("%s.mp4", tokenName), collectionAddress, metadata);
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
            } catch (Exception e) {
                log.error(e.getMessage());
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
                original = new URI(imageUrl).toURL();
                break;
            } catch (Exception ignore) {
            }
        }
        if (original == null) {
            throw new ImageLoadException("Failed to load image. Url: " + imageUrl);
        }

        return original;
    }

    private void uploadSync(BufferedImage image, String filename, String contract, TokenMetadata metadata) throws IOException {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        ImageIO.write(image, metadata.getType(), outstream);
        byte[] buffer = outstream.toByteArray();
        storeMetadataToS3(filename, contract, metadata, buffer);
    }

    private void storeMetadataToS3(String filename, String contract, TokenMetadata metadata, byte[] buffer) {
        InputStream is = new ByteArrayInputStream(buffer);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setCacheControl("public, max-age= 2592000");
        meta.setContentType(metadata.getContentType());
        meta.setContentLength(buffer.length);

        PutObjectRequest putObject = new PutObjectRequest(bucket, String.format("%s/%s/%s", "mainnet", Keys.toChecksumAddress(contract), filename), is, meta)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        s3client.putObject(putObject);
    }

    private void uploadAnimatedSync(URL url, String filename, String collectionAddress, TokenMetadata metadata) throws IOException {
        try (InputStream in = url.openStream()) {

            byte[] data = IOUtils.toByteArray(in);
            storeMetadataToS3(filename, collectionAddress, metadata, data);
        }
    }

    private boolean imageExist(String collectionAddress, String formattedTokenName, TokenMetadata metadata) {
        String image = String.format("%s/%s/%s.%s", "mainnet", collectionAddress, formattedTokenName, metadata.getType());
        boolean exist = s3client.doesObjectExist(bucket, image);
        if (metadata == TokenMetadata.PNG) {
            String resizedImage = String.format("%s/%s/%s-1000.%s", "mainnet", Keys.toChecksumAddress(collectionAddress), formattedTokenName, metadata.getType());
            return exist && s3client.doesObjectExist(bucket, resizedImage);
        }
        return exist;
    }

}
