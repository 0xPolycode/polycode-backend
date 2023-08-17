package polycode

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import polycode.TestBase.Companion.VerifyMessage
import polycode.exception.ErrorCode
import polycode.exception.ErrorResponse
import polycode.testcontainers.SharedTestContainers

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ControllerTestBase : TestBase() {

    @Suppress("unused")
    protected val postgresContainer = SharedTestContainers.postgresContainer

    @Suppress("unused")
    protected val hardhatContainer = SharedTestContainers.hardhatContainer

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun beforeEach(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        hardhatContainer.reset()
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(
                MockMvcRestDocumentation.document(
                    "{ClassName}/{methodName}",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                )
            )
            .build()
    }

    protected fun VerifyMessage.expectResponseErrorCode(result: MvcResult, expectedErrorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)

        expectThat(response.errorCode)
            .isEqualTo(expectedErrorCode)
    }
}
