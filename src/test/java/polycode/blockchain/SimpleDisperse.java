package polycode.blockchain;

import org.web3j.abi.TypeReference;
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
public class SimpleDisperse extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5061066b806100206000396000f3fe6080604052600436106100295760003560e01c8063c73a2d601461002e578063e63d38ed14610050575b600080fd5b34801561003a57600080fd5b5061004e610049366004610492565b610063565b005b61004e61005e3660046103a8565b610257565b6000805b83518110156100b75782818151811061009057634e487b7160e01b600052603260045260246000fd5b6020026020010151826100a391906105be565b9150806100af816105d6565b915050610067565b506040516323b872dd60e01b8152336004820152306024820152604481018290526001600160a01b038516906323b872dd90606401602060405180830381600087803b15801561010657600080fd5b505af115801561011a573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061013e919061046b565b61014757600080fd5b60005b835181101561025057846001600160a01b031663a9059cbb85838151811061018257634e487b7160e01b600052603260045260246000fd5b60200260200101518584815181106101aa57634e487b7160e01b600052603260045260246000fd5b60200260200101516040518363ffffffff1660e01b81526004016101e39291906001600160a01b03929092168252602082015260400190565b602060405180830381600087803b1580156101fd57600080fd5b505af1158015610211573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610235919061046b565b61023e57600080fd5b80610248816105d6565b91505061014a565b5050505050565b60005b82518110156102fd5782818151811061028357634e487b7160e01b600052603260045260246000fd5b60200260200101516001600160a01b03166108fc8383815181106102b757634e487b7160e01b600052603260045260246000fd5b60200260200101519081150290604051600060405180830381858888f193505050501580156102ea573d6000803e3d6000fd5b50806102f5816105d6565b91505061025a565b5047801561033457604051339082156108fc029083906000818181858888f19350505050158015610332573d6000803e3d6000fd5b505b505050565b600082601f830112610349578081fd5b8135602061035e6103598361059a565b610569565b80838252828201915082860187848660051b890101111561037d578586fd5b855b8581101561039b5781358452928401929084019060010161037f565b5090979650505050505050565b600080604083850312156103ba578182fd5b823567ffffffffffffffff808211156103d1578384fd5b818501915085601f8301126103e4578384fd5b813560206103f46103598361059a565b8083825282820191508286018a848660051b8901011115610413578889fd5b8896505b8487101561043e57803561042a8161061d565b835260019690960195918301918301610417565b5096505086013592505080821115610454578283fd5b5061046185828601610339565b9150509250929050565b60006020828403121561047c578081fd5b8151801515811461048b578182fd5b9392505050565b6000806000606084860312156104a6578081fd5b83356104b18161061d565b925060208481013567ffffffffffffffff808211156104ce578384fd5b818701915087601f8301126104e1578384fd5b81356104ef6103598261059a565b8082825285820191508585018b878560051b880101111561050e578788fd5b8795505b838610156105395780356105258161061d565b835260019590950194918601918601610512565b50965050506040870135925080831115610551578384fd5b505061055f86828701610339565b9150509250925092565b604051601f8201601f1916810167ffffffffffffffff8111828210171561059257610592610607565b604052919050565b600067ffffffffffffffff8211156105b4576105b4610607565b5060051b60200190565b600082198211156105d1576105d16105f1565b500190565b60006000198214156105ea576105ea6105f1565b5060010190565b634e487b7160e01b600052601160045260246000fd5b634e487b7160e01b600052604160045260246000fd5b6001600160a01b038116811461063257600080fd5b5056fea2646970667358221220a1ceb103e6819f8238c7d6119c506ea437f54b2e9f8792dd3bc4ad8751d155c864736f6c63430008040033";

    public static final String FUNC_DISPERSEETHER = "disperseEther";

    public static final String FUNC_DISPERSETOKEN = "disperseToken";

    @Deprecated
    protected SimpleDisperse(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SimpleDisperse(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SimpleDisperse(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SimpleDisperse(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> disperseEther(List<String> recipients, List<BigInteger> values, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DISPERSEETHER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                                org.web3j.abi.datatypes.Address.class,
                                org.web3j.abi.Utils.typeMap(recipients, org.web3j.abi.datatypes.Address.class)),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                                org.web3j.abi.datatypes.generated.Uint256.class,
                                org.web3j.abi.Utils.typeMap(values, org.web3j.abi.datatypes.generated.Uint256.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> disperseToken(String token, List<String> recipients, List<BigInteger> values) {
        final Function function = new Function(
                FUNC_DISPERSETOKEN,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, token),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                                org.web3j.abi.datatypes.Address.class,
                                org.web3j.abi.Utils.typeMap(recipients, org.web3j.abi.datatypes.Address.class)),
                        new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                                org.web3j.abi.datatypes.generated.Uint256.class,
                                org.web3j.abi.Utils.typeMap(values, org.web3j.abi.datatypes.generated.Uint256.class))),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static SimpleDisperse load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleDisperse(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SimpleDisperse load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleDisperse(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SimpleDisperse load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SimpleDisperse(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SimpleDisperse load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SimpleDisperse(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SimpleDisperse> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleDisperse.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SimpleDisperse> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SimpleDisperse.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<SimpleDisperse> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(SimpleDisperse.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<SimpleDisperse> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(SimpleDisperse.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
