package com.aries.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmailConfiguration.class)
class ResultContractIntegrationTests extends IntegrationTestSupport {

    @Test void successHasExactlyFourFieldsAndServerGeneratedTrace() throws Exception {
        var response = mvc.perform(get("/api/v1/categories").header("X-Trace-Id", "untrusted"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").isNotEmpty()).andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.traceId").isNotEmpty()).andReturn().getResponse();
        String traceId = response.getHeader("X-Trace-Id");
        assertThat(traceId).isNotEqualTo("untrusted");
        assertThat(java.util.UUID.fromString(traceId).toString()).isEqualTo(traceId);
        assertThat(response.getContentAsString()).contains("\"traceId\":\"" + traceId + "\"");
    }

    @Test void emptySuccessStillIncludesDataAndTrace() throws Exception {
        var session = register("result@example.com", "result123", "测试成员");
        mvc.perform(post("/api/v1/auth/logout").cookie(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test void authenticationAndValidationErrorsUseTheSameEnvelope() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.msg").isNotEmpty()).andExpect(jsonPath("$.data").value(nullValue()));
        mvc.perform(get("/api/v1/publications").param("page", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

}
