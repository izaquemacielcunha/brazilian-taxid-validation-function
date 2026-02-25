package com.github.izaquemacielcunha.cpf.validation;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;

import com.github.izaquemacielcunha.cpf.model.ErrorCode;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class CpfValidator {
    private static final int CPF_LENGTH = 11;
    private static final int FIRST_VERIFIER_INDEX = 9;
    private static final int SECOND_VERIFIER_INDEX = 10;

    @Inject
    public CpfValidator() {}

    public List<ErrorCode> validate(String cpf) {
        List<ErrorCode> errors = new ArrayList<>();

        if (isNullOrBlank(cpf)) {
            errors.add(ErrorCode.CPF_NULL_BLANK);
            return errors;
        }

        String sanitizedCpf = sanitizeCpf(cpf);

        if (isNullOrBlank(sanitizedCpf)) {
            errors.add(ErrorCode.CPF_NULL_BLANK);
            return errors;
        }

        if (isInvalidLength(sanitizedCpf)) {
            errors.add(ErrorCode.CPF_INVALID_LENGTH);
            return errors;
        }

        if (isInvalidFormat(sanitizedCpf)) {
            errors.add(ErrorCode.CPF_ALL_DIGITS_EQUAL);
            return errors;
        }

        if (isInvalidChecksum(sanitizedCpf)) {
            errors.add(ErrorCode.CPF_INVALID);
            return errors;
        }

        return errors;
    }

    private boolean isNullOrBlank(String cpf) {
        return isNull(cpf) || cpf.isBlank();
    }

    public String sanitizeCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }

    private boolean isInvalidLength(String cpf) {
        return cpf.length() != CPF_LENGTH;
    }

    private boolean isInvalidFormat(String cpf) {
        return cpf.chars().distinct().count() == 1;
    }

    private boolean isInvalidChecksum(String cpf) {
        String firstSequence = cpf.substring(0, 9);
        String calculatedFirstDigit = calculateVerifierDigit(firstSequence);

        if (!calculatedFirstDigit.equals(String.valueOf(cpf.charAt(FIRST_VERIFIER_INDEX)))) {
            return true;
        }

        String secondSequence = cpf.substring(1, 10);
        String calculatedSecondDigit = calculateVerifierDigit(secondSequence);

        return !calculatedSecondDigit.equals(String.valueOf(cpf.charAt(SECOND_VERIFIER_INDEX)));
    }

    private String calculateVerifierDigit(String sequence) {
        int sum = calculateWeightedSum(sequence);

        return getDigitFromSum(sum);
    }

    private int calculateWeightedSum(String cpf) {
        int counter = 10 ;
        int sum = 0;

        for (char number : cpf.toCharArray()) {
            sum += (number - '0') * counter;
            counter--;
        }

        return sum;
    }

    private String getDigitFromSum(int sum) {
        int remainder = sum % CPF_LENGTH;

        if (remainder < 2) {
            return "0";
        }

        return String.valueOf(CPF_LENGTH - remainder);
    }

}// end of class