package com.fistraltech.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Spring MVC controller for serving the static single-page UI.
 *
 * <p>The application uses a static frontend (see {@code src/main/resources/static}) and hash-based routing.
 * This controller forwards the root path to {@code /index.html} so the UI can initialize normally.
 *
 * @author Fistral Technologies
 */
@Controller
public class HomeController {
    
    /**
     * Forwards {@code /} to the static UI entrypoint.
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
