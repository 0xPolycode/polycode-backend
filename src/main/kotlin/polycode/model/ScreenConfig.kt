package polycode.model

import polycode.config.validation.ValidationConstants
import javax.validation.constraints.Size

data class ScreenConfig(
    @field:Size(max = ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
    val beforeActionMessage: String?,
    @field:Size(max = ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
    val afterActionMessage: String?
) {
    companion object {
        val EMPTY = ScreenConfig(null, null)
    }

    fun orEmpty(): ScreenConfig? =
        if (this == EMPTY) null else this
}
