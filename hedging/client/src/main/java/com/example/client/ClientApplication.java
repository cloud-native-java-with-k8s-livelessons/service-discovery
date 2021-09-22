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
    private final LoadBalancerClient loadBalancerClient;
    private final DiscoveryClient discoveryClient;

    ClientRestController(DiscoveryClient dc, WebClient http, LoadBalancerClient loadBalancerClient) {
        this.http = http;
        this.discoveryClient = dc;
        this.loadBalancerClient = loadBalancerClient;
    }

    @GetMapping("/client")
    Mono<String> helloClient() {

        var max = 2;
        var serviceInstances = this.discoveryClient.getInstances("service");
        Assert.state(serviceInstances.size() >= 2, () -> "there should be " +
                                                         "at least two instances in the load balancer!");
        var chosenServiceInstances = new HashSet<ServiceInstance>();
        while (chosenServiceInstances.size() < max) {
            var randomIndex = Math.random() * serviceInstances.size();
            var index = (int) Math.min(randomIndex * 1, serviceInstances.size() - 1);
            chosenServiceInstances.add(serviceInstances.get(index));
            System.out.println("there are " + chosenServiceInstances.size() + " elements.");
        }
        var requests = Flux
                .fromIterable(chosenServiceInstances)
                .flatMap(this::withServiceInstance);
        return Flux.firstWithSignal(requests)
                .take(1)
                .singleOrEmpty()
                .doOnNext(s -> System.out.println("result " + s));

    }

    Mono<String> withServiceInstance(ServiceInstance si) {
        return http.get().uri(si.getScheme() + "://" + si.getHost() + ':' + si.getPort() + "/hello").retrieve().bodyToMono(String.class);
    }
}
