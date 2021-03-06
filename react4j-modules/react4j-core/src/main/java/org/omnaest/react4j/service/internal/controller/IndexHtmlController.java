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

import java.util.concurrent.TimeUnit;

import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ClassUtils.Resource;
import org.omnaest.utils.MatcherUtils;
import org.omnaest.utils.duration.TimeDuration;
import org.omnaest.utils.element.cached.CachedElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexHtmlController
{
    @Autowired
    protected HomePageConfigurationService homePageConfigurationService;

    @Autowired
    private RootNodeResolverService resolverService;

    private CachedElement<String> indexHtml = CachedElement.of(() -> ClassUtils.loadResource(this, "/public/index.html")
                                                                               .map(Resource::asString)
                                                                               .map(content -> MatcherUtils.replacer()
                                                                                                           .addExactMatchReplacement("$RANDOM_NUMBER$", ""
                                                                                                                   + Math.abs(("" + Math.random()).hashCode()))
                                                                                                           .addExactMatchReplacements(this.homePageConfigurationService.getConfigurations())
                                                                                                           .addExactMatchReplacement("$STATIC_HTML_CONTENT$",
                                                                                                                                     this.resolverService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML))
                                                                                                           .findAndReplaceAllIn(content))
                                                                               .orElseThrow(() -> new IllegalStateException("Could not load index.html from classpath")))
                                                           .asDurationLimitedCachedElement(TimeDuration.of(5, TimeUnit.SECONDS));

    @RequestMapping(method = RequestMethod.GET, path = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public String get()
    {
        return this.indexHtml.get();
    }

}
