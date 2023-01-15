package com.example.apigateway.filter;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Value("${config.auth-service-url}")
    private String authServiceUrl;
    @Value("${config.job-service-url}")
    private String jobServiceUrl;


    public AuthenticationFilter() {
        super(Config.class);
    }

    private boolean isAuthorizationValid(String authorizationHeader) throws IOException {

        //String token = authorizationHeader.replace("Bearer ", "");



        URL url = new URL(authServiceUrl + "/valid");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", authorizationHeader);

        int status = con.getResponseCode();

        return status == 200;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus)  {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);

        return response.setComplete();
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey("Authorization")) {
                return this.onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
            };

            String authorizationHeader = request.getHeaders().get("Authorization").get(0);

            try {
                if (!this.isAuthorizationValid(authorizationHeader)) {
                    return this.onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Put the configuration properties
    }
}