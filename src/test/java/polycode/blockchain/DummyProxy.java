package polycode.blockchain;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.Arrays;

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
public class DummyProxy extends Contract {
    public static final String BINARY = "608060405260405161010538038061010583398101604081905261002291610047565b600080546001600160a01b0319166001600160a01b0392909216919091179055610075565b600060208284031215610058578081fd5b81516001600160a01b038116811461006e578182fd5b9392505050565b6082806100836000396000f3fe6080604052348015600f57600080fd5b506004361060285760003560e01c80635c60da1b14602d575b600080fd5b600054604080516001600160a01b039092168252519081900360200190f3fea2646970667358221220f4c0050425938b9e7a2a470c029e67f6a11fd2af582f4fd18a36c9b9d1ad6bbc64736f6c63430008040033";

    public static final String FUNC_IMPLEMENTATION = "implementation";

    @Deprecated
    protected DummyProxy(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DummyProxy(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DummyProxy(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DummyProxy(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<String> implementation() {
        final Function function = new Function(FUNC_IMPLEMENTATION,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static DummyProxy load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DummyProxy(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DummyProxy load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DummyProxy(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DummyProxy load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DummyProxy(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DummyProxy load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DummyProxy(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DummyProxy> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String impl) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, impl)));
        return deployRemoteCall(DummyProxy.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<DummyProxy> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String impl) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, impl)));
        return deployRemoteCall(DummyProxy.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DummyProxy> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String impl) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, impl)));
        return deployRemoteCall(DummyProxy.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DummyProxy> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String impl) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, impl)));
        return deployRemoteCall(DummyProxy.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }
}
