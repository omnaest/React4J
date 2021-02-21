package org.omnaest.react4j.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.omnaest.react4j.data.provider.RepositoryProvider;

/**
 * This enables the {@link ReactUI} support for a default {@link RepositoryProvider} based on the linked react4j-data-* dependency like e.g.
 * react4j-data-datagrid.
 * <br>
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EnableReactUIRepository
{
}
