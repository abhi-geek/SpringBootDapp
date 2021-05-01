package com.EthereumDapp.PrimeNft.controller;


import com.EthereumDapp.PrimeNft.contract.PrimeNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

@RestController
@RequestMapping("/dapp")
public class PrimeNftController {

    Logger logger = LoggerFactory.getLogger(PrimeNftController.class);

    private final static String PRIVATE_KEY_ACCT2 = "ed0917a6bf4479588b78a8bb4d8b7bdf903d0d0c5abdfd3cca9462c93eec1293";
    private final static String PRIVATE_KEY_ACCT1 = "e18a11c1fe3b881e239b1fe2705d20382ee00db2b1c4ecc51f109297d4b74915";
    private final static String PRIVATE_KEY_ACCT3 = "6a854713ef796bea68cdd4642bd325291caebbba0ade8f5e9c7d9aaaff755afd";
    private final static String CONTRACT_ADDRESS = "0xdfDE039FAcFD345E3dc83CFD2556AE1da66F399E";

    private final static BigInteger GAS_LIMIT = BigInteger.valueOf(6721L);
    private final static BigInteger GAS_PRICE = BigInteger.valueOf(200000L);

    private final static String Acct1 = "0xb6ec67734946D44fb019BFD22A3D62503F4f6769";
    private final static String Acct2 = "0x14104C4EbD9973B9cF53274327ca0D3822601C3D";
    private final static String Acct3 = "0xc76beE05500ce92B49a4B9caaaAD31ca790D6100";
    private final static Map<Integer,String> accountMap = Map.ofEntries(
            entry(1, "0xb6ec67734946D44fb019BFD22A3D62503F4f6769"),
            entry(2, "0x14104C4EbD9973B9cF53274327ca0D3822601C3D"),
            entry(3, "0xc76beE05500ce92B49a4B9caaaAD31ca790D6100")
    );

    private  Web3j web3j;

    @GetMapping("/walletBalance")
    public String walletBalance(@RequestParam("AccountNumber") Integer account){
        String result = null;
        try {
            // We start by creating a new web3j instance to connect to remote nodes on the network.
            Web3j web3j = getWeb3jConnection();

            EthGetBalance ethGetBalance = web3j
                    .ethGetBalance(accountMap.get(account), DefaultBlockParameterName.LATEST)
                    .sendAsync()
                    .get();

            result = Convert.fromWei(String.valueOf(ethGetBalance.getBalance()),Convert.Unit.ETHER).toString();


        } catch (Exception e) {
            logger.info("Exception message : "+e.getMessage() );
            e.printStackTrace();
            result = "Connection Failed";
        }
        if (result==null) result = "Connection Failed";
        return result;
    }

    @GetMapping("/checkEthConnection")
    public String checkEthConnection() {
        String result = null;
        try {
            // We start by creating a new web3j instance to connect to remote nodes on the network.
            Web3j web3j = getWeb3jConnection();
            result = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            logger.info("Connected to Ethereum client version: "
                    + result);


        } catch (Exception e) {
            logger.info("Exception message : "+e.getMessage() );
            e.printStackTrace();
            result = "Connection Failed";
        }
        if (result==null) result = "Connection Failed";
        return result;
    }

    @GetMapping("/mintPrimeNumbers")
    public List primeNft(@RequestParam("token") Integer token,@RequestParam("from") Integer from) {
        List res = null;
        try {
            // We start by creating a new web3j instance to connect to remote nodes on the network.
            Web3j web3j = getWeb3jConnection();

            TransactionManager transactionManager = new RawTransactionManager(
                    web3j,
                    getCredentialsFromPrivateKey(from)
            );

            logger.info("Credentials loaded");
            ContractGasProvider contractGasProvider = new StaticGasProvider(GAS_PRICE,GAS_LIMIT);
            PrimeNumber contract = PrimeNumber.load(
                    CONTRACT_ADDRESS, web3j, transactionManager,contractGasProvider);

            String contractAddress = contract.getContractAddress();
            logger.info("Minting tokens ....");

            if (Objects.nonNull(token)){
                TransactionReceipt receipt = contract.mint("31").send();
            }
            res = contract.getprimeNumbersNft().send();

            logger.info(res.toString());
        } catch (Exception e) {
            logger.info("Exception message : "+e.getMessage() );
            e.printStackTrace();
        }
        return res;
    }

    @GetMapping("/sendTxn")
    public String sendEth(@RequestParam("From") Integer from,@RequestParam("to") Integer to,@RequestParam("amountInEth") Double amountInEth){
        String result = null;
        try{
            // We start by creating a new web3j instance to connect to remote nodes on the network.
            Web3j web3j = getWeb3jConnection();
            logger.info("Connected to Ethereum client version: "
                    + web3j.web3ClientVersion().send().getWeb3ClientVersion());

            TransactionManager transactionManager = new RawTransactionManager(
                    web3j,
                    getCredentialsFromPrivateKey(from)
            );

            Transfer transfer = new Transfer(web3j, transactionManager);

            TransactionReceipt transactionReceipt = transfer.sendFunds(
                    accountMap.get(to),
                    BigDecimal.valueOf(amountInEth),
                    Convert.Unit.ETHER,
                    GAS_PRICE,
                    GAS_LIMIT
            ).send();

            System.out.print("Transaction = " + transactionReceipt.getTransactionHash());
        }catch (Exception ex){
            logger.info("Exception message : "+ex.getMessage() );
            ex.printStackTrace();
            result = "Connection Failed";
        }
        return result;
    }

    private Credentials getCredentialsFromPrivateKey(Integer account) {
        if(account.equals(3)) return Credentials.create(PRIVATE_KEY_ACCT3);
        if(account.equals(2)) return Credentials.create(PRIVATE_KEY_ACCT2);
        return Credentials.create(PRIVATE_KEY_ACCT1);
    }

    private Web3j getWeb3jConnection() throws IOException {
        if(Objects.nonNull(web3j)) return web3j;
        web3j = Web3j.build(new HttpService("http://localhost:7545"));

        logger.info("Connected to Ethereum client version: "
                + web3j.web3ClientVersion().send().getWeb3ClientVersion());
        return web3j;
    }

    @GetMapping("/factorial")
    public BigInteger primeNft(@RequestParam("number") BigInteger token) {
        BigInteger res = null;
        try {
            // We start by creating a new web3j instance to connect to remote nodes on the network.
            Web3j web3j = getWeb3jConnection();

            TransactionManager transactionManager = new RawTransactionManager(
                    web3j,
                    getCredentialsFromPrivateKey(2)
            );

            logger.info("Credentials loaded");
            ContractGasProvider contractGasProvider = new StaticGasProvider(GAS_PRICE,GAS_LIMIT);
            PrimeNumber contract = PrimeNumber.load(
                    CONTRACT_ADDRESS, web3j, transactionManager,contractGasProvider);

            String contractAddress = contract.getContractAddress();
            logger.info("calling contract ....");
            res = contract.getFactorial(token).send();

            logger.info(res.toString());
        } catch (Exception e) {
            logger.info("Exception message : "+e.getMessage() );
            e.printStackTrace();
        }
        return res;
    }

}
