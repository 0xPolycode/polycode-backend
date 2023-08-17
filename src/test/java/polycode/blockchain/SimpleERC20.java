package polycode.blockchain;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import java.math.BigInteger;
import java.util.ArrayList;
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
public class SimpleERC20 extends Contract {
    public static final String BINARY = "60806040523480156200001157600080fd5b506040516200099e3803806200099e8339810160408190526200003491620001fa565b8151835114620000a05760405162461bcd60e51b815260206004820152602d60248201527f4d69736d61746368696e67206163636f756e7420616e642062616c616e63652060448201526c6172726179206c656e6774687360981b606482015260840160405180910390fd5b60005b83518110156200013c57828181518110620000ce57634e487b7160e01b600052603260045260246000fd5b6020026020010151600080868481518110620000fa57634e487b7160e01b600052603260045260246000fd5b60200260200101516001600160a01b03166001600160a01b03168152602001908152602001600020819055508080620001339062000336565b915050620000a3565b50600280546001600160a01b0319166001600160a01b039290921691909117905550620003749050565b80516001600160a01b03811681146200017e57600080fd5b919050565b600082601f83011262000194578081fd5b81516020620001ad620001a78362000310565b620002dd565b80838252828201915082860187848660051b8901011115620001cd578586fd5b855b85811015620001ed57815184529284019290840190600101620001cf565b5090979650505050505050565b6000806000606084860312156200020f578283fd5b83516001600160401b038082111562000226578485fd5b818601915086601f8301126200023a578485fd5b815160206200024d620001a78362000310565b8083825282820191508286018b848660051b89010111156200026d57898afd5b8996505b848710156200029a57620002858162000166565b83526001969096019591830191830162000271565b5091890151919750909350505080821115620002b4578384fd5b50620002c38682870162000183565b925050620002d46040850162000166565b90509250925092565b604051601f8201601f191681016001600160401b03811182821017156200030857620003086200035e565b604052919050565b60006001600160401b038211156200032c576200032c6200035e565b5060051b60200190565b60006000198214156200035757634e487b7160e01b81526011600452602481fd5b5060010190565b634e487b7160e01b600052604160045260246000fd5b61061a80620003846000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c8063095ea7b31461005157806323b872dd1461006657806370a082311461008e578063a9059cbb146100c5575b600080fd5b61006461005f366004610576565b6100d8565b005b61007961007436600461053b565b610134565b60405190151581526020015b60405180910390f35b6100b761009c36600461051a565b6001600160a01b031660009081526020819052604090205490565b604051908152602001610085565b6100796100d3366004610576565b6103ad565b6001600160a01b03821660008181526001602090815260408083203380855290835292819020859055518481527f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925910160405180910390a35050565b60006001600160a01b0384166101915760405162461bcd60e51b815260206004820152601e60248201527f5472616e736665722066726f6d20746865207a65726f2061646472657373000060448201526064015b60405180910390fd5b6001600160a01b0383166101e75760405162461bcd60e51b815260206004820152601c60248201527f5472616e7366657220746f20746865207a65726f2061646472657373000000006044820152606401610188565b6001600160a01b03841660009081526020819052604090205482111561024f5760405162461bcd60e51b815260206004820152601f60248201527f5472616e7366657220616d6f756e7420657863656564732062616c616e6365006044820152606401610188565b3360009081526001602090815260408083206001600160a01b03881684529091529020548211156102c25760405162461bcd60e51b815260206004820152601b60248201527f4e6f7420656e6f75676820617070726f7665642062616c616e636500000000006044820152606401610188565b6001600160a01b038416600090815260208190526040812080548492906102ea9084906105b7565b90915550506001600160a01b0383166000908152602081905260408120805484929061031790849061059f565b90915550503360009081526001602090815260408083206001600160a01b03881684529091528120805484929061034f9084906105b7565b92505081905550826001600160a01b0316846001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8460405161039b91815260200190565b60405180910390a35060019392505050565b60006001600160a01b0383166104055760405162461bcd60e51b815260206004820152601c60248201527f5472616e7366657220746f20746865207a65726f2061646472657373000000006044820152606401610188565b336000908152602081905260409020548211156104645760405162461bcd60e51b815260206004820152601f60248201527f5472616e7366657220616d6f756e7420657863656564732062616c616e6365006044820152606401610188565b33600090815260208190526040812080548492906104839084906105b7565b90915550506001600160a01b038316600090815260208190526040812080548492906104b090849061059f565b90915550506040518281526001600160a01b0384169033907fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9060200160405180910390a350600192915050565b80356001600160a01b038116811461051557600080fd5b919050565b60006020828403121561052b578081fd5b610534826104fe565b9392505050565b60008060006060848603121561054f578182fd5b610558846104fe565b9250610566602085016104fe565b9150604084013590509250925092565b60008060408385031215610588578182fd5b610591836104fe565b946020939093013593505050565b600082198211156105b2576105b26105ce565b500190565b6000828210156105c9576105c96105ce565b500390565b634e487b7160e01b600052601160045260246000fdfea2646970667358221220190afac23cd2f2a2d9e51955080e720850ffb55302dd8c707cccf8ff717db01164736f6c63430008040033";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_TRANSFER = "transfer";

