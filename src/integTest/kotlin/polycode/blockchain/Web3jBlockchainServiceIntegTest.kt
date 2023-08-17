package polycode.blockchain

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Uint
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.properties.ChainSpec
import polycode.config.ApplicationProperties
import polycode.config.ChainProperties
import polycode.exception.BlockchainReadException
import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.BoolType
import polycode.features.contract.abi.model.DynamicArrayType
import polycode.features.contract.abi.model.StringType
import polycode.features.contract.abi.model.UintType
import polycode.features.contract.abi.service.EthereumAbiDecoderService
import polycode.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.params.OutputParameter
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.features.functions.encoding.service.EthereumFunctionEncoderService
import polycode.features.payout.model.params.GetPayoutsForInvestorParams
import polycode.features.payout.model.result.PayoutForInvestor
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleHash
import polycode.features.payout.util.PayoutAccountBalance
import polycode.model.DeserializableEvent
import polycode.model.DeserializableEventInput
import polycode.model.result.BlockchainTransactionInfo
import polycode.model.result.EventArgumentHash
import polycode.model.result.EventArgumentValue
import polycode.model.result.EventInfo
import polycode.model.result.FullContractDeploymentTransactionInfo
import polycode.service.CurrentUtcDateTimeProvider
import polycode.service.RandomUuidProvider
import polycode.testcontainers.HardhatTestContainer
import polycode.testcontainers.SharedTestContainers
import polycode.util.AccountBalance
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.EthStorageSlot
import polycode.util.FunctionData
import polycode.util.Keccak256Hash
import polycode.util.PredefinedEvents
import polycode.util.TransactionHash
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import polycode.util.ZeroAddress
import java.math.BigDecimal
import java.math.BigInteger

