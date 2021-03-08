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
package org.omnaest.react4j.service.internal.configuration;

import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.controller.content.ContentControllerBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ComponentScan(basePackageClasses = ReactUIService.class, excludeFilters = @ComponentScan.Filter(classes = ContentControllerBean.class))
@EnableScheduling
@ConditionalOnWebApplication
@ConditionalOnBean(annotation = EnableReactUI.class)
public class ReactUIAutoConfiguration
{
}
