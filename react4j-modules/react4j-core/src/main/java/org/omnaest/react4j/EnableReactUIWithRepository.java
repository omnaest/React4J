package org.omnaest.react4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.data.annotations.EnableReactUIRepository;

/**
 * This is a short cut which defines {@link EnableReactUI} and {@link EnableReactUIRepository}
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@EnableReactUI
@EnableReactUIInMemoryRepository
@Inherited
public @interface EnableReactUIWithRepository
{
}
