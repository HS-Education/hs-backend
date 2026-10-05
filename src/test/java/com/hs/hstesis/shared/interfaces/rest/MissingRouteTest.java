package com.hs.hstesis.shared.interfaces.rest;

import com.hs.hstesis.shared.interfaces.rest.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real DispatcherServlet and static resource handler, not a controller that fabricates a 404.
@SpringJUnitConfig(MissingRouteTest.Config.class)
@WebAppConfiguration
class MissingRouteTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }

    @Test void absentPublicRoutesAndDisabledSwaggerAre404Not500() throws Exception {
        for (String path : new String[]{"/homeaaa", "/swagger-ui.html", "/swagger-ui/index.html",
                "/v3/api-docs", "/missing.js", "/assets/missing.svg", "/api/v1/missing"}) {
            mvc.perform(get(path).accept(MediaType.TEXT_HTML, MediaType.ALL))
                    .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("status").value(404)).andExpect(jsonPath("error").value("Not Found"))
                    .andExpect(jsonPath("message").value("The requested resource was not found."))
                    .andExpect(jsonPath("timestamp").isString()).andExpect(forwardedUrl(null));
        }
    }

    @Test void missingPathAndQueryAreNotReflectedInTheResponse() throws Exception {
        mvc.perform(get("/PRIVATE_PATH_MARKER.js").queryParam("token", "PRIVATE_QUERY_MARKER"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("PRIVATE_PATH_MARKER"))))
                .andExpect(content().string(not(containsString("PRIVATE_QUERY_MARKER"))));
    }

    @Test void knownFrontendDeepLinksStillForwardToAngular() throws Exception {
        for (String path : new String[]{"/", "/sign-in", "/home", "/classrooms/1", "/not-found"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test void realServerFailuresRemain500WithoutExceptionDetails() throws Exception {
        mvc.perform(get("/diagnostic/real-failure"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("status").value(500))
                .andExpect(content().string(not(containsString("PRIVATE_FAILURE_MARKER"))));
    }

    @Test void frameworkClientErrorsKeep400405406And415InsteadOf500() throws Exception {
        mvc.perform(get("/diagnostic/required")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("status").value(400));
        mvc.perform(post("/diagnostic/required")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
        mvc.perform(get("/diagnostic/json").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
        mvc.perform(post("/diagnostic/json").contentType(MediaType.TEXT_PLAIN).content("PRIVATE_BODY_MARKER"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("status").value(415))
                .andExpect(content().string(not(containsString("PRIVATE_BODY_MARKER"))));
    }

    @RestController static class FailureController {
        @GetMapping("/diagnostic/real-failure") String fail() { throw new RuntimeException("PRIVATE_FAILURE_MARKER"); }
        @GetMapping("/diagnostic/required") String required(@RequestParam String value) { return value; }
        @GetMapping(value = "/diagnostic/json", produces = "application/json") String json() { return "{}"; }
        @PostMapping(value = "/diagnostic/json", consumes = "application/json") String body(@RequestBody String value) { return value; }
    }

    @Configuration @EnableWebMvc static class Config implements WebMvcConfigurer {
        @Bean FrontendController frontend() { return new FrontendController(); }
        @Bean GlobalExceptionHandler advice() { return new GlobalExceptionHandler(); }
        @Bean FailureController failure() { return new FailureController(); }
        @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**").addResourceLocations("classpath:/route-test-fixtures/");
        }
    }
}
