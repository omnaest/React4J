package org.omnaest.react4j.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This enables the {@link ReactUI} support for a default {@link RepositoryProvider} based on the linked react4j-data-* dependency like e.g.
 * react4j-data-datagrid.
 * <br>
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableReactUIRepository
{
}
