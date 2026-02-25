package com.github.izaquemacielcunha.cpf.model;

import java.time.LocalDateTime;

public record ValidateCpfResponseError(
        LocalDateTime timestamp,
        int errorCode,
        String errorMessage
) { }// end of class