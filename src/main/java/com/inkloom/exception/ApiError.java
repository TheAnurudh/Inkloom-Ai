package com.inkloom.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/** JSON shape of EVERY error response. requestId matches the [rid] printed in the console log. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String error, String message,
                       String path, String requestId, Map<String, String> fieldErrors) { }
