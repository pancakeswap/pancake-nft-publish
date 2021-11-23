# PancakeSwap NFT listing

Here is an app for help chefs listing NFT. You only need provide necessary env params and run it

# Build

```shell
mvn clean install
```
or you can use maven wrapper
```shell
./mvnw clean install
```

# Run

```shell
java -DMONGO_DB_URI= -DMONGO_DB_DATABASE= -DAWS_ACCESS_KEY= -DAWS_SECRET_KEY= -DAWS_BUCKET= -DWALLET_ADDRESS= -DNFT_COLLECTION_ADDRESS= -DNFT_COLLECTION_OWNER= -DNFT_COLLECTION_NAME= -DNFT_COLLECTION_DESCRIPTION= -DNFT_COLLECTION_SYMBOL= -DNFT_COLLECTION_AVATAR= -DNFT_COLLECTION_BANNER= -DNFT_COLLECTION_MODIFY_TOKEN_NAME= -jar target/pancake-nft-publish-0.0.1-SNAPSHOT.jar LIST
```

App parameters:

```shell
MONGO_DB_URI                     - uri conection to PROD MongoDB 

MONGO_DB_DATABASE                - database name

AWS_ACCESS_KEY                   - your aws account access key

AWS_SECRET_KEY                   - your aws account secret key 

AWS_BUCKET                       - NFT bucket

WALLET_ADDRESS                   - your BSC wallet (user for calling function from blockchain)

NFT_COLLECTION_ADDRESS           - listed NFT smart contract address 

NFT_COLLECTION_OWNER             - listed NFT owner address (fee) 

NFT_COLLECTION_NAME              - name of collection

NFT_COLLECTION_DESCRIPTION       - description of collection 

NFT_COLLECTION_SYMBOL            - symbol of collection (can be found on BSC smart contract)

NFT_COLLECTION_AVATAR            - url to .png image

NFT_COLLECTION_BANNER            - url to .png image

NFT_COLLECTION_MODIFY_TOKEN_NAME - if true: add token id to token name (making token name uniq)
```

App attributes:
(no case sensitive)

```shell
LIST              - list NFT collection

RELIST_TOKENS     - relist some tokens (should be provided secound attributes, list of tokenIds separated by coma - '1,2,3').
                    on the end of LIST collection, ids of failed token will be logged if any present. So this options can be used in that case.

RELIST_COLLECTION - relist whole collection (could be provided secount attribute 'delete' means delete existing collection before relist)   
```