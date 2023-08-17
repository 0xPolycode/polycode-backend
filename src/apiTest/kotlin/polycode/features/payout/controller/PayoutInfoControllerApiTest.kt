package polycode.features.payout.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.TestData
import polycode.exception.ErrorCode
import polycode.features.payout.model.response.FetchMerkleTreePathResponse
import polycode.features.payout.model.response.FetchMerkleTreeResponse
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleTree
import polycode.features.payout.util.PayoutAccountBalance
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger

class PayoutInfoControllerApiTest : ControllerTestBase() {

    private final val userAddress = WalletAddress("0x8f52B0cC50967fc59C6289f8FDB3E356EdeEBD23")
    private final val secondUserAddress = WalletAddress("0xd43e088622404A5A21267033EC200383d39C22ca")
    protected final val contractAddress = ContractAddress("0x5BF28A1E60Eb56107FAd2dE1F2AA51FC7A60C690")

    @Autowired
    private lateinit var merkleTreeRepository: MerkleTreeRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyFetchPayoutTree() {
        val accountBalance = PayoutAccountBalance(userAddress, Balance(BigInteger.ONE))
        val tree = MerkleTree(listOf(accountBalance), HashFunction.KECCAK_256)
        val blockNumber = BlockNumber(BigInteger.TEN)

        suppose("some Merkle tree is stored in the database") {
            merkleTreeRepository.storeTree(tree, TestData.CHAIN_ID, contractAddress, blockNumber)
        }

        verify("correct Merkle tree is returned") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/payout-info/${TestData.CHAIN_ID.value}/${contractAddress.rawValue}" +
                        "/tree/${tree.root.hash.value}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
            val responseTree = objectMapper.readTree(response.response.contentAsString)

            expectThat(responseTree)
                .isEqualTo(objectMapper.valueToTree(FetchMerkleTreeResponse(tree)))
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingNonExistentPayoutTree() {
        verify("error is returned for non-existent Merkle tree") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/payout-info/123/0x1/tree/unknownHash")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchPayoutPathForSomeAccount() {
        val accountBalance = PayoutAccountBalance(userAddress, Balance(BigInteger.ONE))
        val tree = MerkleTree(listOf(accountBalance), HashFunction.KECCAK_256)
        val blockNumber = BlockNumber(BigInteger.TEN)

        suppose("some Merkle tree is stored in the database") {
            merkleTreeRepository.storeTree(tree, TestData.CHAIN_ID, contractAddress, blockNumber)
        }

        verify("correct Merkle tree path is returned") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/payout-info/${TestData.CHAIN_ID.value}/${contractAddress.rawValue}/tree" +
                        "/${tree.root.hash.value}/path/${accountBalance.address.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
            val responseTree = objectMapper.readTree(response.response.contentAsString)

            expectThat(responseTree)
                .isEqualTo(
                    objectMapper.valueToTree(
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
    fun mustReturn404NotFoundWhenFetchingPayoutPathForNonExistentPayout() {
        verify("error is returned for non-existent payout") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/payout-info/123/0x1/tree/unknownHash/path/0x2")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustReturn404NotFoundWhenFetchingPayoutPathForAccountNotIncludedInPayout() {
        val payoutAccountBalance = PayoutAccountBalance(secondUserAddress, Balance(BigInteger.TEN))
        val tree = MerkleTree(listOf(payoutAccountBalance), HashFunction.KECCAK_256)
        val blockNumber = BlockNumber(BigInteger.TEN)

        suppose("some Merkle tree is stored in the database") {
            merkleTreeRepository.storeTree(tree, TestData.CHAIN_ID, contractAddress, blockNumber)
        }

        val requestAccountBalance = PayoutAccountBalance(userAddress, Balance(BigInteger.ONE))

        verify("error is returned for account not included in the payout") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/payout-info/${TestData.CHAIN_ID.value}/${contractAddress.rawValue}/tree" +
                        "/${tree.root.hash.value}/path/${requestAccountBalance.address.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
