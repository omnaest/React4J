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
package org.omnaest.react4j.service.internal.service.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.omnaest.react4j.domain.configuration.HomePageConfiguration;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.springframework.stereotype.Service;

@Service
public class HomePageConfigurationServiceImpl implements HomePageConfigurationService
{
    private Map<String, String> configurations = new ConcurrentHashMap<>();

    @Override
    public HomePageConfiguration setTitle(String title)
    {
        this.configurations.put("$TITLE$", title);
        return this;
    }

    @Override
    public Map<String, String> getConfigurations()
    {
        return this.configurations;
    }

    @Override
    public HomePageConfiguration setDescription(String description)
    {
        this.configurations.put("$DESCRIPTION$", description);
        return this;
    }

}
