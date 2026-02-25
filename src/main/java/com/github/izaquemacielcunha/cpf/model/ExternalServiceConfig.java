package com.github.izaquemacielcunha.cpf.model;

public record ExternalServiceConfig(
        String url,
        String token,
        String mountedUrl
) { }// end of class