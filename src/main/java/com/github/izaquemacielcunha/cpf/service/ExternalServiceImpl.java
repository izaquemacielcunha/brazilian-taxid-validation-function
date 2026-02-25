package com.github.izaquemacielcunha.cpf.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponse;
import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponseError;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Singleton
public class ExternalServiceImpl implements ExternalService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceImpl.class);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    @Inject
    public ExternalServiceImpl(HttpClient httpClient, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public ExternalServiceResponse call(String url) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        logger.debug("[ValidateCpf] - Response from external service: {}",  response.body());

        if (isGenericErrorResponse(response)) {
            ExternalServiceResponseError error = mapper.readValue(response.body(), ExternalServiceResponseError.class);
            return new ExternalServiceResponse(0, null, null, error.message(), error.code());
        }

        return mapper.readValue(response.body(), ExternalServiceResponse.class);
    }

    private boolean isGenericErrorResponse(HttpResponse<String> response) {
        JsonNode rootNode = mapper.readTree(response.body());
        return response.statusCode() != 200 && rootNode.has("message");
    }

}// end of class
