/*******************************************************************************
 * Copyright 2021 Danny Kunz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.omnaest.react4j.data.internal.configuration.ReactUIDataAutoConfiguration;
import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.springframework.context.annotation.Import;

/**
 * Enables the repository support of the ReactUI by component scanning the whole <code>org.omnaest.react4j.data</code> package tree of the classpath:
 * every <b>stereotyped</b> configuration found there is registered. With react4j-data-inmemory on the classpath (as it is on the
 * react4j-app-starter-parent) this registers the default in-memory {@link RepositoryProvider} unless another one is already registered.
 * <br>
 * <br>
 * This annotation does <b>not</b> enable the data grid provider of react4j-data-datagrid, even when that dependency is on the classpath: its
 * configuration is deliberately not stereotyped, so this scan cannot find it. To use the data grid provider, declare the react4j-data-datagrid
 * dependency and add <code>EnableReactUIDataGridRepository</code>.
 * <br>
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Import(ReactUIDataAutoConfiguration.class)
public @interface EnableReactUIRepository
{
}
