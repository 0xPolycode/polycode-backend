package polycode.util

import polycode.generated.jooq.id.ProjectId
import java.util.UUID

object Constants {
    val NIL_PROJECT_ID = ProjectId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
}
