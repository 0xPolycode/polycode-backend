package polycode.util.annotation

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SchemaName(val name: String)
