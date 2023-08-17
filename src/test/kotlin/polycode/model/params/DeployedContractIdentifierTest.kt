package polycode.model.params

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.exception.InvalidRequestBodyException
import polycode.features.contract.deployment.model.params.DeployedContractAddressIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractAliasIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractIdentifierRequestBody
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress
import java.util.UUID

class DeployedContractIdentifierTest : TestBase() {

    companion object {
        data class RequestBody(
            override val deployedContractId: ContractDeploymentRequestId?,
            override val deployedContractAlias: String?,
            override val contractAddress: String?
        ) : DeployedContractIdentifierRequestBody
    }

    @Test
    fun mustCorrectlyCreateDeployedContractIdentifierFromDeployedContractId() {
        val id = ContractDeploymentRequestId(UUID.randomUUID())

        val result = suppose("deployed contract identifier will be created") {
            DeployedContractIdentifier(
                RequestBody(
                    deployedContractId = id,
                    deployedContractAlias = null,
                    contractAddress = null
                )
            )
        }

        verify("correct identifier is created") {
            expectThat(result)
                .isEqualTo(DeployedContractIdIdentifier(id))
        }
    }

    @Test
    fun mustCorrectlyCreateDeployedContractIdentifierFromDeployedContractAlias() {
        val alias = "alias"

        val result = suppose("deployed contract identifier will be created") {
            DeployedContractIdentifier(
                RequestBody(
                    deployedContractId = null,
                    deployedContractAlias = alias,
                    contractAddress = null
                )
            )
        }

        verify("correct identifier is created") {
            expectThat(result)
                .isEqualTo(DeployedContractAliasIdentifier(alias))
        }
    }

    @Test
    fun mustCorrectlyCreateDeployedContractIdentifierFromDeployedContractAddress() {
        val contractAddress = ContractAddress("a")

        val result = suppose("deployed contract identifier will be created") {
            DeployedContractIdentifier(
                RequestBody(
                    deployedContractId = null,
                    deployedContractAlias = null,
                    contractAddress = contractAddress.rawValue
                )
            )
        }

        verify("correct identifier is created") {
            expectThat(result)
                .isEqualTo(DeployedContractAddressIdentifier(contractAddress))
        }
    }

    @Test
    fun mustThrowInvalidRequestBodyExceptionWhenAllContractIdentifiersArePresent() {
        verify("InvalidRequestBodyException is thrown") {
            expectThrows<InvalidRequestBodyException> {
                DeployedContractIdentifier(
                    RequestBody(
                        deployedContractId = ContractDeploymentRequestId(UUID.randomUUID()),
                        deployedContractAlias = "alias",
                        contractAddress = "a"
                    )
                )
            }
        }
    }

    @Test
    fun mustThrowInvalidRequestBodyExceptionWhenNoContractIdentifiersArePresent() {
        verify("InvalidRequestBodyException is thrown") {
            expectThrows<InvalidRequestBodyException> {
                DeployedContractIdentifier(
                    RequestBody(
                        deployedContractId = null,
                        deployedContractAlias = null,
                        contractAddress = null
                    )
                )
            }
        }
    }
}
