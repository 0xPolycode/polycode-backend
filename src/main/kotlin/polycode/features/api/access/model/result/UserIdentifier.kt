package polycode.features.api.access.model.result

import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.UserId
import polycode.util.WalletAddress
import polyflow.generated.jooq.id.PolyflowUserId

sealed interface UserIdentifier {
    val id: UserId
    val userIdentifier: String
    val identifierType: UserIdentifierType
}

data class UserWalletAddressIdentifier(
    override val id: UserId,
    val walletAddress: WalletAddress
) : UserIdentifier {
    override val userIdentifier = walletAddress.rawValue
    override val identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
}

data class UserPolyflowAccountIdIdentifier(
    override val id: UserId,
    val polyflowId: PolyflowUserId
) : UserIdentifier {
    override val userIdentifier = polyflowId.value.toString()
    override val identifierType = UserIdentifierType.POLYFLOW_USER_ID
}
