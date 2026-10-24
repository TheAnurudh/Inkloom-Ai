package com.inkloom.exception;

import org.springframework.http.HttpStatus;

/** Business-rule error that is safe to show to the client. Use the static helpers. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static ApiException badRequest(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    public static ApiException forbidden(String m)  { return new ApiException(HttpStatus.FORBIDDEN, m); }
    public static ApiException notFound(String m)   { return new ApiException(HttpStatus.NOT_FOUND, m); }
    public static ApiException conflict(String m)   { return new ApiException(HttpStatus.CONFLICT, m); }
}
