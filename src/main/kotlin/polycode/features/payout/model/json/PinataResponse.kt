package polycode.features.payout.model.json

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.ZonedDateTime

data class PinataResponse(
    @JsonProperty("IpfsHash")
    val ipfsHash: String?,
    @JsonProperty("PinSize")
    val pinSize: Long?,
    @JsonProperty("Timestamp")
    val timestamp: ZonedDateTime?
)
