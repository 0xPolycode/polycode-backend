package polycode.config.authentication

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import polycode.exception.JwtTokenException
import polycode.generated.jooq.id.UserId
import polycode.util.Either
import polycode.util.Left
import polycode.util.Right
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date
import java.util.UUID
import kotlin.time.Duration

object JwtTokenUtils {

    private const val ID_KEY = "id"
    private const val EMAIL_KEY = "email"
    private const val POLYFLOW_JWT_SUBJECT = "Polyflow"
    private const val POLYCODE_JWT_SUBJECT = "Polycode"

    @Suppress("ThrowsCount")
    fun decodeToken(token: String, publicKey: RSAPublicKey): JwtAuthToken {
        try {
            val parser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
            val claims = parser.parseClaimsJws(token)
            val expiration = claims.body.expiration

            if (Date().after(expiration)) {
                throw JwtTokenException("JWT token expired")
            }

            val (id: Either<UserId, WalletAddress>, email) = when (claims.body.subject) {
                POLYFLOW_JWT_SUBJECT -> Pair(
                    claims.getClaim(ID_KEY) { Left(UserId(UUID.fromString(it))) },
                    claims.getClaim(EMAIL_KEY) { it }
                )

                POLYCODE_JWT_SUBJECT -> Pair(claims.getClaim(ID_KEY) { Right(WalletAddress(it)) }, null)
                else -> throw JwtTokenException("Unsupported JWT subject")
            }

            return JwtAuthToken(
                token = token,
                id = id,
                email = email,
                validUntil = UtcDateTime.ofInstant(expiration.toInstant())
            )
        } catch (_: JwtException) {
            throw JwtTokenException("Could not validate JWT token")
        } catch (_: IllegalArgumentException) {
            throw JwtTokenException("Invalid user ID in JWT token")
        } catch (_: NumberFormatException) {
            throw JwtTokenException("Invalid wallet address in JWT token")
        }
    }

    fun encodeToken(walletAddress: WalletAddress, privateKey: RSAPrivateKey, tokenValidity: Duration): JwtAuthToken {
        val issuedAt = Date()
        val validUntil = Date.from(issuedAt.toInstant().plusMillis(tokenValidity.inWholeMilliseconds))
        val token = Jwts.builder()
            .setSubject(POLYCODE_JWT_SUBJECT)
            .claim(ID_KEY, walletAddress.rawValue)
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .setIssuedAt(issuedAt)
            .setExpiration(validUntil)
            .compact()

        return JwtAuthToken(
            token = token,
            id = Right(walletAddress),
            email = null,
            validUntil = UtcDateTime.ofInstant(validUntil.toInstant())
        )
    }

    private fun <T> Jws<Claims>.getClaim(key: String, transform: (String) -> T): T =
        (body[key] as? String)?.let(transform) ?: throw JwtTokenException("Invalid JWT token format")
}
