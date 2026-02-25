package com.github.izaquemacielcunha.cpf.function;

import java.util.List;
import java.util.Optional;

import com.github.izaquemacielcunha.cpf.model.ErrorCode;
import com.github.izaquemacielcunha.cpf.model.ExternalServiceConfig;
import com.github.izaquemacielcunha.cpf.model.mapper.ValidateCpfResponseMapper;
import com.github.izaquemacielcunha.cpf.service.ExternalService;
import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponse;
import com.github.izaquemacielcunha.cpf.validation.CpfValidator;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;


public class ValidateCpf {
    private static final Logger logger = LoggerFactory.getLogger(ValidateCpf.class);

    private static final int ERROR = 0;

    private final CpfValidator cpfValidator;
    private final ExternalService externalService;
    private final ValidateCpfResponseMapper mapper;
    private final ExternalServiceConfig config;

    @Inject
    public ValidateCpf(
            CpfValidator cpfValidator, ExternalService externalService,
            ValidateCpfResponseMapper mapper, ExternalServiceConfig config) {
        this.cpfValidator = cpfValidator;
        this.externalService = externalService;
        this.mapper = mapper;
        this.config = config;
    }

    @FunctionName("ValidateCpf")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws Exception {

        String cpf = request.getQueryParameters().get("cpf");

        logger.debug("[ValidateCpf] - Received request to validate CPF {}", cpf);

        List<ErrorCode> errors = cpfValidator.validate(cpf);

        if (!errors.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(mapper.mapToValidateCpfResponseErrorJson(errors.getFirst()))
                    .build();
        }

        if (config.url().isBlank() || config.token().isBlank() || config.mountedUrl().isBlank()) {
            logger.error("[ValidateCpf] - Missing API environment variables");

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(mapper.mapToValidateCpfResponseErrorJson(ErrorCode.INTERNAL_SERVER_ERROR))
                    .build();
        }

        ExternalServiceResponse response;

        try {
            response = externalService.call(config.mountedUrl() + cpfValidator.sanitizeCpf(cpf));
        }
        catch (Exception e) {
            logger.error("[ValidateCpf] - Error calling external service. Error: {}", e.getMessage(), e);

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(mapper.mapToValidateCpfResponseErrorJson(ErrorCode.INTERNAL_SERVER_ERROR))
                    .build();
        }

        if (response.status() == ERROR) {

            ErrorCode errorCode = ErrorCode.fromCode(response.erroCodigo());

            if (errorCode == ErrorCode.UNMAPPED_ERROR) {
                logger.error("[ValidateCpf] - Unmapped error from external service. Code: {}. Description: {}", response.erroCodigo(), response.erro());

                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header("Content-Type", "application/json")
                        .body(mapper.mapToValidateCpfResponseErrorJson(ErrorCode.INTERNAL_SERVER_ERROR))
                        .build();
            }

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body(mapper.mapToValidateCpfResponseErrorJson(errorCode))
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(mapper.mapToValidateCpfResponseSuccessfulJson(response.cpf(), response.nome()))
                .build();
    }

}// end of class
