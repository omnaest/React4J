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

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.omnaest.react4j.service.internal.handler.EventHandlerService;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.ResponseBody;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReactUIController
{
    private static Logger LOG = LoggerFactory.getLogger(ReactUIController.class);

    @Autowired
    private RootNodeResolverService resolverService;

    @Autowired
    private EventHandlerService eventHandlerService;

    //    @CrossOrigin("*")
    @RequestMapping(method = RequestMethod.GET, path = "{contextPath}/ui", produces = MediaType.APPLICATION_JSON_VALUE)
    public NodeHierarchy getNodeHierarchy(@PathVariable("contextPath") String contextPath)
    {
        return this.resolverService.resolveNodeHierarchy(contextPath);
    }

    //    @CrossOrigin("*")
    @RequestMapping(method = RequestMethod.GET, path = "/ui", produces = MediaType.APPLICATION_JSON_VALUE)
    public NodeHierarchy getNodeHierarchy()
    {
        return this.resolverService.resolveDefaultNodeHierarchy();
    }

    //    @CrossOrigin("*")
    @RequestMapping(method = RequestMethod.POST, path = "/ui/event", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<ResponseBody> acceptEvent(@RequestBody EventBody eventBody)
    {
        return this.eventHandlerService.handleEvent(eventBody);
    }

    @PostConstruct
    public void postInit()
    {
        LOG.info(this.getClass()
                     .getSimpleName()
                + " enabled.");
    }
}
