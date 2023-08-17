package polycode.blockchain

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import polycode.TestBase
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleHash
import polycode.features.payout.util.MerkleTree
import polycode.features.payout.util.PayoutAccountBalance
import polycode.testcontainers.HardhatTestContainer
import polycode.testcontainers.SharedTestContainers
import polycode.util.Balance
import polycode.util.WalletAddress
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MerkleTreePathValidatorHashCompatibilityIntegTest : TestBase() {

    private val hardhatContainer = SharedTestContainers.hardhatContainer
    private lateinit var contract: MerkleTreePathValidator

    @BeforeAll
    fun beforeAll() {
        hardhatContainer.reset()

        contract = suppose("Merkle tree path validator is deployed") {
            val future = MerkleTreePathValidator.deploy(
                hardhatContainer.web3j,
                HardhatTestContainer.ACCOUNTS[0],
                DefaultGasProvider()
            ).sendAsync()
            hardhatContainer.mine()
            future.get()
        }
    }

    @Test
    fun mustBeCompatibleWithMerkleTreeWithSingleElement() {
        val accountBalance = PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0")))
        val tree = suppose("Merkle tree with single element is created") {
            MerkleTree(
                listOf(accountBalance),
                HashFunction.KECCAK_256
            )
        }

        verify("contract call will return true for account balance contained in Merkle tree") {
            expectThat(
                contract.containsNode(
                    tree.root.hash.asEthByteArray(),
                    BigInteger.valueOf(tree.root.depth.toLong()),
                    accountBalance.address.rawValue,
                    accountBalance.balance.rawValue,
                    accountBalance.proof(tree)
                ).send()
            ).isTrue()
        }

        verify("contract call will return false for account balance not contained in Merkle tree") {
            expectThat(
                contract.containsNode(
                    tree.root.hash.asEthByteArray(),
                    BigInteger.valueOf(tree.root.depth.toLong()),
                    accountBalance.address.rawValue,
                    accountBalance.balance.rawValue + BigInteger.ONE,
                    accountBalance.proof(tree)
                ).send()
            ).isFalse()
        }
    }

    @Test
    fun mustBeCompatibleWithMerkleTreeWithTwoElements() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1")))
        )
        val tree = suppose("Merkle tree with two elements is created") {
            MerkleTree(
                accountBalances,
                HashFunction.KECCAK_256
            )
        }

        contract.verifyContractCalls(accountBalances, tree)
    }

    @Test
    fun mustBeCompatibleWithMerkleTreeWithThreeElements() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1"))),
            PayoutAccountBalance(WalletAddress("0x2"), Balance(BigInteger("2")))
        )
        val tree = suppose("Merkle tree with three elements is created") {
            MerkleTree(
                accountBalances,
                HashFunction.KECCAK_256
            )
        }

        contract.verifyContractCalls(accountBalances, tree)
    }

    @Test
    fun mustBeCompatibleWithMerkleTreeWithFourElements() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1"))),
            PayoutAccountBalance(WalletAddress("0x2"), Balance(BigInteger("2"))),
            PayoutAccountBalance(WalletAddress("0x3"), Balance(BigInteger("3")))
        )
        val tree = suppose("Merkle tree with four elements is created") {
            MerkleTree(
                accountBalances,
                HashFunction.KECCAK_256
            )
        }

        contract.verifyContractCalls(accountBalances, tree)
    }

    @Test
    fun mustBeCompatibleWithBalancedMerkleTree() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1"))),
            PayoutAccountBalance(WalletAddress("0x2"), Balance(BigInteger("2"))),
            PayoutAccountBalance(WalletAddress("0x3"), Balance(BigInteger("3"))),
            PayoutAccountBalance(WalletAddress("0x4"), Balance(BigInteger("4"))),
            PayoutAccountBalance(WalletAddress("0x5"), Balance(BigInteger("5"))),
            PayoutAccountBalance(WalletAddress("0x6"), Balance(BigInteger("6"))),
            PayoutAccountBalance(WalletAddress("0x7"), Balance(BigInteger("7")))
        )
        val tree = suppose("Merkle tree with 8 elements is created") {
            MerkleTree(
                accountBalances,
                HashFunction.KECCAK_256
            )
        }

        contract.verifyContractCalls(accountBalances, tree)
    }

    @Test
    fun mustBeCompatibleWithUnbalancedMerkleTreeWithEvenNumberOfElements() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1"))),
            PayoutAccountBalance(WalletAddress("0x2"), Balance(BigInteger("2"))),
            PayoutAccountBalance(WalletAddress("0x3"), Balance(BigInteger("3"))),
            PayoutAccountBalance(WalletAddress("0x4"), Balance(BigInteger("4"))),
            PayoutAccountBalance(WalletAddress("0x5"), Balance(BigInteger("5"))),
            PayoutAccountBalance(WalletAddress("0x6"), Balance(BigInteger("6"))),
            PayoutAccountBalance(WalletAddress("0x7"), Balance(BigInteger("7"))),
            PayoutAccountBalance(WalletAddress("0x8"), Balance(BigInteger("8"))),
            PayoutAccountBalance(WalletAddress("0x9"), Balance(BigInteger("9"))),
            PayoutAccountBalance(WalletAddress("0xa"), Balance(BigInteger("10"))),
            PayoutAccountBalance(WalletAddress("0xb"), Balance(BigInteger("11")))
        )
        val tree = suppose("Merkle tree with 12 elements is created") {
            MerkleTree(
                accountBalances,
                HashFunction.KECCAK_256
            )
        }

        contract.verifyContractCalls(accountBalances, tree)
    }

    @Test
    fun mustBeCompatibleWithUnbalancedMerkleTreeWithOddNumberOfElements() {
        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("0x0"), Balance(BigInteger("0"))),
            PayoutAccountBalance(WalletAddress("0x1"), Balance(BigInteger("1"))),
            PayoutAccountBalance(WalletAddress("0x2"), Balance(BigInteger("2"))),
            PayoutAccountBalance(WalletAddress("0x3"), Balance(BigInteger("3"))),
            PayoutAccountBalance(WalletAddress("0x4"), Balance(BigInteger("4"))),
            PayoutAccountBalance(WalletAddress("0x5"), Balance(BigInteger("5"))),
            PayoutAccountBalance(WalletAddress("0x6"), Balance(BigInteger("6"))),
            PayoutAccountBalance(WalletAddress("0x7"), Balance(BigInteger("7"))),
            PayoutAccountBalance(WalletAddress("0x8"), Balance(BigInteger("8"))),
            PayoutAccountBalance(WalletAddress("0x9"), Balance(BigInteger("9"))),
            PayoutAccountBalance(WalletAddress("0xa"), Balance(BigInteger("10"))),
            PayoutAccountBalance(WalletAddress("0xb"), Balance(BigInteger("11"))),
            PayoutAccountBalance(WalletAddress("0xc"), Balance(BigInteger("12")))
        )
        val tree = suppose("Merkle tree with 13 elements is created") {
            MerkleTree(
                accountBalances,
                HashFunction.KECCAK_256
            )
        }

        contract.verifyContractCalls(accountBalances, tree)
    }

    private fun MerkleTreePathValidator.verifyContractCalls(
        accountBalances: List<PayoutAccountBalance>,
        tree: MerkleTree
    ) {
        val contract = this

        verify("contract call will return true for account balances contained in Merkle tree") {
            accountBalances.withIndex().forEach {
                expectThat(
                    contract.containsNode(
                        tree.root.hash.asEthByteArray(),
                        BigInteger.valueOf(tree.root.depth.toLong()),
                        it.value.address.rawValue,
                        it.value.balance.rawValue,
                        it.value.proof(tree)
                    ).send()
                )
            }
        }

        verify("contract call will return false for account balances not contained in Merkle tree") {
            accountBalances.withIndex().forEach {
                expectThat(
                    contract.containsNode(
                        tree.root.hash.asEthByteArray(),
                        BigInteger.valueOf(tree.root.depth.toLong()),
                        it.value.address.rawValue,
                        it.value.balance.rawValue + BigInteger.ONE,
                        it.value.proof(tree)
                    ).send()
                )
            }
        }
    }

    private fun PayoutAccountBalance.proof(tree: MerkleTree): List<ByteArray> =
        tree.pathTo(this)?.map { it.siblingHash.asEthByteArray() }!!

    private fun MerkleHash.asEthByteArray() = Numeric.hexStringToByteArray(this.value)
}
