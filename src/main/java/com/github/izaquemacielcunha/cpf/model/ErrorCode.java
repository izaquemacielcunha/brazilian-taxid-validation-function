package com.github.izaquemacielcunha.cpf.model;

public enum ErrorCode {

    CPF_NULL_BLANK(99, "CPF is required"),
    CPF_INVALID(100, "Invalid CPF"),
    CPF_INVALID_LENGTH(101, "CPF must have 11 digits"),
    CPF_NOT_FOUND(102, "CPF not found in Federal Revenue database"),
    CPF_ALL_DIGITS_EQUAL(150, "CPF cannot contain all identical digits"),
    INVALID_PARAMETERS(400, "Incorrect parameters"),
    INVALID_TOKEN(1000, "Invalid token. Token does not match the source IP"),
    INSUFFICIENT_CREDITS(1001, "Insufficient credits for the selected package"),
    ACCOUNT_SUSPENDED_OR_INACTIVE(1002, "Account suspended or inactive. Please contact support"),
    TEMPORARY_BLACKLIST(1003, "IP and token temporarily blacklisted"),
    INVALID_OR_UNAVAILABLE_PACKAGE(1004, "Invalid or unavailable package ID"),
    PACKAGE_NOT_ALLOWED_FOR_DOCUMENT(1005, "CPF not allowed for this package"),
    SUPPLIER_OFFLINE(1006, "Data provider is currently offline"),
    RATE_LIMIT_EXCEEDED(1007, "Rate limit exceeded. Maximum 20 requests per second"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
    UNMAPPED_ERROR(0, "Unmapped error code");

    private final int code;
    private final String description;

    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return UNMAPPED_ERROR;
    }

}// end of class
