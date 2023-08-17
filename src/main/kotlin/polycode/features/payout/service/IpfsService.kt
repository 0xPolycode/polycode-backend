package polycode.features.payout.service

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.payout.util.IpfsHash

interface IpfsService {
    fun pinJsonToIpfs(json: JsonNode): IpfsHash
}
