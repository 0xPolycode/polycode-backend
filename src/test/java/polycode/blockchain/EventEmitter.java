package polycode.blockchain;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
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
public class EventEmitter extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5060405161050538038061050583398101604081905261002f916100c9565b818460405161003e91906101b6565b604080519182900382208683528415156020840152916001600160a01b038916917f465bc6257dcfc79569508750afdabc2aa7d499dd8de935aa5324446084c99397910160405180910390a4846001600160a01b0316846040516100a291906101d2565b60405180910390a1505050505061024b565b805180151581146100c457600080fd5b919050565b600080600080600060a086880312156100e0578081fd5b85516001600160a01b03811681146100f6578182fd5b60208701519095506001600160401b0380821115610112578283fd5b818801915088601f830112610125578283fd5b81518181111561013757610137610235565b604051601f8201601f19908116603f0116810190838211818310171561015f5761015f610235565b816040528281528b6020848701011115610177578586fd5b610188836020830160208801610205565b809850505050505060408601519250606086015191506101aa608087016100b4565b90509295509295909350565b600082516101c8818460208701610205565b9190910192915050565b60208152600082518060208401526101f1816040850160208701610205565b601f01601f19169190910160400192915050565b60005b83811015610220578181015183820152602001610208565b8381111561022f576000848401525b50505050565b634e487b7160e01b600052604160045260246000fd5b6102ab8061025a6000396000f3fe608060405234801561001057600080fd5b506004361061002a5760003560e01c80624749351461002f575b600080fd5b61004261003d3660046100f4565b610056565b604051901515815260200160405180910390f35b6000828560405161006791906101e0565b604080519182900382208783528515156020840152916001600160a01b038a16917f465bc6257dcfc79569508750afdabc2aa7d499dd8de935aa5324446084c99397910160405180910390a4856001600160a01b0316856040516100cb91906101fc565b60405180910390a150600195945050505050565b803580151581146100ef57600080fd5b919050565b600080600080600060a0868803121561010b578081fd5b85356001600160a01b0381168114610121578182fd5b9450602086013567ffffffffffffffff8082111561013d578283fd5b818801915088601f830112610150578283fd5b8135818111156101625761016261025f565b604051601f8201601f19908116603f0116810190838211818310171561018a5761018a61025f565b816040528281528b60208487010111156101a2578586fd5b82602086016020830137918201602001859052509550505060408601359250606086013591506101d4608087016100df565b90509295509295909350565b600082516101f281846020870161022f565b9190910192915050565b602081526000825180602084015261021b81604085016020870161022f565b601f01601f19169190910160400192915050565b60005b8381101561024a578181015183820152602001610232565b83811115610259576000848401525b50505050565b634e487b7160e01b600052604160045260246000fdfea26469706673582212201547df7ebcb2abcb7c2de6838cefd7ffd066100707b9dd6a5039c245ee9a137c64736f6c63430008040033";

    public static final String FUNC_EMITEVENTS = "emitEvents";

    public static final Event ANONYMOUSEVENT_EVENT = new Event("AnonymousEvent",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event EXAMPLEEVENT_EVENT = new Event("ExampleEvent",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Utf8String>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>(true) {}, new TypeReference<Bool>() {}));
    ;

    @Deprecated
    protected EventEmitter(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected EventEmitter(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected EventEmitter(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected EventEmitter(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<AnonymousEventEventResponse> getAnonymousEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ANONYMOUSEVENT_EVENT, transactionReceipt);
        ArrayList<AnonymousEventEventResponse> responses = new ArrayList<AnonymousEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AnonymousEventEventResponse typedResponse = new AnonymousEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.message = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AnonymousEventEventResponse> anonymousEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, AnonymousEventEventResponse>() {
            @Override
            public AnonymousEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ANONYMOUSEVENT_EVENT, log);
                AnonymousEventEventResponse typedResponse = new AnonymousEventEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.message = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AnonymousEventEventResponse> anonymousEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ANONYMOUSEVENT_EVENT));
        return anonymousEventEventFlowable(filter);
    }

    public List<ExampleEventEventResponse> getExampleEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXAMPLEEVENT_EVENT, transactionReceipt);
        ArrayList<ExampleEventEventResponse> responses = new ArrayList<ExampleEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExampleEventEventResponse typedResponse = new ExampleEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.message = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.value = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.example = (Boolean) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ExampleEventEventResponse> exampleEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ExampleEventEventResponse>() {
            @Override
            public ExampleEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(EXAMPLEEVENT_EVENT, log);
                ExampleEventEventResponse typedResponse = new ExampleEventEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.message = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.value = (BigInteger) eventValues.getIndexedValues().get(2).getValue();
                typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.example = (Boolean) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ExampleEventEventResponse> exampleEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXAMPLEEVENT_EVENT));
        return exampleEventEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> emitEvents(String owner, String message, BigInteger amount, BigInteger value, Boolean example) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_EMITEVENTS,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                        new org.web3j.abi.datatypes.Utf8String(message),
                        new org.web3j.abi.datatypes.generated.Uint256(amount),
                        new org.web3j.abi.datatypes.generated.Uint256(value),
                        new org.web3j.abi.datatypes.Bool(example)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static EventEmitter load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new EventEmitter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static EventEmitter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new EventEmitter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static EventEmitter load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new EventEmitter(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static EventEmitter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new EventEmitter(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<EventEmitter> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String owner, String message, BigInteger amount, BigInteger value, Boolean example) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                new org.web3j.abi.datatypes.Utf8String(message),
                new org.web3j.abi.datatypes.generated.Uint256(amount),
                new org.web3j.abi.datatypes.generated.Uint256(value),
                new org.web3j.abi.datatypes.Bool(example)));
        return deployRemoteCall(EventEmitter.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<EventEmitter> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String owner, String message, BigInteger amount, BigInteger value, Boolean example) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                new org.web3j.abi.datatypes.Utf8String(message),
                new org.web3j.abi.datatypes.generated.Uint256(amount),
                new org.web3j.abi.datatypes.generated.Uint256(value),
                new org.web3j.abi.datatypes.Bool(example)));
        return deployRemoteCall(EventEmitter.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<EventEmitter> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String owner, String message, BigInteger amount, BigInteger value, Boolean example) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                new org.web3j.abi.datatypes.Utf8String(message),
                new org.web3j.abi.datatypes.generated.Uint256(amount),
                new org.web3j.abi.datatypes.generated.Uint256(value),
                new org.web3j.abi.datatypes.Bool(example)));
        return deployRemoteCall(EventEmitter.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<EventEmitter> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String owner, String message, BigInteger amount, BigInteger value, Boolean example) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner),
                new org.web3j.abi.datatypes.Utf8String(message),
                new org.web3j.abi.datatypes.generated.Uint256(amount),
                new org.web3j.abi.datatypes.generated.Uint256(value),
                new org.web3j.abi.datatypes.Bool(example)));
        return deployRemoteCall(EventEmitter.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class AnonymousEventEventResponse extends BaseEventResponse {
        public String owner;

        public String message;
    }

    public static class ExampleEventEventResponse extends BaseEventResponse {
        public String owner;

        public byte[] message;

        public BigInteger value;

        public BigInteger amount;

        public Boolean example;
    }
}
