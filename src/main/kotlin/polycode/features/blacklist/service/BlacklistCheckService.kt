package polycode.features.blacklist.service

import polycode.util.EthereumAddress

interface BlacklistCheckService {
    fun exists(address: EthereumAddress): Boolean
}
