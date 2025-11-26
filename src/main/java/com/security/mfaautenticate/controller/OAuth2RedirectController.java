package com.security.mfaautenticate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuth2RedirectController {

    @GetMapping("/oauth2/redirect")
    public String redirect(@RequestParam String token) {
        return "redirect:http://localhost:3000/dashboard?token=" + token;
    }

    @GetMapping("/mfa-verification")
    public String mfaVerification(@RequestParam String email) {
        return "redirect:http://localhost:3000/mfa-verify?email=" + email;
    }
}