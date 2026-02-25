package com.github.izaquemacielcunha.cpf.model.mapper;

import java.time.LocalDateTime;

import com.github.izaquemacielcunha.cpf.model.ErrorCode;
import com.github.izaquemacielcunha.cpf.model.ValidateCpfResponseError;
import com.github.izaquemacielcunha.cpf.model.ValidateCpfResponse;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import tools.jackson.databind.ObjectMapper;

@Singleton
public class ValidateCpfResponseMapper {
    private final ObjectMapper mapper;

    @Inject
    public ValidateCpfResponseMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String mapToValidateCpfResponseErrorJson(ErrorCode errorCode) {

        ValidateCpfResponseError error = new ValidateCpfResponseError(
                LocalDateTime.now(),
                errorCode.getCode(),
                errorCode.getDescription()
        );

        return mapper.writeValueAsString(error);
    }

    public String mapToValidateCpfResponseSuccessfulJson(String cpf, String nome) {
        return mapper.writeValueAsString(new ValidateCpfResponse(
                cpf,
                nome
        ));
    }

}// end of class