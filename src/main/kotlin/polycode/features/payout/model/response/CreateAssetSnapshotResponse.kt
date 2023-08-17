package polycode.features.payout.model.response

import com.fasterxml.jackson.annotation.JsonCreator
import polycode.generated.jooq.id.AssetSnapshotId
import java.util.UUID

data class CreateAssetSnapshotResponse(val id: AssetSnapshotId) {
    companion object {
        @JvmStatic
        @JsonCreator // needed in API tests
        fun create(id: UUID) = CreateAssetSnapshotResponse(AssetSnapshotId(id))
    }
}
