package com.majortom.algorithms.server.dto;

import java.time.Instant;

/** Stable HTTP error payload for the algorithm execution API. */
public record ErrorResponse(Instant timestamp, int status, String code, String message, String path) {
}
