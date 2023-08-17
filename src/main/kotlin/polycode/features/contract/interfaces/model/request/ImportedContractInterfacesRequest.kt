package polycode.features.contract.interfaces.model.request

import org.springframework.validation.annotation.Validated
import polycode.config.validation.MaxArgsSize
import polycode.config.validation.MaxStringSize
import javax.validation.Valid
import javax.validation.constraints.NotNull

@Validated
data class ImportedContractInterfacesRequest(
    @field:Valid
    @field:NotNull
    @field:MaxArgsSize
    val interfaces: List<@MaxStringSize String>
)