@Disabled("test only locally due to instability")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Web3jBlockchainServiceIntegTest : TestBase() {

    private val hardhatContainer = SharedTestContainers.hardhatContainer
    private val accounts = HardhatTestContainer.ACCOUNTS

    @BeforeEach
    fun beforeEach() {
        hardhatContainer.reset()
    }

    @Test
    fun mustCorrectlyReadStorageSlot() {
        val mainAccount = accounts[0]
        val proxyContractAddress = ContractAddress("abc123")

        val contract = suppose("some simple proxy contract is deployed") {
            DummyProxy.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                proxyContractAddress.rawValue
            ).send()
        }

        verify("storage slot is correctly read") {
            val service = createService()
            val slotValue = service.readStorageSlot(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                slot = EthStorageSlot("0x0")
            )

            expectThat(ContractAddress(slotValue))
                .isEqualTo(proxyContractAddress)
        }

        verify("zero is returned for empty storage slot") {
            val service = createService()
            val slotValue = service.readStorageSlot(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                slot = EthStorageSlot("0x123456")
            )

            expectThat(ContractAddress(slotValue))
                .isEqualTo(ZeroAddress.toContractAddress())
        }
    }

    @Test
    fun mustCorrectlyFetchAccountBalanceForLatestBlock() {
        val mainAccount = accounts[0]
        val value = Convert.toWei(BigDecimal.ONE, Convert.Unit.ETHER)
        val accountBalance = AccountBalance(
            wallet = WalletAddress("cafebabe"),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(value.toBigInteger())
        )

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accountBalance.wallet.rawValue,
                value,
                Convert.Unit.WEI
            ).send()
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct account balance is fetched for latest block") {
            val service = createService()
            service.fetchAccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                walletAddress = accountBalance.wallet
            )
            val fetchedAccountBalance = service.fetchAccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                walletAddress = accountBalance.wallet
            )

            expectThat(fetchedAccountBalance)
                .isEqualTo(
                    accountBalance.copy(
                        blockNumber = fetchedAccountBalance.blockNumber,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            expectThat(fetchedAccountBalance.blockNumber.value)
                .isPositive()
            expectThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAccountBalanceForSpecifiedBlockNumber() {
        val mainAccount = accounts[0]
        val value = Convert.toWei(BigDecimal.ONE, Convert.Unit.ETHER)
        val accountBalance = AccountBalance(
            wallet = WalletAddress("cafebabe"),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(value.toBigInteger())
        )

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accountBalance.wallet.rawValue,
                value,
                Convert.Unit.WEI
            ).send()
            hardhatContainer.mine()
        }

        val blockNumberBeforeSendingBalance = hardhatContainer.blockNumber()

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accountBalance.wallet.rawValue,
                value,
                Convert.Unit.WEI
            ).send()
            hardhatContainer.mine()
        }

        val service = createService()

        verify("correct ETH balance is fetched for block number before ETH transfer is made") {
            val fetchedAccountBalance = service.fetchAccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberBeforeSendingBalance
            )

            expectThat(fetchedAccountBalance)
                .isEqualTo(
                    accountBalance.copy(
                        blockNumber = blockNumberBeforeSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            expectThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        val blockNumberAfterSendingBalance = hardhatContainer.blockNumber()

        verify("correct ETH balance is fetched for block number after ETH transfer is made") {
            val fetchedAccountBalance = service.fetchAccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberAfterSendingBalance
            )

            expectThat(fetchedAccountBalance)
                .isEqualTo(
                    accountBalance.copy(
                        amount = Balance(value.toBigInteger().multiply(BigInteger.TWO)),
                        blockNumber = blockNumberAfterSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            expectThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceForLatestBlock() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        verify("correct ERC20 balance is fetched for latest block") {
            val service = createService()
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.wallet
            )

            expectThat(fetchedAccountBalance)
                .isEqualTo(
                    accountBalance.copy(
                        blockNumber = fetchedAccountBalance.blockNumber,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            expectThat(fetchedAccountBalance.blockNumber.value)
                .isPositive()
            expectThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchErc20BalanceForSpecifiedBlockNumber() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        val blockNumberBeforeSendingBalance = hardhatContainer.blockNumber()

        suppose("some ERC20 transaction is made") {
            contract.transfer(accounts[1].address, accountBalance.amount.rawValue).send()
        }

        val service = createService()

        verify("correct ERC20 balance is fetched for block number before ERC20 transfer is made") {
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberBeforeSendingBalance
            )

            expectThat(fetchedAccountBalance)
                .isEqualTo(
                    accountBalance.copy(
                        blockNumber = blockNumberBeforeSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            expectThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        val blockNumberAfterSendingBalance = hardhatContainer.blockNumber()

        verify("correct ERC20 balance is fetched for block number after ERC20 transfer is made") {
            val fetchedAccountBalance = service.fetchErc20AccountBalance(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                walletAddress = accountBalance.wallet,
                blockParameter = blockNumberAfterSendingBalance
            )

            expectThat(fetchedAccountBalance)
                .isEqualTo(
                    accountBalance.copy(
                        amount = Balance.ZERO,
                        blockNumber = blockNumberAfterSendingBalance,
                        timestamp = fetchedAccountBalance.timestamp
                    )
                )
            expectThat(fetchedAccountBalance.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustThrowBlockchainReadExceptionWhenReadingErc20BalanceFromInvalidErc20ContractAddress() {
        verify("BlockchainReadException is thrown when reading ERC20 balance from invalid contract address") {
            val service = createService()

            expectThrows<BlockchainReadException> {
                service.fetchErc20AccountBalance(
                    chainSpec = TestData.CHAIN_ID.toSpec(),
                    contractAddress = ContractAddress(accounts[0].address),
                    walletAddress = WalletAddress(accounts[0].address)
                )
            }
        }
    }

    @Test
    fun mustCorrectlyFetchContractTransactionInfoForSuccessfulTransaction() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        val txHash = suppose("some ERC20 transfer transaction is made") {
            contract.transfer(accounts[1].address, accountBalance.amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct transaction info is fetched") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = listOf(PredefinedEvents.ERC20_TRANSFER)
            )

            expectThat(transactionInfo)
                .isNotNull()

            expectThat(transactionInfo!!)
                .isEqualTo(
                    BlockchainTransactionInfo(
                        hash = txHash,
                        from = WalletAddress(mainAccount.address),
                        to = WalletAddress(contract.contractAddress),
                        deployedContractAddress = null,
                        data = transactionInfo.data,
                        value = Balance.ZERO,
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = true,
                        events = listOf(
                            EventInfo(
                                signature = "Transfer(address,address,uint256)",
                                arguments = listOf(
                                    EventArgumentValue("from", mainAccount.address),
                                    EventArgumentValue("to", accounts[1].address),
                                    EventArgumentValue("value", accountBalance.amount.rawValue)
                                )
                            )
                        )
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(WalletAddress(accounts[1].address)),
                    FunctionArgument(accountBalance.amount)
                )
            )

            expectThat(transactionInfo.data)
                .isEqualTo(expectedData)

            expectThat(transactionInfo.blockConfirmations)
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyFetchContractTransactionInfoForFailedTransaction() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )
        val sendAmount = Balance(accountBalance.amount.rawValue * BigInteger.TEN)

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        val txHash = suppose("some ERC20 transfer transaction is made") {
            try {
                contract.transfer(accounts[1].address, sendAmount.rawValue).send()
                    ?.transactionHash?.let { TransactionHash(it) }!!
            } catch (e: TransactionException) {
                // web3j is really something...
                e.message?.removePrefix("{\"txHash\":\"")?.removeSuffix("\"}")
                    ?.let { TransactionHash(it) }!!
            }
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct transaction info is fetched") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = emptyList()
            )

            expectThat(transactionInfo)
                .isNotNull()

            expectThat(transactionInfo!!)
                .isEqualTo(
                    BlockchainTransactionInfo(
                        hash = txHash,
                        from = WalletAddress(mainAccount.address),
                        to = WalletAddress(contract.contractAddress),
                        deployedContractAddress = null,
                        data = transactionInfo.data,
                        value = Balance.ZERO,
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = false,
                        events = emptyList()
                    )
                )

            val expectedData = EthereumFunctionEncoderService().encode(
                functionName = "transfer",
                arguments = listOf(
                    FunctionArgument(WalletAddress(accounts[1].address)),
                    FunctionArgument(sendAmount)
                )
            )

            expectThat(transactionInfo.data)
                .isEqualTo(expectedData)

            expectThat(transactionInfo.blockConfirmations)
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyFetchNonContractTransactionInfo() {
        val mainAccount = accounts[0]
        val value = Convert.toWei(BigDecimal.ONE, Convert.Unit.ETHER)
        val txHash = suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                accounts[1].address,
                value,
                Convert.Unit.WEI
            ).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("correct transaction info is fetched") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = emptyList()
            )

            expectThat(transactionInfo)
                .isNotNull()

            expectThat(transactionInfo!!)
                .isEqualTo(
                    BlockchainTransactionInfo(
                        hash = txHash,
                        from = WalletAddress(mainAccount.address),
                        to = WalletAddress(accounts[1].address),
                        deployedContractAddress = null,
                        data = FunctionData("0x"),
                        value = Balance(value.toBigInteger()),
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = true,
                        events = emptyList()
                    )
                )

            expectThat(transactionInfo.blockConfirmations)
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyFetchContractDeploymentTransactionInfo() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        val encodedConstructor = EthereumFunctionEncoderService().encodeConstructor(
            arguments = listOf(
                FunctionArgument(DynamicArray(Address::class.java, listOf(accountBalance.wallet.value))),
                FunctionArgument(DynamicArray(Uint::class.java, listOf(accountBalance.amount.value))),
                FunctionArgument(WalletAddress(mainAccount.address))
            )
        )
        val data = "0x" + SimpleERC20.BINARY + encodedConstructor.withoutPrefix

        verify("correct transaction info is fetched") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = emptyList()
            )

            expectThat(transactionInfo)
                .isNotNull()

            expectThat(transactionInfo!!)
                .isEqualTo(
                    BlockchainTransactionInfo(
                        hash = txHash,
                        from = WalletAddress(mainAccount.address),
                        to = ZeroAddress.toWalletAddress(),
                        deployedContractAddress = ContractAddress(contract.contractAddress),
                        data = FunctionData(data),
                        value = Balance.ZERO,
                        blockConfirmations = transactionInfo.blockConfirmations,
                        timestamp = transactionInfo.timestamp,
                        success = true,
                        events = emptyList()
                    )
                )

            expectThat(transactionInfo.blockConfirmations)
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyReadWellDefinedEventsFromContractDeploymentTransaction() {
        val mainAccount = accounts[0]
        val owner = WalletAddress("cafebabe")
        val message = "message"
        val amount = Balance(BigInteger.valueOf(123L))
        val value = Balance(BigInteger.valueOf(456L))
        val example = true

        val contract = suppose("event emitter contract is deployed") {
            EventEmitter.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                owner.rawValue,
                message,
                amount.rawValue,
                value.rawValue,
                example
            ).send()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("transaction info events are correctly decoded") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = listOf(
                    DeserializableEvent(
                        signature = "ExampleEvent(address,string,uint256,uint256,bool)",
                        inputsOrder = listOf("owner", "message", "amount", "value", "example"),
                        indexedInputs = listOf(
                            DeserializableEventInput("owner", AddressType),
                            DeserializableEventInput("message", StringType),
                            DeserializableEventInput("value", UintType)
                        ),
                        regularInputs = listOf(
                            DeserializableEventInput("amount", UintType),
                            DeserializableEventInput("example", BoolType)
                        )
                    ),
                    DeserializableEvent(
                        signature = "AnonymousEvent(address,string)",
                        inputsOrder = listOf("owner", "message"),
                        indexedInputs = listOf(
                            DeserializableEventInput("owner", AddressType)
                        ),
                        regularInputs = listOf(
                            DeserializableEventInput("message", StringType)
                        )
                    )
                )
            )!!

            expectThat(transactionInfo.events)
                .isEqualTo(
                    listOf(
                        EventInfo(
                            signature = "ExampleEvent(address,string,uint256,uint256,bool)",
                            arguments = listOf(
                                EventArgumentValue("owner", owner.rawValue),
                                EventArgumentHash("message", "0x${Keccak256Hash(message).value}"),
                                EventArgumentValue("amount", amount.rawValue),
                                EventArgumentValue("value", value.rawValue),
                                EventArgumentValue("example", example)
                            )
                        ),
                        EventInfo(
                            signature = "AnonymousEvent(address,string)",
                            arguments = listOf(
                                EventArgumentValue("owner", owner.rawValue),
                                EventArgumentValue("message", message)
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReadEventsWhenNoneAreSpecifiedFromContractDeploymentTransaction() {
        val mainAccount = accounts[0]
        val owner = WalletAddress("cafebabe")
        val message = "message"
        val amount = Balance(BigInteger.valueOf(123L))
        val value = Balance(BigInteger.valueOf(456L))
        val example = true

        val contract = suppose("event emitter contract is deployed") {
            EventEmitter.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                owner.rawValue,
                message,
                amount.rawValue,
                value.rawValue,
                example
            ).send()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("transaction info events are correctly decoded") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = emptyList()
            )!!

            expectThat(transactionInfo.events)
                .isEqualTo(
                    listOf(
                        EventInfo(
                            signature = null,
                            arguments = listOf(
                                EventArgumentValue(
                                    name = "arg0",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 123
                                    )
                                ),
                                EventArgumentValue(
                                    name = "arg1",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 1
                                    )
                                ),
                                EventArgumentHash(
                                    name = "arg2",
                                    hash = "0x465bc6257dcfc79569508750afdabc2aa7d499dd8de935aa5324446084c99397"
                                ),
                                EventArgumentHash(
                                    name = "arg3",
                                    hash = "0x00000000000000000000000000000000000000000000000000000000cafebabe"
                                ),
                                EventArgumentHash(
                                    name = "arg4",
                                    hash = "0xc2baf6c66618acd49fb133cebc22f55bd907fe9f0d69a726d45b7539ba6bbe08"
                                ),
                                EventArgumentHash(
                                    name = "arg5",
                                    hash = "0x00000000000000000000000000000000000000000000000000000000000001c8"
                                )
                            )
                        ),
                        EventInfo(
                            signature = null,
                            arguments = listOf(
                                EventArgumentValue(
                                    name = "arg0",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 32
                                    )
                                ),
                                EventArgumentValue(
                                    name = "arg1",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 7
                                    )
                                ),
                                EventArgumentValue(
                                    name = "arg2",
                                    value = byteList(
                                        109, 101, 115, 115, 97, 103, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                                    )
                                ),
                                EventArgumentHash(
                                    name = "arg3",
                                    hash = "0x00000000000000000000000000000000000000000000000000000000cafebabe"
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyReadEventsWhenMalformedEventsAreSpecifiedFromContractDeploymentTransaction() {
        val mainAccount = accounts[0]
        val owner = WalletAddress("cafebabe")
        val message = "message"
        val amount = Balance(BigInteger.valueOf(123L))
        val value = Balance(BigInteger.valueOf(456L))
        val example = true

        val contract = suppose("event emitter contract is deployed") {
            EventEmitter.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                owner.rawValue,
                message,
                amount.rawValue,
                value.rawValue,
                example
            ).send()
        }

        val txHash = TransactionHash(contract.transactionReceipt.get().transactionHash)

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        verify("transaction info events are correctly decoded") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = txHash,
                events = listOf(
                    DeserializableEvent(
                        signature = "ExampleEvent(address,string,uint256,uint256,bool)",
                        inputsOrder = listOf("owner", "message", "amount", "value", "example"),
                        indexedInputs = listOf(
                            DeserializableEventInput("owner", StringType),
                            DeserializableEventInput("message", StringType),
                            DeserializableEventInput("value", StringType)
                        ),
                        regularInputs = listOf(
                            DeserializableEventInput("amount", DynamicArrayType(StringType)),
                            DeserializableEventInput("example", DynamicArrayType(StringType))
                        )
                    ),
                    DeserializableEvent(
                        signature = "AnonymousEvent(address,string)",
                        inputsOrder = listOf("owner", "message"),
                        indexedInputs = listOf(
                            DeserializableEventInput("owner", StringType)
                        ),
                        regularInputs = listOf(
                            DeserializableEventInput("message", DynamicArrayType(StringType))
                        )
                    )
                )
            )!!

            expectThat(transactionInfo.events)
                .isEqualTo(
                    listOf(
                        EventInfo(
                            signature = null,
                            arguments = listOf(
                                EventArgumentValue(
                                    name = "arg0",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 123
                                    )
                                ),
                                EventArgumentValue(
                                    name = "arg1",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 1
                                    )
                                ),
                                EventArgumentHash(
                                    name = "arg2",
                                    hash = "0x465bc6257dcfc79569508750afdabc2aa7d499dd8de935aa5324446084c99397"
                                ),
                                EventArgumentHash(
                                    name = "arg3",
                                    hash = "0x00000000000000000000000000000000000000000000000000000000cafebabe"
                                ),
                                EventArgumentHash(
                                    name = "arg4",
                                    hash = "0xc2baf6c66618acd49fb133cebc22f55bd907fe9f0d69a726d45b7539ba6bbe08"
                                ),
                                EventArgumentHash(
                                    name = "arg5",
                                    hash = "0x00000000000000000000000000000000000000000000000000000000000001c8"
                                )
                            )
                        ),
                        EventInfo(
                            signature = null,
                            arguments = listOf(
                                EventArgumentValue(
                                    name = "arg0",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 32
                                    )
                                ),
                                EventArgumentValue(
                                    name = "arg1",
                                    value = byteList(
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 7
                                    )
                                ),
                                EventArgumentValue(
                                    name = "arg2",
                                    value = byteList(
                                        109, 101, 115, 115, 97, 103, 101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0
                                    )
                                ),
                                EventArgumentHash(
                                    name = "arg3",
                                    hash = "0x00000000000000000000000000000000000000000000000000000000cafebabe"
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentTransactionInfo() {
        verify("null is returned for non existent transaction") {
            val service = createService()
            val transactionInfo = service.fetchTransactionInfo(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                txHash = TransactionHash("0x123456"),
                events = emptyList()
            )

            expectThat(transactionInfo)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyCallReadonlyFunction() {
        val mainAccount = accounts[0]

        val contract = suppose("readonly function calls contract is deployed") {
            ReadonlyFunctionCallsContract.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val functionName = "returningUint"
        val uintValue = Uint256(BigInteger.TEN)
        val functionData = EthereumFunctionEncoderService().encode(
            functionName = functionName,
            arguments = listOf(FunctionArgument(uintValue))
        )

        verify("correct value is returned for latest block") {
            val service = createService()
            val result = service.callReadonlyFunction(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                params = ExecuteReadonlyFunctionCallParams(
                    contractAddress = ContractAddress(contract.contractAddress),
                    callerAddress = WalletAddress("a"),
                    functionName = functionName,
                    functionData = functionData,
                    outputParams = listOf(OutputParameter(UintType))
                )
            )

            expectThat(result)
                .isEqualTo(
                    ReadonlyFunctionCallResult(
                        blockNumber = result.blockNumber,
                        timestamp = result.timestamp,
                        returnValues = listOf(uintValue.value),
                        rawReturnValue = EthereumFunctionEncoderService()
                            .encodeConstructor(listOf(FunctionArgument(uintValue))).value
                    )
                )
            expectThat(result.blockNumber.value)
                .isPositive()
            expectThat(result.timestamp.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustThrowBlockchainReadExceptionWhenCallingReadonlyFunctionOnInvalidContractAddress() {
        val functionName = "returningUint"
        val uintValue = Uint256(BigInteger.TEN)
        val functionData = EthereumFunctionEncoderService().encode(
            functionName = functionName,
            arguments = listOf(FunctionArgument(uintValue))
        )

        verify("BlockchainReadException is thrown when calling readonly function on invalid contract address") {
            val service = createService()

            expectThrows<BlockchainReadException> {
                service.callReadonlyFunction(
                    chainSpec = TestData.CHAIN_ID.toSpec(),
                    params = ExecuteReadonlyFunctionCallParams(
                        contractAddress = ContractAddress("dead"),
                        callerAddress = WalletAddress("a"),
                        functionName = functionName,
                        functionData = functionData,
                        outputParams = listOf(OutputParameter(UintType))
                    )
                )
            }
        }
    }

    @Test
    fun mustCorrectlyFindContractDeploymentTransaction() {
        val mainAccount = accounts[0]
        val accountBalance = AccountBalance(
            wallet = WalletAddress(mainAccount.address),
            blockNumber = BlockNumber(BigInteger.ZERO),
            timestamp = UtcDateTime.ofEpochSeconds(0L),
            amount = Balance(BigInteger("10000"))
        )

        suppose("1000 blocks will be mined before contract deployment transaction") {
            repeat(1_000) { hardhatContainer.mine() }
        }

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(accountBalance.wallet.rawValue),
                listOf(accountBalance.amount.rawValue),
                mainAccount.address
            ).send()
        }

        hardhatContainer.mine()

        val deploymentTransaction = contract.transactionReceipt.get()

        suppose("1000 blocks will be mined after contract deployment transaction") {
            repeat(1_000) { hardhatContainer.mine() }
        }

        val encodedConstructor = EthereumFunctionEncoderService().encodeConstructor(
            arguments = listOf(
                FunctionArgument(DynamicArray(Address::class.java, listOf(accountBalance.wallet.value))),
                FunctionArgument(DynamicArray(Uint::class.java, listOf(accountBalance.amount.value))),
                FunctionArgument(WalletAddress(mainAccount.address))
            )
        )
        val data = "0x" + SimpleERC20.BINARY + encodedConstructor.withoutPrefix
        val rawBinaryIndex = SimpleERC20.BINARY.lastIndexOf("608060")

        verify("contract deployment transaction is correctly found") {
            val service = createService()
            val transactionInfo = service.findContractDeploymentTransaction(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                contractAddress = ContractAddress(contract.contractAddress),
                events = emptyList()
            )

            expectThat(transactionInfo)
                .isNotNull()

            expectThat(transactionInfo!!)
                .isEqualTo(
                    FullContractDeploymentTransactionInfo(
                        hash = TransactionHash(deploymentTransaction.transactionHash),
                        from = WalletAddress(mainAccount.address),
                        deployedContractAddress = ContractAddress(contract.contractAddress),
                        data = FunctionData(data),
                        value = Balance.ZERO,
                        binary = ContractBinaryData(SimpleERC20.BINARY.substring(rawBinaryIndex)),
                        blockNumber = BlockNumber(deploymentTransaction.blockNumber),
                        events = emptyList()
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchBalancesBasedOnBlockRange() {
        val mainAccount = accounts[0]

        suppose("1000 blocks will be mined before contract deployment transaction") {
            repeat(1_000) { hardhatContainer.mine() }
        }

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        suppose("1000 blocks will be mined after contract deployment transaction") {
            repeat(1_000) { hardhatContainer.mine() }
        }

        suppose("some accounts get ERC20 tokens") {
            contract.transfer(accounts[1].address, BigInteger("100")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[2].address, BigInteger("200")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[3].address, BigInteger("300")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[4].address, BigInteger("400")).send()
            hardhatContainer.mine()
        }

        val startBlock = BlockNumber(BigInteger.ZERO)
        val endBlock1 = hardhatContainer.blockNumber()

        contract.applyWeb3jFilterFix(startBlock, endBlock1)

        suppose("some additional transactions of ERC20 token are made") {
            contract.transfer(accounts[1].address, BigInteger("900")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[5].address, BigInteger("1000")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[6].address, BigInteger("2000")).send()
            hardhatContainer.mine()
        }

        verify("correct balances are fetched for first end block") {
            val service = createService()
            val balances = service.fetchErc20AccountBalances(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                erc20ContractAddress = ContractAddress(contract.contractAddress),
                ignoredErc20Addresses = emptySet(),
                startBlock = startBlock,
                endBlock = endBlock1
            )

            expectThat(balances).containsExactlyInAnyOrder(
                PayoutAccountBalance(WalletAddress(mainAccount.address), Balance(BigInteger("9000"))),
                PayoutAccountBalance(WalletAddress(accounts[1].address), Balance(BigInteger("100"))),
                PayoutAccountBalance(WalletAddress(accounts[2].address), Balance(BigInteger("200"))),
                PayoutAccountBalance(WalletAddress(accounts[3].address), Balance(BigInteger("300"))),
                PayoutAccountBalance(WalletAddress(accounts[4].address), Balance(BigInteger("400")))
            )
        }

        val endBlock2 = hardhatContainer.blockNumber()

        verify("correct balances are fetched for second end block") {
            val service = createService()
            val balances = service.fetchErc20AccountBalances(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                erc20ContractAddress = ContractAddress(contract.contractAddress),
                ignoredErc20Addresses = emptySet(),
                startBlock = startBlock,
                endBlock = endBlock2
            )

            expectThat(balances).containsExactlyInAnyOrder(
                PayoutAccountBalance(WalletAddress(mainAccount.address), Balance(BigInteger("5100"))),
                PayoutAccountBalance(WalletAddress(accounts[1].address), Balance(BigInteger("1000"))),
                PayoutAccountBalance(WalletAddress(accounts[2].address), Balance(BigInteger("200"))),
                PayoutAccountBalance(WalletAddress(accounts[3].address), Balance(BigInteger("300"))),
                PayoutAccountBalance(WalletAddress(accounts[4].address), Balance(BigInteger("400"))),
                PayoutAccountBalance(WalletAddress(accounts[5].address), Balance(BigInteger("1000"))),
                PayoutAccountBalance(WalletAddress(accounts[6].address), Balance(BigInteger("2000")))
            )
        }
    }

    @Test
    fun mustCorrectlyFetchBalancesBasedOnBlockRangeWhenSomeAddressesAreIgnored() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        hardhatContainer.mine()

        suppose("some accounts get ERC20 tokens") {
            contract.transfer(accounts[1].address, BigInteger("100")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[2].address, BigInteger("200")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[3].address, BigInteger("300")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[4].address, BigInteger("400")).send()
            hardhatContainer.mine()
        }

        val startBlock = BlockNumber(BigInteger.ZERO)
        val endBlock = hardhatContainer.blockNumber()

        contract.applyWeb3jFilterFix(startBlock, endBlock)

        suppose("some additional transactions of ERC20 token are made") {
            contract.transfer(accounts[1].address, BigInteger("900")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[5].address, BigInteger("1000")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[6].address, BigInteger("2000")).send()
            hardhatContainer.mine()
        }

        val ignoredAddresses = setOf(
            WalletAddress(mainAccount.address),
            WalletAddress(accounts[1].address),
            WalletAddress(accounts[3].address)
        )

        verify("correct balances are fetched") {
            val service = createService()
            val balances = service.fetchErc20AccountBalances(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                erc20ContractAddress = ContractAddress(contract.contractAddress),
                ignoredErc20Addresses = ignoredAddresses,
                startBlock = startBlock,
                endBlock = endBlock
            )

            expectThat(balances).containsExactlyInAnyOrder(
                PayoutAccountBalance(WalletAddress(accounts[2].address), Balance(BigInteger("200"))),
                PayoutAccountBalance(WalletAddress(accounts[4].address), Balance(BigInteger("400")))
            )
        }
    }

    @Test
    fun mustCorrectlyFetchBalancesForNullStartBlock() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        hardhatContainer.mine()

        suppose("some accounts get ERC20 tokens") {
            contract.transfer(accounts[1].address, BigInteger("100")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[2].address, BigInteger("200")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[3].address, BigInteger("300")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[4].address, BigInteger("400")).send()
            hardhatContainer.mine()
        }

        val startBlock = null
        val endBlock1 = hardhatContainer.blockNumber()

        contract.applyWeb3jFilterFix(startBlock, endBlock1)

        suppose("some additional transactions of ERC20 token are made") {
            contract.transfer(accounts[1].address, BigInteger("900")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[5].address, BigInteger("1000")).send()
            hardhatContainer.mine()
            contract.transfer(accounts[6].address, BigInteger("2000")).send()
            hardhatContainer.mine()
        }

        verify("correct balances are fetched for first end block") {
            val service = createService()
            val balances = service.fetchErc20AccountBalances(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                erc20ContractAddress = ContractAddress(contract.contractAddress),
                ignoredErc20Addresses = emptySet(),
                startBlock = startBlock,
                endBlock = endBlock1
            )

            expectThat(balances).containsExactlyInAnyOrder(
                PayoutAccountBalance(WalletAddress(mainAccount.address), Balance(BigInteger("9000"))),
                PayoutAccountBalance(WalletAddress(accounts[1].address), Balance(BigInteger("100"))),
                PayoutAccountBalance(WalletAddress(accounts[2].address), Balance(BigInteger("200"))),
                PayoutAccountBalance(WalletAddress(accounts[3].address), Balance(BigInteger("300"))),
                PayoutAccountBalance(WalletAddress(accounts[4].address), Balance(BigInteger("400")))
            )
        }

        val endBlock2 = hardhatContainer.blockNumber()

        verify("correct balances are fetched for second end block") {
            val service = createService()
            val balances = service.fetchErc20AccountBalances(
                chainSpec = TestData.CHAIN_ID.toSpec(),
                erc20ContractAddress = ContractAddress(contract.contractAddress),
                ignoredErc20Addresses = emptySet(),
                startBlock = startBlock,
                endBlock = endBlock2
            )

            expectThat(balances).containsExactlyInAnyOrder(
                PayoutAccountBalance(WalletAddress(mainAccount.address), Balance(BigInteger("5100"))),
                PayoutAccountBalance(WalletAddress(accounts[1].address), Balance(BigInteger("1000"))),
                PayoutAccountBalance(WalletAddress(accounts[2].address), Balance(BigInteger("200"))),
                PayoutAccountBalance(WalletAddress(accounts[3].address), Balance(BigInteger("300"))),
                PayoutAccountBalance(WalletAddress(accounts[4].address), Balance(BigInteger("400"))),
                PayoutAccountBalance(WalletAddress(accounts[5].address), Balance(BigInteger("1000"))),
                PayoutAccountBalance(WalletAddress(accounts[6].address), Balance(BigInteger("2000")))
            )
        }
    }

    @Test
    fun mustCorrectlyFetchPayoutsForInvestor() {
        val mainAccount = accounts[0]
        val hash = HashFunction.KECCAK_256.invoke("test")
        val owner1 = WalletAddress("aaa1")
        val owner2 = WalletAddress("aaa2")
        val owner3 = WalletAddress("aaa3")
        val investor1 = WalletAddress("bbb1")
        val investor2 = WalletAddress("bbb2")
        val payoutsAndInvestments = listOf(
            createPayoutWithInvestor(id = 0, owner = owner1, asset = "a", hash = hash, investor = investor1),
            createPayoutWithInvestor(id = 1, owner = owner1, asset = "a", hash = hash, investor = investor1),
            createPayoutWithInvestor(id = 2, owner = owner2, asset = "b", hash = hash, investor = investor2),
            createPayoutWithInvestor(id = 3, owner = owner2, asset = "c", hash = hash, investor = investor2),
            createPayoutWithInvestor(id = 4, owner = owner3, asset = "d", hash = hash, investor = investor1)
        )

        val payouts = payoutsAndInvestments.map { it.first }

        val manager = suppose("simple payout manager contract is deployed") {
            SimplePayoutManager.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                payouts
            ).send()
        }

        hardhatContainer.mine()

        suppose("some investments are claimed") {
            payoutsAndInvestments.forEach {
                manager.setClaim(it.second.payoutId, it.second.investor, it.second.amountClaimed).send()
                hardhatContainer.mine()
            }
        }

        val investor1NullParams = GetPayoutsForInvestorParams(
            payoutManager = ContractAddress(manager.contractAddress),
            investor = investor1
        )
        val investor2NullParams = investor1NullParams.copy(investor = investor2)
        val blockchainService = createService()
        val chainSpec = TestData.CHAIN_ID.toSpec()

        verify("all payouts states are fetched") {
            // investor 1
            expectThat(blockchainService.getPayoutsForInvestor(chainSpec, investor1NullParams))
                .containsExactlyInAnyOrderElementsOf(payoutsAndInvestments.forInvestor(investor1))

            // investor 2
            expectThat(blockchainService.getPayoutsForInvestor(chainSpec, investor2NullParams))
                .containsExactlyInAnyOrderElementsOf(payoutsAndInvestments.forInvestor(investor2))
        }
    }

    @Test
    fun mustThrowBlockchainReadExceptionWhenFetchingPayoutsForInvestorFails() {
        val nullParams = GetPayoutsForInvestorParams(
            payoutManager = ContractAddress("0"),
            investor = WalletAddress("1")
        )
        val blockchainService = createService()
        val chainSpec = TestData.CHAIN_ID.toSpec()

        verify("exception is thrown") {
            expectThrows<BlockchainReadException> {
                blockchainService.getPayoutsForInvestor(chainSpec, nullParams)
            }
        }
    }

    private fun hardhatProperties() = ApplicationProperties().apply {
        chain = mapOf(
            TestData.CHAIN_ID to ChainProperties(
                name = "HARDHAT_TESTNET",
                rpcUrl = "http://localhost:${hardhatContainer.mappedPort}",
                infuraUrl = null,
                startBlockNumber = null,
                chainExplorerApiUrl = null,
                chainExplorerApiKey = null,
                minBlockConfirmationsForCaching = null
            )
        )
    }

    private fun ChainId.toSpec() = ChainSpec(this, null)

    private fun createService() =
        Web3jBlockchainService(
            abiDecoderService = EthereumAbiDecoderService(),
            uuidProvider = RandomUuidProvider(),
            utcDateTimeProvider = CurrentUtcDateTimeProvider(),
            web3jBlockchainServiceCacheRepository = mock(),
            applicationProperties = hardhatProperties()
        )

    // This is needed to make web3j work correctly with Hardhat until https://github.com/web3j/web3j/pull/1580 is merged
    private fun SimpleERC20.applyWeb3jFilterFix(startBlock: BlockNumber?, endBlock: BlockNumber) {
        val startBlockParameter =
            startBlock?.value?.let(DefaultBlockParameter::valueOf) ?: DefaultBlockParameterName.EARLIEST
        val endBlockParameter = DefaultBlockParameter.valueOf(endBlock.value)

        repeat(15) {
            hardhatContainer.web3j.ethNewFilter(
                EthFilter(startBlockParameter, endBlockParameter, contractAddress)
            ).send()
            hardhatContainer.mine()
        }
    }

    private fun createPayout(id: Long, owner: WalletAddress, asset: String, hash: MerkleHash): PayoutStruct =
        PayoutStruct(
            BigInteger.valueOf(id),
            owner.rawValue,
            "payout-info-$id",
            false,
            ContractAddress(asset).rawValue,
            BigInteger.valueOf(id * 1_000L),
            emptyList(),
            Numeric.hexStringToByteArray(hash.value),
            BigInteger.valueOf(id),
            BigInteger.valueOf(id + 1),
            "ipfs-hash-$id",
            ContractAddress("ffff").rawValue,
            BigInteger.valueOf(id * 500L),
            BigInteger.valueOf(id * 500L)
        )

    private fun createPayoutWithInvestor(
        id: Long,
        owner: WalletAddress,
        asset: String,
        hash: MerkleHash,
        investor: WalletAddress
    ): Pair<PayoutStruct, PayoutStateForInvestor> =
        Pair(
            createPayout(id = id, owner = owner, asset = asset, hash = hash),
            PayoutStateForInvestor(
                BigInteger.valueOf(id),
                investor.rawValue,
                BigInteger.valueOf(id * 31L)
            )
        )

    private fun List<Pair<PayoutStruct, PayoutStateForInvestor>>.forInvestor(
        investor: WalletAddress
    ): List<PayoutForInvestor> =
        map {
            val state = it.second

            if (WalletAddress(state.investor) == investor) {
                PayoutForInvestor(it.first, it.second)
            } else {
                PayoutForInvestor(it.first, PayoutStateForInvestor(state.payoutId, investor.rawValue, BigInteger.ZERO))
            }
        }

    private fun byteList(vararg bytes: Byte): List<Byte> = bytes.toList()
}
