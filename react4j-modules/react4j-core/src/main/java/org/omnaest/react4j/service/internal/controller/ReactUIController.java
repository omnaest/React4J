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
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.handler.domain.TargetNode;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.rerenderer.RerenderingService;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
public class ReactUIController
{
    private static Logger LOG = LoggerFactory.getLogger(ReactUIController.class);

    @Autowired
    private RootNodeResolverService resolverService;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired
    private RerenderingService rerenderingService;

    @Autowired
    private LocaleService localeService;

    //    @RequestMapping(method = RequestMethod.GET, path = { "{contextPath}/ui", "{languageTag}/{contextPath}/ui" }, produces = MediaType.APPLICATION_JSON_VALUE)
    //    public NodeHierarchy getNodeHierarchy(@PathVariable(name = "languageTag", required = false) String languageTag,
    //                                          @PathVariable("contextPath") String contextPath)
    //    {
    //        this.localeService.setRequestLocaleByLanguageTag(languageTag);
    //        return this.resolverService.resolveNodeHierarchy(contextPath);
    //    }

    @RequestMapping(method = RequestMethod.GET, path = { "/ui", "{languageTag}/ui" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public NodeHierarchy getNodeHierarchy(@PathVariable(name = "languageTag", required = false) String languageTag)
    {
        this.localeService.setExplicitRequestLocaleByLanguageTag(languageTag);
        return this.eventHandlerService.executeTransactionalAndPublishStagingHandlers(this.resolverService::resolveDefaultNodeHierarchy);
    }

    @RequestMapping(method = RequestMethod.POST, path = { "/ui", "{languageTag}/ui" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<TargetNode> getSubNodeHierarchy(@RequestBody SubNodeRerenderingContext context,
                                                    @PathVariable(name = "languageTag", required = false) String languageTag)
    {
        this.localeService.setExplicitRequestLocaleByLanguageTag(languageTag);
        return this.eventHandlerService.executeTransactionalAndPublishStagingHandlers(() -> this.rerenderingService.rerenderTargetNode(context.getTarget(),
                                                                                                                                       Optional.empty()));
    }

    public static class SubNodeRerenderingContext
    {
        @JsonProperty
        private Target target;

        public Target getTarget()
        {
            return this.target;
        }

        @Override
        public String toString()
        {
            return "SubNodeRerenderingContext [target=" + this.target + "]";
        }

    }

    @RequestMapping(method = RequestMethod.POST, path = { "/ui/event",
                                                          "{languageTag}/ui/event" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<ResponseBody> acceptEvent(@RequestBody EventBody eventBody, @PathVariable(name = "languageTag", required = false) String languageTag)
    {
        this.localeService.setExplicitRequestLocaleByLanguageTag(languageTag);
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
