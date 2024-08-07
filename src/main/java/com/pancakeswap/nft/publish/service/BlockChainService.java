package com.pancakeswap.nft.publish.service;

import com.pancakeswap.nft.publish.model.sc.NftInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class BlockChainService {

    @Value("${wallet.address}")
    private String walletAddress;
    @Value("${node.url}")
    private String nodeUrl;
    private Web3j web3j;

    public BlockChainService() {
    }

    @PostConstruct
    public void afterInit() {
        this.web3j = Web3j.build(new HttpService(nodeUrl));
    }

    public BigInteger getTotalSupply(String collectionAddress) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "totalSupply",
                Collections.emptyList(),
                List.of(new TypeReference<Uint>() {
                }));

        List<Type> res = callBlockchainFunction(collectionAddress, function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (BigInteger) res.get(0).getValue();
    }

    public BigInteger getTokenId(String collectionAddress, Integer index) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "tokenByIndex",
                List.of(new Uint(BigInteger.valueOf(index))),
                List.of(new TypeReference<Uint>() {
                }));

        List<Type> res = callBlockchainFunction(collectionAddress, function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (BigInteger) res.get(0).getValue();
    }

    public BigInteger getBunnyId(String collectionAddress, BigInteger index) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "getBunnyId",
                List.of(new Uint(index)),
                List.of(new TypeReference<Uint>() {
                }));

        List<Type> res = callBlockchainFunction(collectionAddress, function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (BigInteger) res.get(0).getValue();
    }

    public String getTokenURI(String collectionAddress, BigInteger index) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "tokenURI",
                List.of(new Uint(index)),
                List.of(new TypeReference<Utf8String>() {
                }));

        List<Type> res = callBlockchainFunction(collectionAddress, function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (String) res.get(0).getValue();
    }

    public NftInfo getNftInfo(String collectionAddress, BigInteger tokenId) throws ExecutionException, InterruptedException {
        Function function = new Function("getNftInfo",
                List.of(new Uint(tokenId)),
                List.of(new TypeReference<Uint>() {
                }, new TypeReference<Uint>() {
                }, new TypeReference<Uint>() {
                }));

        List<Type> response = callBlockchainFunction(collectionAddress, function);
        if (response.size() != 3) {
            throw new RuntimeException("Decoded response is empty");
        }

        return new NftInfo(
                (BigInteger) response.get(0).getValue(),
                (BigInteger) response.get(1).getValue(),
                (BigInteger) response.get(2).getValue());
    }

    public BigInteger getLastTokenId(String collectionAddress) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "lastTokenId",
                Collections.emptyList(),
                List.of(new TypeReference<Uint>() {
                }));

        List<Type> res = callBlockchainFunction(collectionAddress, function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (BigInteger) res.get(0).getValue();
    }

    private List<Type> callBlockchainFunction(String collectionAddress, Function function) throws ExecutionException, InterruptedException {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                        Transaction.createEthCallTransaction(walletAddress, collectionAddress, encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        return FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
    }

}
