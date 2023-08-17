package polycode.exception

import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.validation.ConstraintViolationException

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object : KLogging()

    @ExceptionHandler(ServiceException::class)
    fun handleServiceException(exception: ServiceException): ResponseEntity<ErrorResponse> {
        logger.debug("ServiceException", exception)
        return ResponseEntity(ErrorResponse(exception.errorCode, exception.message), exception.httpStatus)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = exception.allErrors.joinToString(prefix = "Request body errors:", separator = "\n") {
            val fieldName = (it as? FieldError)?.field ?: "(unknown field)"
            "  $fieldName: ${it.defaultMessage}"
        }
        return ResponseEntity(ErrorResponse(ErrorCode.INVALID_REQUEST_BODY, message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleValidationExceptions(exception: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = exception.constraintViolations.joinToString(prefix = "Query param errors:", separator = "\n") {
            "  ${it.propertyPath.last().name}: ${it.message}"
        }
        return ResponseEntity(ErrorResponse(ErrorCode.INVALID_QUERY_PARAM, message), HttpStatus.BAD_REQUEST)
    }
}
