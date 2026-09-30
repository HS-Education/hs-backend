package com.hs.hstesis.shared.interfaces.rest;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FrontendControllerTest {
    @Test void deepLinksForwardToTheSpaButUnknownApisNeverDo() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FrontendController()).build();
        for (String path : new String[]{"/sign-in", "/classrooms/1", "/metrics", "/admin/users", "/not-found"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
        mvc.perform(get("/api/v1/missing")).andExpect(status().isNotFound());
    }
}
