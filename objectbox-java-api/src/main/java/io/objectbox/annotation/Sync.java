package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables sync for an {@link Entity} class.
 * <p>
 * Note that currently sync can not be enabled or disabled for existing entities.
 * Also synced entities can not have relations to non-synced entities.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Sync {
}