    public static final String FUNC_TRANSFERFROM = "transferFrom";

    public static final Event APPROVAL_EVENT = new Event("Approval",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRANSFER_EVENT = new Event("Transfer",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected SimpleERC20(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected SimpleERC20(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected SimpleERC20(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected SimpleERC20(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<ApprovalEventResponse> getApprovalEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(APPROVAL_EVENT, transactionReceipt);
        ArrayList<ApprovalEventResponse> responses = new ArrayList<ApprovalEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ApprovalEventResponse typedResponse = new ApprovalEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ApprovalEventResponse>() {
            @Override
            public ApprovalEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(APPROVAL_EVENT, log);
                ApprovalEventResponse typedResponse = new ApprovalEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVAL_EVENT));
        return approvalEventFlowable(filter);
    }

    public List<TransferEventResponse> getTransferEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(TRANSFER_EVENT, transactionReceipt);
        ArrayList<TransferEventResponse> responses = new ArrayList<TransferEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            TransferEventResponse typedResponse = new TransferEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<TransferEventResponse> transferEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, TransferEventResponse>() {
            @Override
            public TransferEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(TRANSFER_EVENT, log);
                TransferEventResponse typedResponse = new TransferEventResponse();
                typedResponse.log = log;
                typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<TransferEventResponse> transferEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        return transferEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> approve(String spender, BigInteger amount) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_APPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, spender),
                        new org.web3j.abi.datatypes.generated.Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String account) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_BALANCEOF,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, account)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transfer(String recipient, BigInteger amount) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, recipient),
                        new org.web3j.abi.datatypes.generated.Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferFrom(String from, String to, BigInteger amount) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TRANSFERFROM,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, from),
                        new org.web3j.abi.datatypes.Address(160, to),
                        new org.web3j.abi.datatypes.generated.Uint256(amount)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static SimpleERC20 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleERC20(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static SimpleERC20 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new SimpleERC20(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static SimpleERC20 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new SimpleERC20(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static SimpleERC20 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new SimpleERC20(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<SimpleERC20> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, List<String> accounts, List<BigInteger> balances, String ownerAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(accounts, org.web3j.abi.datatypes.Address.class)),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(balances, org.web3j.abi.datatypes.generated.Uint256.class)),
                new org.web3j.abi.datatypes.Address(160, ownerAddress)));
        return deployRemoteCall(SimpleERC20.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<SimpleERC20> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, List<String> accounts, List<BigInteger> balances, String ownerAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(accounts, org.web3j.abi.datatypes.Address.class)),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(balances, org.web3j.abi.datatypes.generated.Uint256.class)),
                new org.web3j.abi.datatypes.Address(160, ownerAddress)));
        return deployRemoteCall(SimpleERC20.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<SimpleERC20> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, List<String> accounts, List<BigInteger> balances, String ownerAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(accounts, org.web3j.abi.datatypes.Address.class)),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(balances, org.web3j.abi.datatypes.generated.Uint256.class)),
                new org.web3j.abi.datatypes.Address(160, ownerAddress)));
        return deployRemoteCall(SimpleERC20.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<SimpleERC20> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, List<String> accounts, List<BigInteger> balances, String ownerAddress) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(accounts, org.web3j.abi.datatypes.Address.class)),
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(balances, org.web3j.abi.datatypes.generated.Uint256.class)),
                new org.web3j.abi.datatypes.Address(160, ownerAddress)));
        return deployRemoteCall(SimpleERC20.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class ApprovalEventResponse extends BaseEventResponse {
        public String owner;

        public String spender;

        public BigInteger value;
    }

    public static class TransferEventResponse extends BaseEventResponse {
        public String from;

        public String to;

        public BigInteger value;
    }
}
