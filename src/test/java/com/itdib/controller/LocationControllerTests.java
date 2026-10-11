package com.itdib.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import com.itdib.model.ServerLocation;
import com.itdib.service.ServerLocationService;

@WebMvcTest({LocationController.class, MapController.class})
class LocationControllerTests {
    @Autowired MockMvc mvc;
    @MockBean ServerLocationService service;

    @Test
    void lookupReturnsJsonCoordinates() throws Exception {
        ServerLocation location = new ServerLocation();
        location.setLatitude("12.5"); location.setLongitude("-34.5");
        when(service.getLocation("192.0.2.1")).thenReturn(location);
        mvc.perform(get("/loc").param("ip", "192.0.2.1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.latitude").value("12.5"))
            .andExpect(jsonPath("$.longitude").value("-34.5"));
    }

    @Test
    void missingQueryReturns400() throws Exception {
        mvc.perform(get("/loc")).andExpect(status().isBadRequest());
    }

    @Test
    void preservesExpectedErrorStatuses() throws Exception {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND, HttpStatus.SERVICE_UNAVAILABLE}) {
            doThrow(new ResponseStatusException(status, "example error")).when(service).getLocation("192.0.2.1");
            mvc.perform(get("/loc").param("ip", "192.0.2.1")).andExpect(status().is(status.value()));
        }
    }

    @Test
    void lookupPageRendersWithoutExternalScripts() throws Exception {
        mvc.perform(get("/getPages")).andExpect(status().isOk()).andExpect(view().name("map"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("IP Location Lookup")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("maps.google.com"))));
    }
}
