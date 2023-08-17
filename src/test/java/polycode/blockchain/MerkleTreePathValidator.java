package polycode.blockchain;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.FixedContract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

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
public class MerkleTreePathValidator extends FixedContract {
    public static final String BINARY = "608060405234801561001057600080fd5b506102e1806100206000396000f3fe608060405234801561001057600080fd5b506004361061002b5760003560e01c80639ddc3d7914610030575b600080fd5b61004361003e366004610171565b610057565b604051901515815260200160405180910390f35b600084825114610069575060006100ab565b604080516001600160a01b038616602080830191909152818301869052825180830384018152606090920190925280519101206100a78388836100b4565b9150505b95945050505050565b600081815b85518110156101665760008682815181106100e457634e487b7160e01b600052603260045260246000fd5b60200260200101519050808311610126576040805160208101859052908101829052606001604051602081830303815290604052805190602001209250610153565b60408051602081018390529081018490526060016040516020818303038152906040528051906020012092505b508061015e8161026e565b9150506100b9565b509092149392505050565b600080600080600060a08688031215610188578081fd5b85359450602080870135945060408701356001600160a01b03811681146101ad578283fd5b935060608701359250608087013567ffffffffffffffff808211156101d0578384fd5b818901915089601f8301126101e3578384fd5b8135818111156101f5576101f5610295565b8060051b604051601f19603f8301168101818110858211171561021a5761021a610295565b604052828152858101935084860182860187018e1015610238578788fd5b8795505b8386101561025a57803585526001959095019493860193860161023c565b508096505050505050509295509295909350565b600060001982141561028e57634e487b7160e01b81526011600452602481fd5b5060010190565b634e487b7160e01b600052604160045260246000fdfea26469706673582212208a81b3361bbb772ce998a533e5f6e2ec9f45c80cdd9fe4fa6fe8a3ce93433fd264736f6c63430008040033";

    public static final String FUNC_CONTAINSNODE = "containsNode";

    @Deprecated
    protected MerkleTreePathValidator(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected MerkleTreePathValidator(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MerkleTreePathValidator(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected MerkleTreePathValidator(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<Boolean> containsNode(byte[] merkleTreeRoot, BigInteger treeDepth, String wallet, BigInteger balance, List<byte[]> proof) {
        final Function function = new Function(FUNC_CONTAINSNODE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(merkleTreeRoot),
                        new org.web3j.abi.datatypes.generated.Uint256(treeDepth),
                        new org.web3j.abi.datatypes.Address(160, wallet),
                        new org.web3j.abi.datatypes.generated.Uint256(balance),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                                org.web3j.abi.datatypes.generated.Bytes32.class,
                                org.web3j.abi.Utils.typeMap(proof, org.web3j.abi.datatypes.generated.Bytes32.class))),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static MerkleTreePathValidator load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MerkleTreePathValidator(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MerkleTreePathValidator load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MerkleTreePathValidator(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MerkleTreePathValidator load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MerkleTreePathValidator(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MerkleTreePathValidator load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MerkleTreePathValidator(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MerkleTreePathValidator> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(MerkleTreePathValidator.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<MerkleTreePathValidator> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(MerkleTreePathValidator.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<MerkleTreePathValidator> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(MerkleTreePathValidator.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<MerkleTreePathValidator> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(MerkleTreePathValidator.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
