package polycode.features.payout.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.exception.ResourceNotFoundException
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.params.FetchMerkleTreePathParams
import polycode.features.payout.model.response.FetchMerkleTreePathResponse
import polycode.features.payout.model.response.FetchMerkleTreeResponse
import polycode.features.payout.model.result.MerkleTreeWithId
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleHash
import polycode.features.payout.util.MerkleTree
import polycode.features.payout.util.PayoutAccountBalance
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class PayoutInfoControllerTest : TestBase() {

    @Test
    fun mustCorrectlyFetchPayoutTree() {
        val repository = mock<MerkleTreeRepository>()
        val tree = MerkleTree(
            nodes = listOf(PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ONE))),
            hashFn = HashFunction.IDENTITY
        )
        val params = FetchMerkleTreeParams(
            rootHash = MerkleHash("test"),
            chainId = ChainId(1L),
            assetContractAddress = ContractAddress("abc")
        )

        suppose("some Merkle tree is returned") {
            call(repository.fetchTree(params))
                .willReturn(tree.withRandomId())
        }

        val controller = PayoutInfoController(repository)

        verify("correct response is returned") {
            val response = controller.getPayoutTree(
                chainId = params.chainId.value,
                assetContractAddress = params.assetContractAddress.rawValue,
                rootHash = params.rootHash.value
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(FetchMerkleTreeResponse(tree)))
        }
    }

    @Test
    fun mustThrowExceptionWhenFetchingNonExistentPayoutTree() {
        val repository = mock<MerkleTreeRepository>()
        val params = FetchMerkleTreeParams(
            rootHash = MerkleHash("test"),
            chainId = ChainId(1L),
            assetContractAddress = ContractAddress("abc")
        )

        suppose("null is returned when fetching Merkle tree") {
            call(repository.fetchTree(params))
                .willReturn(null)
        }

        val controller = PayoutInfoController(repository)

        verify("exception is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getPayoutTree(
                    chainId = params.chainId.value,
                    assetContractAddress = params.assetContractAddress.rawValue,
                    rootHash = params.rootHash.value
                )
            }
        }
    }

    @Test
    fun mustCorrectlyFetchPayoutPathForSomeAccount() {
        val repository = mock<MerkleTreeRepository>()
        val accountBalance = PayoutAccountBalance(WalletAddress("def"), Balance(BigInteger.ONE))
        val params = FetchMerkleTreePathParams(
            rootHash = MerkleHash("test"),
            chainId = ChainId(1L),
            assetContractAddress = ContractAddress("abc"),
            walletAddress = accountBalance.address
        )

        suppose("request address is contained in some Merkle tree") {
            call(repository.containsAddress(params))
                .willReturn(true)
        }

        val tree = MerkleTree(
            nodes = listOf(accountBalance),
            hashFn = HashFunction.IDENTITY
        )

        suppose("some Merkle tree is returned") {
            call(repository.fetchTree(params.toFetchMerkleTreeParams))
                .willReturn(tree.withRandomId())
        }

        val controller = PayoutInfoController(repository)

        verify("correct response is returned") {
            val response = controller.getPayoutPath(
                chainId = params.chainId.value,
                assetContractAddress = params.assetContractAddress.rawValue,
                rootHash = params.rootHash.value,
                walletAddress = accountBalance.address.rawValue
            )

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        FetchMerkleTreePathResponse(
                            accountBalance.address.rawValue,
                            accountBalance.balance.rawValue,
                            tree.pathTo(accountBalance)!!
                        )
                    )
                )
        }
    }

    @Test
    fun mustThrowExceptionWhenFetchingPayoutPathForAccountNotIncludedInPayout() {
        val repository = mock<MerkleTreeRepository>()
        val accountBalance = PayoutAccountBalance(WalletAddress("def"), Balance(BigInteger.ONE))
        val params = FetchMerkleTreePathParams(
            rootHash = MerkleHash("test"),
            chainId = ChainId(1L),
            assetContractAddress = ContractAddress("abc"),
            walletAddress = accountBalance.address
        )

        suppose("request address is not contained in some Merkle tree") {
            call(repository.containsAddress(params))
                .willReturn(false)
        }

        val controller = PayoutInfoController(repository)

        verify("exception is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getPayoutPath(
                    chainId = params.chainId.value,
                    assetContractAddress = params.assetContractAddress.rawValue,
                    rootHash = params.rootHash.value,
                    walletAddress = accountBalance.address.rawValue
                )
            }
        }
    }

    @Test
    fun mustThrowExceptionWhenFetchingPayoutPathForNonExistentPayout() {
        val repository = mock<MerkleTreeRepository>()
        val accountBalance = PayoutAccountBalance(WalletAddress("def"), Balance(BigInteger.ONE))
        val params = FetchMerkleTreePathParams(
            rootHash = MerkleHash("test"),
            chainId = ChainId(1L),
            assetContractAddress = ContractAddress("abc"),
            walletAddress = accountBalance.address
        )

        suppose("request address is contained in some Merkle tree") {
            call(repository.containsAddress(params))
                .willReturn(true)
        }

        suppose("null is returned when fetching Merkle tree") {
            call(repository.fetchTree(params.toFetchMerkleTreeParams))
                .willReturn(null)
        }

        val controller = PayoutInfoController(repository)

        verify("exception is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getPayoutPath(
                    chainId = params.chainId.value,
                    assetContractAddress = params.assetContractAddress.rawValue,
                    rootHash = params.rootHash.value,
                    walletAddress = accountBalance.address.rawValue
                )
            }
        }
    }

    @Test
    fun mustThrowExceptionWhenPayoutPathDoesNotExist() {
        val repository = mock<MerkleTreeRepository>()
        val accountBalance = PayoutAccountBalance(WalletAddress("def"), Balance(BigInteger.ONE))
        val params = FetchMerkleTreePathParams(
            rootHash = MerkleHash("test"),
            chainId = ChainId(1L),
            assetContractAddress = ContractAddress("abc"),
            walletAddress = accountBalance.address
        )

        suppose("some address is contained in some Merkle tree") {
            call(repository.containsAddress(any()))
                .willReturn(true)
        }

        val tree = MerkleTree(
            nodes = listOf(accountBalance),
            hashFn = HashFunction.IDENTITY
        )

        suppose("some Merkle tree is returned") {
            call(repository.fetchTree(params.toFetchMerkleTreeParams))
                .willReturn(tree.withRandomId())
        }

        val controller = PayoutInfoController(repository)

        verify("exception is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getPayoutPath(
                    chainId = params.chainId.value,
                    assetContractAddress = params.assetContractAddress.rawValue,
                    rootHash = params.rootHash.value,
                    walletAddress = "fff"
                )
            }
        }
    }

    private fun MerkleTree.withRandomId(): MerkleTreeWithId =
        MerkleTreeWithId(MerkleTreeRootId(UUID.randomUUID()), this)
}
