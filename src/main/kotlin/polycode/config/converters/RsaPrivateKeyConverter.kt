package polycode.config.converters

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

@Component
@ConfigurationPropertiesBinding
class RsaPrivateKeyConverter : Converter<String, RSAPrivateCrtKey> {

    private val beginPublicKey = "-----BEGIN PRIVATE KEY-----"
    private val endPublicKey = "-----END PRIVATE KEY-----"
    private val keyFactory = KeyFactory.getInstance("RSA")

    override fun convert(source: String): RSAPrivateCrtKey? {
        val key = Base64.getDecoder().decode(source.replace(beginPublicKey, "").replace(endPublicKey, ""))
        return keyFactory.generatePrivate(PKCS8EncodedKeySpec(key)) as RSAPrivateCrtKey
    }
}
