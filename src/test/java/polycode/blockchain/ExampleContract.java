package polycode.blockchain;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
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
public class ExampleContract extends Contract {
    public static final String BINARY = "6080604052604051610241380380610241833981016040819052610022916100ab565b600080546001600160a01b0319166001600160a01b03831690811790915560408051602080820183529083905290519182520160408051918290038220602080840183526001600160a01b038516938490529151928352917f69a766900e90becfd154fc1d070ac01e5c63a59f0ff928ff58cad2126b204091910160405180910390a2506100d9565b6000602082840312156100bc578081fd5b81516001600160a01b03811681146100d2578182fd5b9392505050565b610159806100e86000396000f3fe608060405234801561001057600080fd5b50600436106100365760003560e01c806313af40351461003b578063893d20e814610050575b600080fd5b61004e6100493660046100f5565b61006f565b005b600054604080516001600160a01b039092168252519081900360200190f35b600080546001600160a01b0319166001600160a01b03831690811790915560408051602080820183529083905290519182520160408051918290038220602080840183526001600160a01b038516938490529151928352917f69a766900e90becfd154fc1d070ac01e5c63a59f0ff928ff58cad2126b204091910160405180910390a250565b600060208284031215610106578081fd5b81356001600160a01b038116811461011c578182fd5b939250505056fea264697066735822122047c20863208c0d498dab178ed6e76065ca8aa1b150387c04583e0c14f0aab1b464736f6c63430008040033";

    public static final String FUNC_GETOWNER = "getOwner";

    public static final String FUNC_SETOWNER = "setOwner";

    public static final Event EXAMPLEEVENT_EVENT = new Event("ExampleEvent",
            Arrays.<TypeReference<?>>asList(new TypeReference<ExampleStruct>() {}, new TypeReference<ExampleStruct>(true) {}));
    ;

    @Deprecated
    protected ExampleContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ExampleContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ExampleContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ExampleContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<ExampleEventEventResponse> getExampleEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(EXAMPLEEVENT_EVENT, transactionReceipt);
        ArrayList<ExampleEventEventResponse> responses = new ArrayList<ExampleEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ExampleEventEventResponse typedResponse = new ExampleEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.indexedStruct = (ExampleStruct) eventValues.getIndexedValues().get(0);
            typedResponse.nonIndexedStruct = (ExampleStruct) eventValues.getNonIndexedValues().get(0);
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
                typedResponse.indexedStruct = (ExampleStruct) eventValues.getIndexedValues().get(0);
                typedResponse.nonIndexedStruct = (ExampleStruct) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Flowable<ExampleEventEventResponse> exampleEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EXAMPLEEVENT_EVENT));
        return exampleEventEventFlowable(filter);
    }

    public RemoteFunctionCall<String> getOwner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETOWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setOwner(String owner) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ExampleContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ExampleContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ExampleContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ExampleContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ExampleContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ExampleContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ExampleContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ExampleContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ExampleContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String owner) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)));
        return deployRemoteCall(ExampleContract.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ExampleContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String owner) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)));
        return deployRemoteCall(ExampleContract.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ExampleContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String owner) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)));
        return deployRemoteCall(ExampleContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ExampleContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String owner) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner)));
        return deployRemoteCall(ExampleContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class ExampleStruct extends StaticStruct {
        public String owner;

        public ExampleStruct(String owner) {
            super(new org.web3j.abi.datatypes.Address(owner));
            this.owner = owner;
        }

        public ExampleStruct(Address owner) {
            super(owner);
            this.owner = owner.getValue();
        }
    }

    public static class ExampleEventEventResponse extends BaseEventResponse {
        public ExampleStruct indexedStruct;

        public ExampleStruct nonIndexedStruct;
    }
}
