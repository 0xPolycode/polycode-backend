package polycode.features.contract.readcall.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.features.api.access.model.result.Project
import polycode.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.request.ReadonlyFunctionCallRequest
import polycode.features.contract.readcall.model.response.ReadonlyFunctionCallResponse
import polycode.features.contract.readcall.service.ContractReadonlyFunctionCallService
import javax.validation.Valid

@Validated
@RestController
class ContractReadonlyFunctionCallController(
    private val contractReadonlyFunctionCallService: ContractReadonlyFunctionCallService,
    private val objectMapper: ObjectMapper
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/readonly-function-call")
    fun callReadonlyContractFunction(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: ReadonlyFunctionCallRequest
    ): ResponseEntity<ReadonlyFunctionCallResponse> {
        val params = CreateReadonlyFunctionCallParams(requestBody)
        val result = contractReadonlyFunctionCallService.callReadonlyContractFunction(params, project)
        return ResponseEntity.ok(
            ReadonlyFunctionCallResponse(
                result = result,
                outputParams = objectMapper.createArrayNode().apply {
                    addAll(requestBody.outputParams.map { it.rawJson })
                }
            )
        )
    }
}
