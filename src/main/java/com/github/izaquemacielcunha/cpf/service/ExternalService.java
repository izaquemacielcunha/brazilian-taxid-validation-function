package com.github.izaquemacielcunha.cpf.service;

import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponse;

public interface ExternalService {
    ExternalServiceResponse call(String url) throws Exception;

}// end of interface
