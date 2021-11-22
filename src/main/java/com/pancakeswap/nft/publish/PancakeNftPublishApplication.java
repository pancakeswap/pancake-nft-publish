package com.pancakeswap.nft.publish;

import com.pancakeswap.nft.publish.model.entity.Collection;
import com.pancakeswap.nft.publish.service.DBService;
import com.pancakeswap.nft.publish.service.NFTService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableMongoRepositories
public class PancakeNftPublishApplication {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(PancakeNftPublishApplication.class, args);
        if (args[0] == null) {
            throw new IllegalArgumentException("Run mode should be provided as first argument. " +
                    Arrays.stream(RunMode.values()).map(Enum::name)
                            .collect(Collectors.joining(",")));
        }

        NFTService nftService = ctx.getBean(NFTService.class);
        switch (RunMode.getByName(args[0])) {
            case LIST:
                nftService.listNFT();
                break;
            case RELIST_TOKENS:
                List<BigInteger> idsToRelist = Arrays.stream(args[1].split(","))
                        .map(BigInteger::new).collect(Collectors.toList());

                nftService.relistNft(idsToRelist);
                break;
            case RELIST_COLLECTION:
                DBService dbService = ctx.getBean(DBService.class);
                Collection collection = dbService.getCollection();
                dbService.deleteCollection(collection.getId());

                nftService.listNFT();
                break;
        }

        int exitCode = SpringApplication.exit(ctx, () -> 0);
        System.exit(exitCode);
    }

    private enum RunMode {
        LIST, RELIST_TOKENS, RELIST_COLLECTION;

        public static RunMode getByName(String name) {
            return Arrays.stream(RunMode.values()).filter(e -> e.name().toLowerCase(Locale.ROOT)
                    .equalsIgnoreCase(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Illegal Run Mode"));
        }
    }

}
