package polycode.config.converters

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import polycode.util.WalletAddress

@Component
@ConfigurationPropertiesBinding
class StringToWalletAddressConverter : Converter<String, WalletAddress> {
    override fun convert(source: String): WalletAddress = WalletAddress(source)
}
