package polycode.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import polycode.config.authentication.AuthenticationEntryPointExceptionHandler
import polycode.config.authentication.JwtAuthenticationFilter
import polycode.config.authentication.JwtAuthenticationProvider
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfig(private val objectMapper: ObjectMapper) {

    @Autowired
    fun authBuilder(authBuilder: AuthenticationManagerBuilder, jwtProperties: JwtProperties) {
        val spec = RSAPublicKeySpec(jwtProperties.privateKey.modulus, jwtProperties.privateKey.publicExponent)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(spec) as RSAPublicKey

        authBuilder.authenticationProvider(JwtAuthenticationProvider(publicKey))
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("*")
            allowedMethods = listOf(
                HttpMethod.HEAD.name,
                HttpMethod.GET.name,
                HttpMethod.POST.name,
                HttpMethod.PUT.name,
                HttpMethod.PATCH.name,
                HttpMethod.OPTIONS.name,
                HttpMethod.DELETE.name
            )
            allowedHeaders = listOf(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.CACHE_CONTROL,
                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                CustomHeaders.API_KEY_HEADER,
                CustomHeaders.PROJECT_ID_HEADER,
                CustomHeaders.CORRELATION_ID_HEADER
            )
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val authenticationHandler = AuthenticationEntryPointExceptionHandler(objectMapper)
        val authenticationTokenFilter = JwtAuthenticationFilter()

        http.cors().and().csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .logout().disable()
            .authorizeRequests()
            .antMatchers("/actuator/**").permitAll()
            .antMatchers("/public/**").permitAll()
            .antMatchers("/docs/index.html").permitAll()
            .antMatchers("/docs/internal.html").permitAll()
            .antMatchers("/v1/**").permitAll()
            .antMatchers("/v1/wallet-login/**").permitAll()
            .antMatchers("/v1/projects/by-api-key").permitAll()
            .antMatchers("/v1/projects/**").authenticated()
            .antMatchers("/v1/address-book/**").authenticated()
            .antMatchers(HttpMethod.GET, "/v1/address-book/**").permitAll()
            .antMatchers(HttpMethod.GET, "/v1/address-book/by-alias/**").authenticated()
            .antMatchers("/v1/multi-payment-template/**").authenticated()
            .antMatchers(HttpMethod.GET, "/v1/multi-payment-template/**").permitAll()
            .antMatchers(HttpMethod.GET, "/v1/api-usage").permitAll()
            .antMatchers("/v1/api-usage/by-project/**").authenticated()
            .antMatchers("/v1/billing/**").authenticated()
            .antMatchers(HttpMethod.GET, "/v1/billing/available-plans").permitAll()
            .antMatchers(HttpMethod.GET, "/v1/billing/webhook").permitAll()
            .antMatchers("/v1/blacklist/**").authenticated()
            .antMatchers("/v1/claimable-payouts").authenticated()
            .anyRequest().authenticated()
            .and()
            .exceptionHandling().authenticationEntryPoint(authenticationHandler).and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
