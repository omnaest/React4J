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
package org.omnaest.react4j.service.internal.controller;

import org.omnaest.react4j.service.internal.service.DataService;
import org.omnaest.react4j.service.internal.service.DataService.DataPage;
import org.omnaest.react4j.service.internal.service.DataService.DataPageQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping({ "/ui/data", "/ui/{languageTag}/data" })
public class DataController
{
    private static Logger LOG = LoggerFactory.getLogger(DataController.class);

    @Autowired
    private DataService dataService;

    @PostMapping(path = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataPage getData(@PathVariable(name = "languageTag", required = false) String languageTag, @RequestBody DataPageQuery query)
    {
        return this.dataService.getDataPage(query);
    }

    @PostConstruct
    public void postInit()
    {
        LOG.info(this.getClass()
                     .getSimpleName()
                + " enabled.");
    }
}
