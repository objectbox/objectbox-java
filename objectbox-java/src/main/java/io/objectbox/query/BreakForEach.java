package io.objectbox.query;

/**
 * You can throw this inside a {@link QueryConsumer} to signal {@link Query#forEach(QueryConsumer)} should "break".
 * This will stop consuming any further data.
 */
public class BreakForEach extends RuntimeException {
}
