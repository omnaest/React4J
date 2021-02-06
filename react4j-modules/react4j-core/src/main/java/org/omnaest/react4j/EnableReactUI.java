package org.omnaest.react4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.service.ReactUIService;

/**
 * This enables the {@link ReactUI} support in Spring.
 * <br>
 * <br>
 * For use please inject {@link ReactUIService} and create a {@link ReactUI} instance e.g. via {@link ReactUIService#getOrCreateDefaultRoot()}.
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EnableReactUI
{

}
