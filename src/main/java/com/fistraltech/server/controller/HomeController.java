package com.fistraltech.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to redirect root URL to the main game page
 */
@Controller
public class HomeController {
    
    /**
     * Redirect root URL to index.html
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
