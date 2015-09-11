package com.test.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HelloWorldController {

    @RequestMapping("/index")
    public String index(final Model model) {

        return "index";

    }

    @RequestMapping("/hello")
    public String hello(@RequestParam(value = "name", required = false, defaultValue = "World") final String name,
        final Model model) {

        model.addAttribute("name", name);
        // returns the view name
        return "/views/helloworld";

    }

}
