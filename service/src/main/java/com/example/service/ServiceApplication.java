package com.example.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Random;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}

@Controller
@ResponseBody
class GreetingsRestController {

    private long random(int seconds) {
        return new Random().nextInt(seconds) * 1000L;
    }

    @GetMapping("/hello")
    Map<String, String> hello() throws Exception {
        var delay = Math.max(random(5), random(5));
        var message = "Hello, world after " + delay + "ms";
        System.out.println(message);
        Thread.sleep(delay);
        return Map.of("message", message);
    }
}