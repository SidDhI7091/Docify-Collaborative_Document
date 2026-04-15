// PageController.java
package com.docify.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String root(HttpSession session) {
        return session.getAttribute("userId") != null
            ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String login()     { return "forward:/login.html"; }

    @GetMapping("/register")
    public String register()  { return "forward:/register.html"; }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        if (session.getAttribute("userId") == null) return "redirect:/login";
        return "forward:/dashboard.html";
    }

    @GetMapping("/editor/{documentId}")
    public String editor(@PathVariable Long documentId, HttpSession session) {
        if (session.getAttribute("userId") == null) return "redirect:/login";
        return "forward:/editor.html";
    }

    @GetMapping("/share/{token}")
    public String shareView(@PathVariable String token) {
        return "forward:/shared-view.html";
    }
}