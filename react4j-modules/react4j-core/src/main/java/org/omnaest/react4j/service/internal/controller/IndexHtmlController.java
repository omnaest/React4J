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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.AlternativeLocalizedUrlLocation;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.SiteUrlLocation;
import org.omnaest.react4j.service.internal.controller.utils.RequestProvider;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ClassUtils.Resource;
import org.omnaest.utils.MatcherUtils;
import org.omnaest.utils.counter.Counter;
import org.omnaest.utils.duration.TimeDuration;
import org.omnaest.utils.element.cached.CachedElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexHtmlController
{
    private static final Logger            LOG = LoggerFactory.getLogger(IndexHtmlController.class);
    @Autowired
    protected HomePageConfigurationService homePageConfigurationService;

    @Autowired
    private RootNodeResolverService resolverService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private RequestProvider requestProvider;

    @Autowired
    private SiteMapGenerator siteMapGenerator;

    @Value("${react4j.language-redirect:false}")
    private boolean languageRedirectEnabled;

    @Value("${react4j.public-url:#{null}}")
    private Optional<String> publicUrl;

    private Counter pageHitCounter = Counter.fromZero();

    private CachedElement<String> indexHtml = CachedElement.of(() -> ClassUtils.loadResource(this, "/public/index.html")
                                                                               .map(Resource::asString)
                                                                               .map(content -> MatcherUtils.replacer()
                                                                                                           .addExactMatchReplacement("$RANDOM_NUMBER$", ""
                                                                                                                   + Math.abs(("" + Math.random()).hashCode()))
                                                                                                           .addExactMatchReplacements(this.homePageConfigurationService.getConfigurations())
                                                                                                           .addExactMatchReplacement("$STATIC_HTML_CONTENT$",
                                                                                                                                     this.resolverService.renderDefaultNodeHierarchyAsStatic(NodeRenderType.HTML))
                                                                                                           .addExactMatchReplacement("%LOCALE%",
                                                                                                                                     this.localeService.getRequestLocale()
                                                                                                                                                       .orElse(Locale.US)
                                                                                                                                                       .toLanguageTag())
                                                                                                           .addExactMatchReplacement("<hreflang>",
                                                                                                                                     this.generateHrefLangLinkTags())
                                                                                                           .findAndReplaceAllIn(content))
                                                                               .orElseThrow(() -> new IllegalStateException("Could not load index.html from classpath")))
                                                           .asDurationLimitedCachedElement(TimeDuration.of(5, TimeUnit.SECONDS));

    @RequestMapping(method = RequestMethod.GET, path = { "/index.html",
                                                         "{languageTag:[a-zA-Z\\-]+}",
                                                         "{languageTag}/index.html" }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getLanguageSpecific(@PathVariable(name = "languageTag", required = false) String languageTag)
    {
        if (this.languageRedirectEnabled && StringUtils.isBlank(languageTag))
        {
            return this.createRedirectResponse();
        }
        else
        {
            this.localeService.setRequestLocaleByLanguageTag(languageTag);
            this.pageHitCounter.increment()
                               .ifModulo(100, count -> LOG.info("Number of index.html page hits: " + count));
            return ResponseEntity.ok(this.indexHtml.get());
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = { "/sitemap.xml" }, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap()
    {
        Date lastModified = Date.from(Instant.now());
        String currentLanguageTag = this.localeService.getRequestLocale()
                                                      .orElse(Locale.US)
                                                      .toLanguageTag();

        List<AlternativeLocalizedUrlLocation> alternativeLocalizedLocations = this.localeService.getAvailableLocales()
                                                                                                .stream()
                                                                                                .map(locale -> new AlternativeLocalizedUrlLocation(this.generatePublicLocalizedUrl(currentLanguageTag,
                                                                                                                                                                                   locale.toLanguageTag()),
                                                                                                                                                   locale))
                                                                                                .collect(Collectors.toList());

        List<SiteUrlLocation> languageDependentLocations = this.localeService.getAvailableLocales()
                                                                             .stream()
                                                                             .map(Locale::toLanguageTag)
                                                                             .map(languageTag ->
                                                                             {
                                                                                 String url = this.generatePublicLocalizedUrl(currentLanguageTag, languageTag);
                                                                                 double priority = 1.0;
                                                                                 return new SiteUrlLocation(url, lastModified, priority,
                                                                                                            alternativeLocalizedLocations);
                                                                             })
                                                                             .collect(Collectors.toList());

        List<SiteUrlLocation> singleRootLocations = Arrays.asList(new SiteUrlLocation(this.generatePublicRootUrl(currentLanguageTag), lastModified, 1.0,
                                                                                      Collections.emptyList()));

        List<SiteUrlLocation> locations = this.languageRedirectEnabled ? languageDependentLocations : singleRootLocations;
        return ResponseEntity.ok(this.siteMapGenerator.generateSiteMap(locations));
    }

    private String generateHrefLangLinkTags()
    {
        String currentLanguageTag = this.localeService.getRequestLocale()
                                                      .orElse(Locale.US)
                                                      .toLanguageTag();
        return this.localeService.getAvailableLocales()
                                 .stream()
                                 .map(Locale::toLanguageTag)
                                 .map(languageTag ->
                                 {
                                     String url = this.generatePublicLocalizedUrl(currentLanguageTag, languageTag);
                                     return "<link rel=\"alternate\" href=\"" + url + "\" hreflang=\"" + languageTag + "\" />";
                                 })
                                 .collect(Collectors.joining("\n"));
    }

    private String generatePublicLocalizedUrl(String currentLanguageTag, String languageTag)
    {
        return this.generatePublicRootUrl(currentLanguageTag) + "/" + languageTag;
    }

    private String generatePublicRootUrl(String currentLanguageTag)
    {
        return this.publicUrl.orElseGet(() -> this.requestProvider.getRequestBaseUrl()
                                                                  .map(requestUrl -> StringUtils.removeEndIgnoreCase(requestUrl, currentLanguageTag))
                                                                  .orElse(""));
    }

    private ResponseEntity<String> createRedirectResponse()
    {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                             .header(HttpHeaders.LOCATION, this.localeService.getRequestLocale()
                                                                             .orElse(Locale.US)
                                                                             .toLanguageTag())
                             .build();
    }

}
