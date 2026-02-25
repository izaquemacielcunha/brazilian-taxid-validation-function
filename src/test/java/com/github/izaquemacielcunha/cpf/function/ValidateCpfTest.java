package com.github.izaquemacielcunha.cpf.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.github.izaquemacielcunha.cpf.model.ErrorCode;
import com.github.izaquemacielcunha.cpf.model.ExternalServiceConfig;
import com.github.izaquemacielcunha.cpf.model.ExternalServiceResponse;
import com.github.izaquemacielcunha.cpf.model.mapper.ValidateCpfResponseMapper;
import com.github.izaquemacielcunha.cpf.service.ExternalService;
import com.github.izaquemacielcunha.cpf.validation.CpfValidator;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.LoggerFactory;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ValidateCpfTest {

    @Spy
    private CpfValidator cpfValidator = new CpfValidator();

    @Mock
    private ExternalService externalService;

    @Spy
    private ValidateCpfResponseMapper mapper = new ValidateCpfResponseMapper(new ObjectMapper());

    private ExternalServiceConfig config;

    @Mock
    private HttpRequestMessage<Optional<String>> request;

    @Mock
    private HttpResponseMessage.Builder responseBuilder;

    @Mock
    private HttpResponseMessage response;

    @Mock
    private ExecutionContext context;

    @Captor
    private ArgumentCaptor<HttpStatus> httpStatusCaptor;

    @Captor
    private ArgumentCaptor<ErrorCode> errorCodeCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Captor
    private ArgumentCaptor<Throwable> exceptionCaptor;

    private Logger realLogger;

    private ListAppender<ILoggingEvent> listAppender;

    private Map<String, String> queryParams;

    private ValidateCpf validateCpf;

    @BeforeEach
    void setUp() {
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.header(any(), any())).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);

        realLogger = (Logger) LoggerFactory.getLogger(ValidateCpf.class);
        realLogger.setLevel(Level.TRACE);
        listAppender = new ListAppender<>();
        listAppender.start();
        realLogger.addAppender(listAppender);

        config = new ExternalServiceConfig(
                "https://api.cpfcnpj.com.br/{token}/1/",
                "5ae973d7a997af13f0aaf2bf60e65803",
                "https://api.cpfcnpj.com.br/5ae973d7a997af13f0aaf2bf60e65803/1/"
        );
        queryParams = new HashMap<>();
        validateCpf = new ValidateCpf(cpfValidator, externalService, mapper, config);
    }

    @Test
    void shouldReturnOk_ValidCpf() throws Exception {
        String cpf = "42400469040";
        ExternalServiceResponse externalServiceResponse = new ExternalServiceResponse(1, "424.004.690-40", "Keanu Reeves", null, null);

        queryParams.put("cpf", cpf);
        when(request.getQueryParameters()).thenReturn(queryParams);

        when(externalService.call(anyString())).thenReturn(
                externalServiceResponse
        );

        HttpResponseMessage httpResponse = validateCpf.run(request, context);

        verify(request).getQueryParameters();

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(1, logsCaptured.size());
        assertEquals(Level.DEBUG, logsCaptured.getFirst().getLevel());
        assertEquals("[ValidateCpf] - Received request to validate CPF " + cpf,
                logsCaptured.getFirst().getFormattedMessage());

        verify(cpfValidator).validate(stringCaptor.capture());
        assertEquals(cpf, stringCaptor.getValue());

        verify(externalService).call(stringCaptor.capture());
        assertEquals(config.mountedUrl() + cpf, stringCaptor.getValue());

        verify(request).createResponseBuilder(httpStatusCaptor.capture());
        assertEquals(HttpStatus.OK, httpStatusCaptor.getValue());

        verify(responseBuilder).header(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of("Content-Type", "application/json")));

        verify(mapper).mapToValidateCpfResponseSuccessfulJson(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of(externalServiceResponse.cpf(), externalServiceResponse.cpf())));

        assertEquals(response, httpResponse);
    }

    @Test
    void shouldReturnBadRequest_NullCpf() throws Exception {
        queryParams.put("cpf", null);
        when(request.getQueryParameters()).thenReturn(queryParams);

        HttpResponseMessage httpResponse = validateCpf.run(request, context);

        verify(request).getQueryParameters();

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(1, logsCaptured.size());
        assertEquals(Level.DEBUG, logsCaptured.getFirst().getLevel());
        assertEquals("[ValidateCpf] - Received request to validate CPF " + null,
                logsCaptured.getFirst().getFormattedMessage());

        verify(cpfValidator).validate(stringCaptor.capture());
        assertNull(stringCaptor.getValue());

        verify(request).createResponseBuilder(httpStatusCaptor.capture());
        assertEquals(HttpStatus.BAD_REQUEST, httpStatusCaptor.getValue());

        verify(responseBuilder).header(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of("Content-Type", "application/json")));

        verify(mapper).mapToValidateCpfResponseErrorJson(errorCodeCaptor.capture());
        assertEquals(ErrorCode.CPF_NULL_BLANK, errorCodeCaptor.getValue());

        assertEquals(response, httpResponse);
    }

    @Test
    void shouldReturnBadRequest_NoEnvironmentConfigs() throws Exception {
        String cpf = "306.343.120-65";
        config = new ExternalServiceConfig(
                "",
                "",
                ""
        );
        validateCpf = new ValidateCpf(cpfValidator, externalService, mapper, config);

        queryParams.put("cpf", cpf);
        when(request.getQueryParameters()).thenReturn(queryParams);

        HttpResponseMessage httpResponse = validateCpf.run(request, context);

        verify(request).getQueryParameters();

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(2, logsCaptured.size());

        ILoggingEvent debugLog = logsCaptured.stream().filter(event -> event.getLevel().equals(Level.DEBUG)).findFirst().get();
        assertEquals(Level.DEBUG, debugLog.getLevel());
        assertEquals("[ValidateCpf] - Received request to validate CPF " + cpf, debugLog.getFormattedMessage());

        ILoggingEvent errorLog = logsCaptured.stream().filter(event -> event.getLevel().equals(Level.ERROR)).findFirst().get();
        assertEquals(Level.ERROR, errorLog.getLevel());
        assertEquals("[ValidateCpf] - Missing API environment variables", errorLog.getFormattedMessage());

        verify(cpfValidator).validate(stringCaptor.capture());
        assertEquals(cpf, stringCaptor.getValue());

        verify(request).createResponseBuilder(httpStatusCaptor.capture());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatusCaptor.getValue());

        verify(responseBuilder).header(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of("Content-Type", "application/json")));

        verify(mapper).mapToValidateCpfResponseErrorJson(errorCodeCaptor.capture());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, errorCodeCaptor.getValue());

        assertEquals(response, httpResponse);
    }

    @Test
    void shouldReturnInternalServerError_ExternalServiceThrowException() throws Exception {
        String cpf = "42400469040";

        queryParams.put("cpf", cpf);
        when(request.getQueryParameters()).thenReturn(queryParams);

        RuntimeException exception =  new RuntimeException("Connection timeout");
        when(externalService.call(anyString())).thenThrow(exception);

        HttpResponseMessage httpResponse = validateCpf.run(request, context);

        verify(request).getQueryParameters();

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(2, logsCaptured.size());

        ILoggingEvent debugLog = logsCaptured.stream().filter(event -> event.getLevel().equals(Level.DEBUG)).findFirst().get();
        assertEquals(Level.DEBUG, debugLog.getLevel());
        assertEquals("[ValidateCpf] - Received request to validate CPF " + cpf, debugLog.getFormattedMessage());

        ILoggingEvent errorLog = logsCaptured.stream().filter(event -> event.getLevel().equals(Level.ERROR)).findFirst().get();
        assertEquals(Level.ERROR, errorLog.getLevel());
        assertEquals("[ValidateCpf] - Error calling external service. Error: Connection timeout", errorLog.getFormattedMessage());

        verify(cpfValidator).validate(stringCaptor.capture());
        assertEquals(cpf, stringCaptor.getValue());

        verify(externalService).call(stringCaptor.capture());
        assertEquals(config.mountedUrl() + cpf, stringCaptor.getValue());

        verify(request).createResponseBuilder(httpStatusCaptor.capture());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatusCaptor.getValue());

        verify(responseBuilder).header(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of("Content-Type", "application/json")));

        verify(mapper).mapToValidateCpfResponseErrorJson(errorCodeCaptor.capture());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, errorCodeCaptor.getValue());

        assertEquals(response, httpResponse);
    }

    @Test
    void shouldReturnInternalServerError_ExternalServiceUnmappedError() throws Exception {
        String cpf = "42400469040";

        queryParams.put("cpf", cpf);
        when(request.getQueryParameters()).thenReturn(queryParams);

        ExternalServiceResponse externalServiceResponse = new ExternalServiceResponse(
                0, null, null, "Generic error", 22
        );
        when(externalService.call(anyString())).thenReturn(externalServiceResponse);

        HttpResponseMessage httpResponse = validateCpf.run(request, context);

        verify(request).getQueryParameters();

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(2, logsCaptured.size());

        ILoggingEvent debugLog = logsCaptured.stream().filter(event -> event.getLevel().equals(Level.DEBUG)).findFirst().get();
        assertEquals(Level.DEBUG, debugLog.getLevel());
        assertEquals("[ValidateCpf] - Received request to validate CPF " + cpf, debugLog.getFormattedMessage());

        ILoggingEvent errorLog = logsCaptured.stream().filter(event -> event.getLevel().equals(Level.ERROR)).findFirst().get();
        assertEquals(Level.ERROR, errorLog.getLevel());
        assertEquals("[ValidateCpf] - Unmapped error from external service. Code: 22. Description: Generic error", errorLog.getFormattedMessage());

        verify(cpfValidator).validate(stringCaptor.capture());
        assertEquals(cpf, stringCaptor.getValue());

        verify(externalService).call(stringCaptor.capture());
        assertEquals(config.mountedUrl() + cpf, stringCaptor.getValue());

        verify(request).createResponseBuilder(httpStatusCaptor.capture());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, httpStatusCaptor.getValue());

        verify(responseBuilder).header(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of("Content-Type", "application/json")));

        verify(mapper).mapToValidateCpfResponseErrorJson(errorCodeCaptor.capture());
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, errorCodeCaptor.getValue());

        assertEquals(response, httpResponse);
    }

    @Test
    void shouldReturnInternalServerError_ExternalServiceCpfNotFound() throws Exception {
        String cpf = "35215907048";

        queryParams.put("cpf", cpf);
        when(request.getQueryParameters()).thenReturn(queryParams);

        ExternalServiceResponse externalServiceResponse = new ExternalServiceResponse(
                0, "", "", "CPF válido, mas não consta em bases da Receita", 102
        );
        when(externalService.call(anyString())).thenReturn(externalServiceResponse);

        HttpResponseMessage httpResponse = validateCpf.run(request, context);

        verify(request).getQueryParameters();

        List<ILoggingEvent> logsCaptured = listAppender.list;
        assertEquals(1, logsCaptured.size());
        assertEquals(Level.DEBUG, logsCaptured.getFirst().getLevel());
        assertEquals("[ValidateCpf] - Received request to validate CPF " + cpf, logsCaptured.getFirst().getFormattedMessage());

        verify(cpfValidator).validate(stringCaptor.capture());
        assertEquals(cpf, stringCaptor.getValue());

        verify(externalService).call(stringCaptor.capture());
        assertEquals(config.mountedUrl() + cpf, stringCaptor.getValue());

        verify(request).createResponseBuilder(httpStatusCaptor.capture());
        assertEquals(HttpStatus.BAD_REQUEST, httpStatusCaptor.getValue());

        verify(responseBuilder).header(stringCaptor.capture(), stringCaptor.capture());
        assertTrue(stringCaptor.getAllValues().containsAll(List.of("Content-Type", "application/json")));

        verify(mapper).mapToValidateCpfResponseErrorJson(errorCodeCaptor.capture());
        assertEquals(ErrorCode.CPF_NOT_FOUND, errorCodeCaptor.getValue());

        assertEquals(response, httpResponse);
    }

    @AfterEach
    void tearDown() {
        realLogger.detachAppender(listAppender);
    }

}// end of class
