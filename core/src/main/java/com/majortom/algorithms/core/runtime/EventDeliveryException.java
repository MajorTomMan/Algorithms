package com.majortom.algorithms.core.runtime;

/** Indicates failure of the authoritative event channel rather than algorithm logic. */
final class EventDeliveryException extends RuntimeException {

    EventDeliveryException(String message, RuntimeException cause) {
        super(message, cause);
    }
}
