package polycode.features.contract.readcall.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.service.DeployedContractIdentifierResolverService
import polycode.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.util.BlockName
import polycode.util.WithDeployedContractIdAndAddress

@Service
class ContractReadonlyFunctionCallServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val deployedContractIdentifierResolverService: DeployedContractIdentifierResolverService,
    private val blockchainService: BlockchainService
) : ContractReadonlyFunctionCallService {

    companion object : KLogging()

    override fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult> {
        logger.info { "Calling contract read-only function, params: $params, project: $project" }

        val (deployedContractId, contractAddress) = deployedContractIdentifierResolverService
            .resolveContractIdAndAddress(params.identifier, project)
        val data = functionEncoderService.encode(
            functionName = params.functionName,
            arguments = params.functionParams
        )

        val value = blockchainService.callReadonlyFunction(
            chainSpec = ChainSpec(
                chainId = project.chainId,
                customRpcUrl = project.customRpcUrl
            ),
            params = ExecuteReadonlyFunctionCallParams(
                contractAddress = contractAddress,
                callerAddress = params.callerAddress,
                functionName = params.functionName,
                functionData = data,
                outputParams = params.outputParams
            ),
            blockParameter = params.blockNumber ?: BlockName.LATEST
        )

        return WithDeployedContractIdAndAddress(
            value = value,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress
        )
    }
}
