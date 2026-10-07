package com.aries.backend.shared.interfaces.rest;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aries.backend.shared.infrastructure.health.DatabaseProbe;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiExceptionHandlerTest {
    @Test
    void unexpectedAndInfrastructureFailuresPreserveStatusAndTraceWithoutExposingDetails()
            throws Exception {
        DatabaseProbe probe = mock(DatabaseProbe.class);
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(new HealthController(probe))
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(new RequestIdFilter())
                        .build();
        doThrow(new DataAccessResourceFailureException("PRIVATE_DATABASE_SENTINEL"))
                .when(probe)
                .verify();
        MockHttpServletResponse database =
                mvc.perform(get("/api/v1/health"))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$", aMapWithSize(4)))
                        .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                        .andExpect(jsonPath("$.data").value(nullValue()))
                        .andReturn()
                        .getResponse();
        assertThat(database.getContentAsString())
                .doesNotContain("PRIVATE_DATABASE_SENTINEL")
                .contains(database.getHeader("X-Trace-Id"));
        doThrow(new IllegalStateException("PRIVATE_EXCEPTION_SENTINEL")).when(probe).verify();
        MockHttpServletResponse unexpected =
                mvc.perform(get("/api/v1/health"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$", aMapWithSize(4)))
                        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                        .andExpect(jsonPath("$.data").value(nullValue()))
                        .andReturn()
                        .getResponse();
        assertThat(unexpected.getContentAsString())
                .doesNotContain("PRIVATE_EXCEPTION_SENTINEL")
                .contains(unexpected.getHeader("X-Trace-Id"));
    }
}
