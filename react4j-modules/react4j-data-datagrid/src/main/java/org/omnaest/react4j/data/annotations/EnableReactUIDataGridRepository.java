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

import org.omnaest.react4j.data.datagrid.internal.config.DataGridRepositoryProviderConfiguration;
import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.springframework.context.annotation.Import;

/**
 * Opts in to the data grid backed {@link RepositoryProvider} of react4j-data-datagrid.
 * <br>
 * <br>
 * This is <b>opt-in</b>, in two ways:
 * <ul>
 * <li>The react4j-data-datagrid dependency is <b>not</b> part of the react4j-app-starter-parent. An application that wants this provider has to declare
 * the react4j-data-datagrid dependency itself. Without that dependency this annotation does not exist on the classpath.</li>
 * <li>Neither {@link EnableReactUIRepository} nor any other annotation registers the data grid provider. Only this annotation does, so an
 * application that has the dependency but does not use the annotation never starts a data grid.</li>
 * </ul>
 * <b>This starts an embedded data grid</b> (an Apache Ignite node, via CommonsDataGrid) inside the application process when the application context is
 * created, and shuts it down when the context is closed. The default {@link RepositoryProvider} of the react4j-app-starter-parent is the in-memory one
 * (see {@code EnableReactUIInMemoryRepository} in react4j-data-inmemory). When both are present, the data grid provider is the preferred one.
 * <br>
 * <br>
 * <b>Runtime requirements of the embedded node</b> (observed when this module's own tests start a grid, not guaranteed by the library):
 * <ul>
 * <li><b>JVM options.</b> The JVM of an application using this annotation must be started with the {@code --add-opens} set that CommonsDataGrid itself
 * uses for its own tests, declared in the surefire {@code argLine} of the CommonsDataGrid {@code pom.xml}: {@code java.base/java.util},
 * {@code java.base/java.lang}, {@code java.base/java.lang.reflect}, {@code java.base/java.nio}, {@code java.base/sun.nio.ch} and
 * {@code java.base/java.io}, each to {@code ALL-UNNAMED}, plus {@code -Dio.netty.tryReflectionSetAccessible=true}. Without them the node fails to start with
 * {@code NoClassDefFoundError: Could not initialize class ...SerializableInstantiation}. A plain {@code java -jar} start of a hosted application has to
 * pass these options explicitly.</li>
 * <li><b>Fixed ports.</b> The embedded node binds fixed ports (observed: 47500, 10800 and 10300), they are not configurable through this annotation. Two data
 * grids on one machine, for example two applications that both use this annotation or a leftover node of a previous run, collide on them.</li>
 * </ul>
 *
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Import(DataGridRepositoryProviderConfiguration.class)
public @interface EnableReactUIDataGridRepository
{
}
