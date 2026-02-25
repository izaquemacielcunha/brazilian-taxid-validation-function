package com.github.izaquemacielcunha.cpf.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.izaquemacielcunha.cpf.model.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class CpfValidatorTest {
    private CpfValidator cpfValidator;

    @BeforeEach
    void setUp() {
        cpfValidator = new CpfValidator();
    }

    @Test
    void shouldReturnValidCpf() {
        String validCpf = "795.348.710-10";
        List<ErrorCode> errors = cpfValidator.validate(validCpf);

        assertEquals(0, errors.size());
    }

    @Test
    void shouldReturnValidCpf_WithoutCharacters() {
        String validCpf = "79534871010";
        List<ErrorCode> errors = cpfValidator.validate(validCpf);

        assertEquals(0, errors.size());
    }

    @Test
    void shouldReturnNullBlankCpf_NullCpf() {
        List<ErrorCode> errors = cpfValidator.validate(null);

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_NULL_BLANK, errors.getFirst());
    }

    @Test
    void shouldReturnNullBlank_EmptyCpf() {
        List<ErrorCode> errors = cpfValidator.validate("");

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_NULL_BLANK, errors.getFirst());
    }

    @Test
    void shouldReturnNullBlank_EmptyCpfAfterSanitization() {
        List<ErrorCode> errors = cpfValidator.validate("att.ata.etd-ee");

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_NULL_BLANK, errors.getFirst());
    }

    @Test
    void shouldReturnInvalidLength_WrongCpfLength() {
        String invalidCpf = "591.112.260-054";
        List<ErrorCode> errors = cpfValidator.validate(invalidCpf);

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_INVALID_LENGTH, errors.getFirst());
    }

    @Test
    void shouldReturnAllDigitsEqual_InvalidCpfSequence() {
        String invalidCpf = "444.444.444-44";
        List<ErrorCode> errors = cpfValidator.validate(invalidCpf);

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_ALL_DIGITS_EQUAL, errors.getFirst());
    }

    @Test
    void shouldReturnInValidCpf_EightInvalidDigitCpf() {
        String invalidCpf = "591.112.260-15";
        List<ErrorCode> errors = cpfValidator.validate(invalidCpf);

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_INVALID, errors.getFirst());
    }

    @Test
    void shouldReturnInValidCpf_ninthInvalidDigitCpf() {
        String invalidCpf = "591.112.260-03";

        List<ErrorCode> errors = cpfValidator.validate(invalidCpf);

        assertEquals(1, errors.size());
        assertEquals(ErrorCode.CPF_INVALID, errors.getFirst());
    }

}// end of class
