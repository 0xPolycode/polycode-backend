package polycode.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.VoidResponse
import org.web3j.protocol.http.HttpService
import polycode.util.BlockNumber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class HardhatTestContainer : GenericContainer<HardhatTestContainer>("gluwa/hardhat-dev:1.0.0") {

    @Suppress("unused")
    companion object {
        private const val HARDHAT_PORT = 8545

        const val ACCOUNT_ADDRESS_1 = "0x959fd7ef9089b7142b6b908dc3a8af7aa8ff0fa1"
        const val ACCOUNT_ADDRESS_2 = "0x4e90a36b45879f5bae71b57ad525e817afa54890"
        const val ACCOUNT_ADDRESS_3 = "0xb6a8490101a0521677b66866b8052ee9f9975c17"
        const val ACCOUNT_ADDRESS_4 = "0xb0201641d9b936eb20155a38439ae6ab07d85fbd"
        const val ACCOUNT_ADDRESS_5 = "0x301e1528bad61177ef8ff89bd4ad6760581e5409"
        const val ACCOUNT_ADDRESS_6 = "0x453460d66ebde5f888f999255f9291f1caa83e5b"
        const val ACCOUNT_ADDRESS_7 = "0x21a0fad034cc95891006e0687892c3434c59521d"
        const val ACCOUNT_ADDRESS_8 = "0x53af25e00ef5a4a9b0f4c2431bd07a2d18ed5b8b"
        const val ACCOUNT_ADDRESS_9 = "0xb73614389f815b78217052984d5561bad52a420a"
        const val ACCOUNT_ADDRESS_10 = "0x35e13c4870077f4610b74f23e887cbb10e21c19f"

        val ACCOUNTS: List<Credentials> = listOf(
            Credentials.create("0xabf82ff96b463e9d82b83cb9bb450fe87e6166d4db6d7021d0c71d7e960d5abe"),
            Credentials.create("0xdcb7118c9946a39cd40b661e0d368e4afcc3cc48d21aa750d8164ca2e44961c4"),
            Credentials.create("0x2d7aaa9b78d759813448eb26483284cd5e4344a17dede2ab7f062f0757113a28"),
            Credentials.create("0x0e5c6904f09186a0cfe945da201e9d9f0443e07d9e795a9d26cc5cbb882874ac"),
            Credentials.create("0x7f60d75be8f8833a47311c001adbc3771784c52ea9115200a516e3f050c3bc2b"),
            Credentials.create("0x949dbd0607598c41478b32c27da65ab550d54246922fa8978a8c1b9e901e06a6"),
            Credentials.create("0x87a3c9405478581d513a16075038e5869d02311371b757f7163200a09f230f18"),
            Credentials.create("0xe5faea48461ef5a0b78839573073e5a2f579155bf7a4cceb15e49b41963af6a3"),
            Credentials.create("0xccfb970ed6f3bb68a15d87a67071da16544c918cf978dc41906e686326bb953d"),
            Credentials.create("0x27a3706e23375353aabc8da00d59db6795abae3036dee967103088c8f15e5335")
        )
    }

    private val web3jService: Web3jService
    lateinit var web3j: Web3j
        private set

    val mappedPort: String

    init {
        waitStrategy = LogMessageWaitStrategy()
            .withRegEx("Started HTTP and WebSocket JSON-RPC server at .*")
            .withTimes(1)
            .withStartupTimeout(60.seconds.toJavaDuration())

        addExposedPort(HARDHAT_PORT)
        start()

        mappedPort = getMappedPort(HARDHAT_PORT).toString()
        web3jService = HttpService("http://localhost:$mappedPort")

        System.setProperty("HARDHAT_PORT", mappedPort)

        reset()
    }

    fun mine() {
        Request("evm_mine", emptyList<String>(), web3jService, VoidResponse::class.java).send()
    }

    fun reset() {
        web3j = Web3j.build(web3jService)

        Request("hardhat_reset", emptyList<String>(), web3jService, VoidResponse::class.java).send()
        Request("evm_setAutomine", listOf(true), web3jService, VoidResponse::class.java).send()

        web3j = Web3j.build(web3jService)
    }

    fun blockNumber(): BlockNumber = BlockNumber(web3j.ethBlockNumber().send().blockNumber)
}
