package com.pancakeswap.nft.publish.service;

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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class BlockChainService {

    @Value("${nft.collection.address}")
    private String contract;
    @Value("${wallet.address}")
    private String address;
    private final Web3j web3j;

    public BlockChainService() {
        this.web3j = Web3j.build(new HttpService("https://bsc-dataseed1.binance.org:443"));;
    }

    public BigInteger getTotalSupply() throws ExecutionException, InterruptedException {
        Function function = new Function(
                "totalSupply",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Uint>() {}));

        List<Type> res = callBlockchainFunction(function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (BigInteger) res.get(0).getValue();
    }

    public BigInteger getTokenId(Integer index) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "tokenByIndex",
                Arrays.asList(new Uint(BigInteger.valueOf(index))),
                Arrays.asList(new TypeReference<Uint>() {}));

        List<Type> res = callBlockchainFunction(function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (BigInteger) res.get(0).getValue();
    }

    public String getTokenURI(BigInteger index) throws ExecutionException, InterruptedException {
        Function function = new Function(
                "tokenURI",
                Arrays.asList(new Uint(index)),
                Arrays.asList(new TypeReference<Utf8String>() {}));

        List<Type> res = callBlockchainFunction(function);
        if (res.isEmpty()) {
            throw new RuntimeException("Decoded response is empty");
        }
        return (String) res.get(0).getValue();
    }

    private List<Type> callBlockchainFunction(Function function) throws ExecutionException, InterruptedException {
        String encodedFunction = FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(address, contract, encodedFunction),
                DefaultBlockParameterName.LATEST)
                .sendAsync().get();

        return FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
    }

}
