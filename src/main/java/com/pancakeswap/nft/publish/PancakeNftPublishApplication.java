package com.pancakeswap.nft.publish;

import com.pancakeswap.nft.publish.service.DBService;
import com.pancakeswap.nft.publish.service.NFTService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.ExecutionException;

@SpringBootApplication
@EnableMongoRepositories
public class PancakeNftPublishApplication {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(PancakeNftPublishApplication.class, args);
        NFTService nftService = ctx.getBean(NFTService.class);
        DBService dbService = ctx.getBean(DBService.class);
        dbService.deleteCollection("6193f283c4da985dcb2b00ad");

        nftService.listNFT();

        int exitCode = SpringApplication.exit(ctx, () -> 0);
        System.exit(exitCode);
    }

}
