package com.tuckshop.pos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/pos")
    public String pos() {
        return "pos";
    }

    @GetMapping("/products")
    public String products() {
        return "products";
    }

    @GetMapping("/sales")
    public String sales() {
        return "sales";
    }

    @GetMapping("/khata")
    public String khata() {
        return "khata";
    }

    @GetMapping("/reports")
    public String reports() {
        return "reports";
    }

    @GetMapping("/users")
    public String users() {
        return "users";
    }

    @GetMapping("/backups")
    public String backups() {
        return "backups";
    }

    @GetMapping("/license")
    public String license() {
        return "license";
    }

    @GetMapping("/sale-edits")
    public String saleEdits() {
        return "sale-edits";
    }

    @GetMapping("/receipt/{saleId}")
    public String receipt(@PathVariable Long saleId, Model model) {
        model.addAttribute("saleId", saleId);
        return "receipt";
    }
}
