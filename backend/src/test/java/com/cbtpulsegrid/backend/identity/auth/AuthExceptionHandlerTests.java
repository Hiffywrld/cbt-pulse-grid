package com.cbtpulsegrid.backend.identity.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class AuthExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void configureExceptionHandling() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void unexpectedExceptionsReturnGenericJsonWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/test/unexpected"))
                .andExpect(jsonPath("$.validationErrors").isEmpty())
                .andExpect(content().string(not(containsString("sensitive-database-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/unexpected")
        void fail() {
            throw new RuntimeException("sensitive-database-detail");
        }
    }
}
