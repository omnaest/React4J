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

import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.AlternativeLocalizedUrlLocation;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.CanonicalUrlLocation;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.SiteUrlLocation;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.service.ContextService;
import org.omnaest.react4j.service.internal.service.HomePageConfigurationService;
import org.omnaest.react4j.service.internal.service.internal.translation.component.LocaleService;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ClassUtils.Resource;
import org.omnaest.utils.ListUtils;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexHtmlController
{
    private static final Logger LOG = LoggerFactory.getLogger(IndexHtmlController.class);

    @Autowired
    protected HomePageConfigurationService homePageConfigurationService;

    @Autowired
    private RootNodeResolverService resolverService;

    @Autowired
    private LocaleService localeService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private SiteMapGenerator siteMapGenerator;

    @Value("${react4j.language-redirect:false}")
    private boolean languageRedirectEnabled;

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
                                                                                                           .addExactMatchReplacement("<hreflang/>",
                                                                                                                                     this.generateHrefLangRelatedLinkTags())
                                                                                                           .findAndReplaceAllIn(content))
                                                                               .orElseThrow(() -> new IllegalStateException("Could not load index.html from classpath")))
                                                           .asDurationLimitedCachedElement(TimeDuration.of(5, TimeUnit.SECONDS));

    @GetMapping(path = { "/", "/index.html", "{languageTag:[a-zA-Z\\-]+}", "{languageTag}/index.html", "{languageTag}/" }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getLanguageSpecific(@PathVariable(name = "languageTag", required = false) String languageTag)
    {
        if (this.languageRedirectEnabled && StringUtils.isBlank(languageTag) && !this.localeService.isRequestLocaleEqualToDefaultLocale())
        {
            return this.createRedirectResponse();
        }
        else
        {
            this.localeService.setExplicitRequestLocaleByLanguageTag(languageTag);
            this.pageHitCounter.increment()
                               .ifModulo(100, count -> LOG.info("Number of index.html page hits: " + count));
            return ResponseEntity.ok(this.indexHtml.get());
        }
    }

    @GetMapping(path = { "/sitemap.xml" }, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap()
    {
        Date lastModified = Date.from(Instant.now());

        List<AlternativeLocalizedUrlLocation> alternativeLocalizedLocations = this.localeService.getAvailableLocales()
                                                                                                .stream()
                                                                                                .map(locale -> new AlternativeLocalizedUrlLocation(this.generatePublicLocalizedUrl(locale),
                                                                                                                                                   locale))
                                                                                                .collect(Collectors.toList());

        List<SiteUrlLocation> languageDependentLocations = this.localeService.getAvailableLocales()
                                                                             .stream()
                                                                             .map(locale ->
                                                                             {
                                                                                 String url = this.generatePublicLocalizedUrl(locale);
                                                                                 double priority = 1.0;
                                                                                 List<CanonicalUrlLocation> canonicalLocations = Collections.emptyList();
                                                                                 return new SiteUrlLocation(url, lastModified, priority,
                                                                                                            alternativeLocalizedLocations, canonicalLocations);
                                                                             })
                                                                             .collect(Collectors.toList());

        List<SiteUrlLocation> singleRootLocations = Arrays.asList(new SiteUrlLocation(this.generatePublicRootUrl(), lastModified, 1.0, Collections.emptyList(),
                                                                                      Arrays.asList(new CanonicalUrlLocation(this.generatePublicLocalizedUrl(this.localeService.getDefaultLocale())))));

        List<SiteUrlLocation> locations = this.languageRedirectEnabled ? ListUtils.mergedList(languageDependentLocations, singleRootLocations)
                : singleRootLocations;
        return ResponseEntity.ok(this.siteMapGenerator.generateSiteMap(locations));
    }

    private String generatePublicRootUrl()
    {
        return this.contextService.getPublicDomainUrl()
                                  .map(URL::toExternalForm)
                                  .orElse("");
    }

    private String generateHrefLangRelatedLinkTags()
    {
        return this.generateHrefLangPrimaryLinkTags() + " " + this.generateHrefLangCanonicalLinkTag();
    }

    private String generateHrefLangCanonicalLinkTag()
    {
        if (!this.localeService.isExplicitRequestLocaleGiven())
        {
            Locale defaultLocale = this.localeService.getDefaultLocale();
            String languageSpecificUrl = this.generatePublicLocalizedUrl(defaultLocale);
            return "<link rel=\"canonical\" href=\"" + languageSpecificUrl + "\" />";
        }
        else
        {
            return "";
        }
    }

    private String generateHrefLangPrimaryLinkTags()
    {
        return this.localeService.getAvailableLocales()
                                 .stream()
                                 .map(locale ->
                                 {
                                     String url = this.generatePublicLocalizedUrl(locale);
                                     return "<link rel=\"alternate\" href=\"" + url + "\" hreflang=\"" + locale.toLanguageTag() + "\" />";
                                 })
                                 .collect(Collectors.joining("\n"));
    }

    private String generatePublicLocalizedUrl(Locale locale)
    {
        return this.contextService.getPublicDomainUrl(locale)
                                  .map(URL::toExternalForm)
                                  .orElse("");
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
