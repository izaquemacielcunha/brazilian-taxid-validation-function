package com.github.izaquemacielcunha.cpf.model;

public record ExternalServiceResponse(
        int status,
        String cpf,
        String nome,
        String erro,
        Integer erroCodigo
) { }// end of class