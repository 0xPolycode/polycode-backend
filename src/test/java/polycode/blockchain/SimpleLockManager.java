package polycode.blockchain;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class SimpleLockManager extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b506101c1806100206000396000f3fe608060405234801561001057600080fd5b50600436106100365760003560e01c8063a4e2d6341461003b578063bc633cd414610055575b600080fd5b60005460ff16604051901515815260200160405180910390f35b610077610063366004610095565b50506000805460ff19166001179055505050565b005b80356001600160a01b038116811461009057600080fd5b919050565b600080600080600060a086880312156100ac578081fd5b6100b586610079565b94506020860135935060408601359250606086013567ffffffffffffffff808211156100df578283fd5b818801915088601f8301126100f2578283fd5b81358181111561010457610104610175565b604051601f8201601f19908116603f0116810190838211818310171561012c5761012c610175565b816040528281528b6020848701011115610144578586fd5b8260208601602083013791820160200185905250935061016991505060808701610079565b90509295509295909350565b634e487b7160e01b600052604160045260246000fdfea264697066735822122094c76a229c4535984207cdbcfb51dcf66c5f24b804600bad03443817228a1e9764736f6c63430008040033";

    public static final String FUNC_ISLOCKED = "isLocked";

    public static final String FUNC_LOCK = "lock";

    @Deprecated
    protected SimpleLockManager(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SimpleLockManager(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SimpleLockManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SimpleLockManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<Boolean> isLocked() {
        final Function function = new Function(FUNC_ISLOCKED,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> lock(String tokenAddress, BigInteger amount, BigInteger duration, String info, String unlockPrivilegeWallet) {
        final Function function = new Function(
                FUNC_LOCK,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, tokenAddress),
                        new org.web3j.abi.datatypes.generated.Uint256(amount),
                        new org.web3j.abi.datatypes.generated.Uint256(duration),
                        new org.web3j.abi.datatypes.Utf8String(info),
                        new org.web3j.abi.datatypes.Address(160, unlockPrivilegeWallet)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static SimpleLockManager load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleLockManager(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SimpleLockManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleLockManager(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SimpleLockManager load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SimpleLockManager(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SimpleLockManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SimpleLockManager(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SimpleLockManager> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleLockManager.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SimpleLockManager> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SimpleLockManager.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<SimpleLockManager> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleLockManager.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SimpleLockManager> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SimpleLockManager.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
