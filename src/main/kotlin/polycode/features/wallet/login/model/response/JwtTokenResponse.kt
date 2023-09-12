package polycode.features.wallet.login.model.response

import polycode.config.authentication.JwtAuthToken
import java.time.OffsetDateTime

data class JwtTokenResponse(
    val token: String,
    val email: String?,
    val validUntil: OffsetDateTime
) {
    constructor(jwt: JwtAuthToken) : this(
        token = jwt.token,
        email = jwt.email,
        validUntil = jwt.validUntil.value
    )
}
