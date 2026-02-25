package com.github.izaquemacielcunha.cpf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponseError;
import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponse;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import tools.jackson.databind.ObjectMapper;

import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ExternalServiceImplTest {

    @Mock
    private HttpClient httpClient;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private HttpResponse<String> response;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<Class<?>> classCaptor;

    private Logger realLogger;

    private ListAppender<ILoggingEvent> listAppender;

    private ExternalService service;

    @BeforeEach
    void setUp() {
        service = new ExternalServiceImpl(httpClient, mapper);

        realLogger = (Logger) LoggerFactory.getLogger(ExternalServiceImpl.class);
        realLogger.setLevel(Level.TRACE);
        listAppender = new ListAppender<>();
        listAppender.start();
        realLogger.addAppender(listAppender);
    }

    @Test
    void shouldReturnExternalServiceResponse_Ok() throws Exception {
        String url = "https://api.cpfcnpj.com.br/5ae973d7a997af13f0aaf2bf60e65803/1/65557598014";
        String externalServiceJsonResponse = """
                    {
                        "status": 1,
                        "cpf": "655.575.980-14",
                        "nome": "keanu reeves",
                        "pacoteUsado": 1,
                        "saldo": 123,
                        "consultaID": "11bb22cc33dd44ee",
                        "delay": 0.3
                    }
                    """;

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(externalServiceJsonResponse);

        ExternalServiceResponse externalServiceResponse = service.call(url);

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(1, logsCaptured.size());
        assertEquals(Level.DEBUG, logsCaptured.getFirst().getLevel());
        assertEquals("[ValidateCpf] - Response from external service: " + externalServiceJsonResponse,
                logsCaptured.getFirst().getFormattedMessage());

        verify(mapper).readValue(stringCaptor.capture(), classCaptor.capture());
        assertEquals(externalServiceJsonResponse, stringCaptor.getValue());
        assertEquals(ExternalServiceResponse.class, classCaptor.getValue());

        assertEquals(1, externalServiceResponse.status());
        assertEquals("655.575.980-14",externalServiceResponse.cpf());
        assertEquals("keanu reeves", externalServiceResponse.nome());
        assertNull(externalServiceResponse.erro());
        assertNull(externalServiceResponse.erroCodigo());
    }

    @Test
    void shouldReturnExternalServiceResponse_CpfNotFound() throws Exception {
        String url = "https://api.cpfcnpj.com.br/5ae973d7a997af13f0aaf2bf60e65803/1/65557598014";
        String externalServiceJsonResponse = """
                    {
                        "status": 0,
                        "cpf": "",
                        "nome": null,
                        "erro": "CPF válido, mas não consta em bases da Receita.",
                        "pacoteUsado": 1,
                        "erroCodigo": 102
                    }
                    """;

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(externalServiceJsonResponse);

        ExternalServiceResponse externalServiceResponse = service.call(url);

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(1, logsCaptured.size());
        assertEquals(Level.DEBUG, logsCaptured.getFirst().getLevel());
        assertEquals("[ValidateCpf] - Response from external service: " + externalServiceJsonResponse,
                logsCaptured.getFirst().getFormattedMessage());

        verify(mapper).readValue(stringCaptor.capture(), classCaptor.capture());
        assertEquals(externalServiceJsonResponse, stringCaptor.getValue());
        assertEquals(ExternalServiceResponse.class, classCaptor.getValue());

        assertEquals(0, externalServiceResponse.status());
        assertEquals("",  externalServiceResponse.cpf());
        assertNull(externalServiceResponse.nome());
        assertEquals(102, externalServiceResponse.erroCodigo());
        assertEquals("CPF válido, mas não consta em bases da Receita.", externalServiceResponse.erro());
    }

    @Test
    void shouldReturnExternalServiceResponse_GenericError() throws Exception {
        String url = "https://api.cpfcnpj.com.br/5ae973d7a997af13f0aaf2bf60e65803/1/65557598014";
        String externalServiceJsonGenericErrorResponse = """
                    {
                        "status": "error",
                        "code": 400,
                        "message": "Incorrect parameters. For more information..."
                    }
                    """;

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.statusCode()).thenReturn(400);
        when(response.body()).thenReturn(externalServiceJsonGenericErrorResponse);

        ExternalServiceResponse externalServiceResponse = service.call(url);

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(1, logsCaptured.size());
        assertEquals(Level.DEBUG, logsCaptured.getFirst().getLevel());
        assertEquals("[ValidateCpf] - Response from external service: " + externalServiceJsonGenericErrorResponse,
                logsCaptured.getFirst().getFormattedMessage());

        verify(mapper).readValue(stringCaptor.capture(), classCaptor.capture());
        assertEquals(externalServiceJsonGenericErrorResponse, stringCaptor.getValue());
        assertEquals(ExternalServiceResponseError.class, classCaptor.getValue());

        assertEquals(0, externalServiceResponse.status());
        assertNull(externalServiceResponse.cpf());
        assertNull(externalServiceResponse.nome());
        assertEquals(400, externalServiceResponse.erroCodigo());
        assertEquals("Incorrect parameters. For more information...", externalServiceResponse.erro());
    }

    @AfterEach
    void tearDown() {
        realLogger.detachAppender(listAppender);
    }

}// end of class

