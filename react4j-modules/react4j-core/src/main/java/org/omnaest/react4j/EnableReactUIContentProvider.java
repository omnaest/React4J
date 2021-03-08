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
package org.omnaest.react4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.omnaest.react4j.domain.ReactUI;
import org.omnaest.react4j.service.ReactUIService;

/**
 * This enables the content provider support in Spring for the {@link ReactUI}.<br>
 * <br>
 * The content provider will scan the 'content', 'content/images' and 'content/attachment' folder in the project root. <br>
 * <br>
 * The files within that folders are synchronized every 10 seconds.
 * <br>
 * For security reasons only those directories are accessible, the given resource names don't allow any navigation.
 * <br>
 * <br>
 * For use please inject {@link ReactUIService} and create a {@link ReactUI} instance e.g. via {@link ReactUIService#getOrCreateDefaultRoot()}.
 * 
 * @author omnaest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EnableReactUIContentProvider
{

}
