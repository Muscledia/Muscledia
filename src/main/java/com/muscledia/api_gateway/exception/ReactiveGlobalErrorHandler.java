package com.muscledia.api_gateway.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Order(-2) // Higher precedence than default error handler
public class ReactiveGlobalErrorHandler implements ErrorWebExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveGlobalErrorHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // Log the error
        logger.error("Gateway error occurred: ", ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "Internal Server Error";

        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            errorMessage = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex.getCause() instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorMessage = "Service temporarily unavailable";
        } else if (ex.getCause() instanceof java.util.concurrent.TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            errorMessage = "Request timeout";
        }

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"timestamp\":\"%s\",\"path\":\"%s\"}",
                errorMessage,
                status.value(),
                Instant.now().toString(),
                exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
