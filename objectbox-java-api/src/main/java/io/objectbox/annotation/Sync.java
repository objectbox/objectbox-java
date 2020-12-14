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

    /**
     * Set to {@code true} to enable shared global IDs for a {@link Sync}-enabled {@link Entity} class.
     * <p>
     * By default each Sync client has its own local {@link Id ID} space for Objects.
     * IDs are mapped to global IDs when syncing behind the scenes. Turn this on
     * to treat Object IDs as global and turn of ID mapping. The ID of an Object will
     * then be the same on all clients.
     * <p>
     * When using this, it is recommended to use {@link Id#assignable() assignable IDs}
     * to turn off automatically assigned IDs. Without special care, two Sync clients are
     * likely to overwrite each others Objects if IDs are assigned automatically.
     */
    boolean sharedGlobalIds() default false;
}
