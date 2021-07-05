package org.omnaest.react4j.service.internal.controller.sitemap;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.AlternativeLocalizedUrlLocation;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.CanonicalUrlLocation;
import org.omnaest.react4j.service.internal.controller.sitemap.SiteMapGenerator.SiteUrlLocation;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ListUtils;

public class SiteMapGeneratorTest
{
    private SiteMapGenerator generator = new SiteMapGenerator();

    @Test
    public void testGenerateSiteMap() throws Exception
    {
        Date lastModified = Date.from(Instant.parse("2021-06-26T12:13:31.957Z"));
        List<SiteUrlLocation> locations = ListUtils.builder()
                                                   .add(new SiteUrlLocation("https://www.domain.org/en-US", lastModified, 1.0, ListUtils.builder()
                                                                                                                                        .add(new AlternativeLocalizedUrlLocation("https://www.domain.org/en-US",
                                                                                                                                                                                 Locale.US))
                                                                                                                                        .add(new AlternativeLocalizedUrlLocation("https://www.domain.org/fr",
                                                                                                                                                                                 Locale.FRENCH))
                                                                                                                                        .build(),
                                                                            Collections.emptyList()))
                                                   .add(new SiteUrlLocation("https://www.domain.org/fr", lastModified, 1.0, ListUtils.builder()
                                                                                                                                     .add(new AlternativeLocalizedUrlLocation("https://www.domain.org/en-US",
                                                                                                                                                                              Locale.US))
                                                                                                                                     .add(new AlternativeLocalizedUrlLocation("https://www.domain.org/fr",
                                                                                                                                                                              Locale.FRENCH))
                                                                                                                                     .build(),
                                                                            Collections.emptyList()))
                                                   .add(new SiteUrlLocation("https://www.domain.org", lastModified, 1.0, Collections.emptyList(),
                                                                            ListUtils.builder()
                                                                                     .add(new CanonicalUrlLocation("https://www.domain.org/en-US"))
                                                                                     .build()))
                                                   .build();
        String generatedSiteMap = this.generator.generateSiteMap(locations);
        String expectedSiteMap = ClassUtils.loadResource(this, "expectedSiteMap1.xml")
                                           .get()
                                           .asString();

        assertEquals(expectedSiteMap, generatedSiteMap);
    }

}
