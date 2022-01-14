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
        DBService dbService = ctx.getBean(DBService.class);
        Collection collection = dbService.getCollection();

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
                if (args.length > 1) {
                    if ("delete".equalsIgnoreCase(args[1])) {
                        dbService.deleteCollection(collection.getId());
                    } else if ("delete_attr".equalsIgnoreCase(args[1])) {
                        dbService.deleteAttributes(collection.getId());
                    } else {
                        throw new IllegalArgumentException("RELIST_COLLECTION argument not correct: " + args[1]);
                    }
                }
                nftService.listNFT();
                break;
            case DELETE_COLLECTION:
                dbService.deleteCollection(collection.getId());
                break;
        }

        int exitCode = SpringApplication.exit(ctx, () -> 0);
        System.exit(exitCode);
    }

    private enum RunMode {
        LIST, RELIST_TOKENS, RELIST_COLLECTION, DELETE_COLLECTION;

        public static RunMode getByName(String name) {
            return Arrays.stream(RunMode.values()).filter(e -> e.name().toLowerCase(Locale.ROOT)
                    .equalsIgnoreCase(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Illegal Run Mode"));
        }
    }

}
