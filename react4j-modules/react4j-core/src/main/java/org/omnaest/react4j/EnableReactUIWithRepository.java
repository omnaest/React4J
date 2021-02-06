package org.omnaest.react4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.omnaest.react4j.data.EnableReactUIRepository;

/**
 * This is a short cut which defines {@link EnableReactUI} and {@link EnableReactUIRepository}
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EnableReactUI
@EnableReactUIRepository
@Inherited
public @interface EnableReactUIWithRepository
{
}
