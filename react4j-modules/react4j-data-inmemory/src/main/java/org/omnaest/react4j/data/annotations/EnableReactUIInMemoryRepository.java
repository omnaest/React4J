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

import org.omnaest.react4j.data.provider.RepositoryProvider;
import org.omnaest.react4j.data.provider.memory.config.InMemoryRepositoryProviderConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Enables the in-memory {@link RepositoryProvider} of react4j-data-inmemory, which is part of the react4j-app-starter-parent. It is registered
 * unless another {@link RepositoryProvider} is already registered. The data lives in the memory of the application process only.
 * <br>
 * <br>
 * For a data grid backed provider see <code>EnableReactUIDataGridRepository</code> in the opt-in react4j-data-datagrid module.
 * <br>
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Import(InMemoryRepositoryProviderConfiguration.class)
public @interface EnableReactUIInMemoryRepository
{
}
