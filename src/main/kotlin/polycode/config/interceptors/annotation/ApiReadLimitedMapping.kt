package polycode.config.interceptors.annotation

import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@RequestMapping(method = [RequestMethod.GET])
annotation class ApiReadLimitedMapping(
    val idType: IdType,
    @get:AliasFor(annotation = RequestMapping::class) val path: String
)
