package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestConfig {
    @Bean
    public RestTemplate restTemplate() {
//        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.getInterceptors().add((request, body, execution) ->{
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if(authentication != null && authentication.getPrincipal() instanceof Jwt){
//                Jwt jwt = (Jwt) authentication.getPrincipal();
//                String tokenValue = jwt.getTokenValue();
//                System.out.println("tokenValue: " + tokenValue);
//                request.getHeaders().setBearerAuth(tokenValue);
//            }
//            return execution.execute(request, body);
//        });
//        return restTemplate;

        RestTemplate restTemplate = new RestTemplate();
        ClientHttpRequestInterceptor interceptor = (request, body, execution) ->{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if(authentication != null && authentication.getPrincipal() instanceof Jwt jwt){
                String token = jwt.getTokenValue();
                request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(List.of(interceptor));
        return restTemplate;
    }
}
