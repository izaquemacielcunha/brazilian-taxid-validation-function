package com.github.izaquemacielcunha.cpf.model;

public record ExternalServiceResponseError(
        String status,
        int code,
        String message
) { }// end of class