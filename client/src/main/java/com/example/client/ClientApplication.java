package com.example.client;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(DiscoveryClient dc) {
        return args -> dc.getInstances("service")
                .forEach(si -> System.out.println("new service: " + si.getServiceId() + ':' + si.getPort()));
    }

    @Bean
    WebClient webClient(WebClient.Builder b) {
        return b.build();
    }
}

@Controller
@ResponseBody
class ClientRestController {

    private final WebClient http;
    private final DiscoveryClient dc;
    private final LoadBalancerClient lbc;

    ClientRestController(WebClient http, DiscoveryClient dc, LoadBalancerClient lbc) {
        this.http = http;
        this.dc = dc;
        this.lbc = lbc;
    }

    @GetMapping("/client")
    Mono<String> helloClient() {
        var max = 3;
        var serviceInstances = this.dc.getInstances("service");
        Assert.isTrue(max <= serviceInstances.size(),
                () -> "you should have at least " + max + " instances in the service registry!");
        var chosen = new HashSet<ServiceInstance>();
        while (chosen.size() < max) {
            var randomIndex = Math.random() * serviceInstances.size();
            var index = (int) (Math.min(randomIndex * 1, serviceInstances.size() - 1));
            var element = serviceInstances.get(index);
            chosen.add(element);
            System.out.println("there are " + chosen.size() + " elements already chosen");
        }
        var requests = Flux
                .fromIterable(chosen)
                .flatMap(si -> {
                    var uri = si.getScheme() + "://" + si.getHost() + ':' + si.getPort() + "/hello";
                    return http.get().uri(uri).retrieve().bodyToMono(String.class);
                });
        return Flux
                .firstWithSignal(requests)
                .take(1)
                .singleOrEmpty()
                .doOnNext(str -> System.out.println("result " + str));

    }


}
