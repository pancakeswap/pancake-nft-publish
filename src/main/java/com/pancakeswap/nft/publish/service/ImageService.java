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
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.pancakeswap.nft.publish.exception.ImageLoadException;
import com.pancakeswap.nft.publish.model.dto.TokenDataDto;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.pancakeswap.nft.publish.util.FileNameUtil.formattedTokenName;
import static com.pancakeswap.nft.publish.util.UrlUtil.getIpfsFormattedUrl;

@Service
@Slf4j
public class ImageService {

    @Value("${aws.access.key}")
    private String accessKey;
    @Value("${aws.secret.key}")
    private String secretKey;
    @Value("${aws.bucket}")
    private String bucket;

    private AmazonS3 s3client;
    private TransferManager tm;

    @PostConstruct
    public void postConstruct() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.AP_NORTHEAST_1)
                .build();
        tm = TransferManagerBuilder.standard()
                .withS3Client(s3client)
                .build();

    }

    public CompletableFuture<?> uploadAvatarImage(String imageUrl, String contract) {
        return CompletableFuture.runAsync(() -> {
            try {
                BufferedImage original = ImageIO.read(new URL(imageUrl));

                uploadSync(original, contract, "avatar.png");
            } catch (IOException ignore) {
                log.error("failed to upload image avatar. url: {}", imageUrl);
            }
        });
    }

    public CompletableFuture<?> uploadBannerImage(String imageUrl, String contract) {
        return CompletableFuture.runAsync(() -> {
            try {
                BufferedImage original = ImageIO.read(new URL(imageUrl));

                uploadSync(original, contract, "banner-lg.png");
                uploadSync(original, contract, "banner-sm.png");
            } catch (IOException ignore) {
                log.error("failed to upload image banner. url: {}", imageUrl);
            }
        });
    }

    public CompletableFuture<?> s3UploadTokenImagesAsync(String imageUrl, String contract, TokenDataDto tokenData, Set<String> tokenIdsFailed) {
        return CompletableFuture.runAsync(() -> {
            String tokenName = formattedTokenName(tokenData.getName());
            if (!imageExist(contract, tokenName)) {
                int attempts = 10;
                int i = 0;
                boolean done = false;
                while (i < attempts && !done) {
                    try {
                        AbstractMap.SimpleEntry<BufferedImage, BufferedImage> images = getImages(imageUrl);
                        uploadSync(images.getKey(), contract, String.format("%s.png", tokenName));
                        uploadSync(images.getValue(), contract, String.format("%s-1000.png", tokenName));
                        done = true;
                    } catch (IOException ignore) {
                    }
                    i++;
                }
                if (attempts == i) {
                    tokenIdsFailed.add(tokenData.getTokenId());
                    log.error("failed to upload image. url: {}, formattedTokenName: {}", imageUrl, tokenName);
                }
            }
        });
    }

    public List<Upload> s3AsyncUploadTokenImage(String imageUrl, String contract, String formattedTokenName) throws IOException {
        AbstractMap.SimpleEntry<BufferedImage, BufferedImage> images = getImages(imageUrl);

        return List.of(
                uploadAsync(images.getKey(), contract, String.format("%s.png", formattedTokenName)),
                uploadAsync(images.getValue(), contract, String.format("%s-1000.png", formattedTokenName))
        );
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

    private Upload uploadAsync(BufferedImage image, String contract, String filename) throws IOException {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outstream);
        byte[] buffer = outstream.toByteArray();
        InputStream is = new ByteArrayInputStream(buffer);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setCacheControl("public, max-age= 2592000");
        meta.setContentType("image/png");
        meta.setContentLength(buffer.length);

        PutObjectRequest putObject = new PutObjectRequest(bucket, String.format("%s/%s/%s", "mainnet", contract, filename), is, meta)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        return tm.upload(putObject);
    }

    private void uploadSync(BufferedImage image, String contract, String filename) throws IOException {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outstream);
        byte[] buffer = outstream.toByteArray();
        InputStream is = new ByteArrayInputStream(buffer);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setCacheControl("public, max-age= 2592000");
        meta.setContentType("image/png");
        meta.setContentLength(buffer.length);

        PutObjectRequest putObject = new PutObjectRequest(bucket, String.format("%s/%s/%s", "mainnet", contract, filename), is, meta)
                .withCannedAcl(CannedAccessControlList.PublicRead);

        s3client.putObject(putObject);
    }

    private boolean imageExist(String contract, String formattedTokenName) {
        String originalImage = String.format("%s/%s/%s.png", "mainnet", contract, formattedTokenName);
        String resizedImage = String.format("%s/%s/%s-1000.png", "mainnet", contract, formattedTokenName);

        return s3client.doesObjectExist(bucket, originalImage) && s3client.doesObjectExist(bucket, resizedImage);
    }

}
