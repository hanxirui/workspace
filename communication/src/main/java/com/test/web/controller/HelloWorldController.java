package com.test.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HelloWorldController {

    @Value("#{configProperties['tcp_port']}")
    private int port;
    
    @Value("${tcp_port}")
    private int port1;
    
    @RequestMapping("/index")
    public String index(final Model model) {
        System.out.println("--------------------"+ port);
        System.out.println("--------------------"+ port1);
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
