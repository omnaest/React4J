package org.omnaest.react4j.service.internal.controller.sitemap;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.omnaest.utils.NumberUtils;
import org.omnaest.utils.template.TemplateUtils;
import org.omnaest.utils.template.TemplateUtils.TemplateProcessor;
import org.springframework.stereotype.Component;

/**
 * Helper to create a xml sitemap
 * 
 * @author omnaest
 */
@Component
public class SiteMapGenerator
{

    public String generateSiteMap(List<SiteUrlLocation> locations)
    {
        List<NativeSiteUrlLocation> nativeSiteUrlLocations = locations.stream()
                                                                      .map(this.createUrlLocationToNativeUrlLocationMapper())
                                                                      .collect(Collectors.toList());
        TemplateProcessor processor = TemplateUtils.builder()
                                                   .useTemplateClassResource(this.getClass(), "sitemap.xml.ftl")
                                                   .add("locations", nativeSiteUrlLocations)
                                                   .build();

        return processor.get();
    }

    private Function<SiteUrlLocation, NativeSiteUrlLocation> createUrlLocationToNativeUrlLocationMapper()
    {
        return location ->
        {
            String url = location.getUrl();
            String lastModified = location.getLastModified()
                                          .toInstant()
                                          .toString();
            String priority = NumberUtils.formatter()
                                         .withMaximumFractionDigits(2)
                                         .format(location.getPriority());
            List<NativeAlternativeLocalizedUrlLocation> alternativeLocalizedLocations = location.getAlternativeLocalizedLocations()
                                                                                                .stream()
                                                                                                .map(alternativeLocation -> new NativeAlternativeLocalizedUrlLocation(alternativeLocation.getUrl(),
                                                                                                                                                                      alternativeLocation.getLocale()
                                                                                                                                                                                         .toLanguageTag()))
                                                                                                .collect(Collectors.toList());
            return new NativeSiteUrlLocation(url, lastModified, priority, alternativeLocalizedLocations);
        };
    }

    public static class SiteUrlLocation
    {
        private String                                url;
        private Date                                  lastModified;
        private double                                priority;
        private List<AlternativeLocalizedUrlLocation> alternativeLocalizedLocations;

        public SiteUrlLocation(String url, Date lastModified, double priority, List<AlternativeLocalizedUrlLocation> alternativeLocalizedLocations)
        {
            super();
            this.url = url;
            this.lastModified = lastModified;
            this.priority = priority;
            this.alternativeLocalizedLocations = alternativeLocalizedLocations;
        }

        public String getUrl()
        {
            return this.url;
        }

        public Date getLastModified()
        {
            return this.lastModified;
        }

        public double getPriority()
        {
            return this.priority;
        }

        public List<AlternativeLocalizedUrlLocation> getAlternativeLocalizedLocations()
        {
            return this.alternativeLocalizedLocations;
        }

    }

    public static class AlternativeLocalizedUrlLocation
    {
        private String url;
        private Locale locale;

        public AlternativeLocalizedUrlLocation(String url, Locale locale)
        {
            super();
            this.url = url;
            this.locale = locale;
        }

        public String getUrl()
        {
            return this.url;
        }

        public Locale getLocale()
        {
            return this.locale;
        }

    }

    public static class NativeSiteUrlLocation
    {
        private String                                      url;
        private String                                      lastModified;
        private String                                      priority;
        private List<NativeAlternativeLocalizedUrlLocation> alternativeLocalizedLocations;

        public NativeSiteUrlLocation(String url, String lastModified, String priority,
                                     List<NativeAlternativeLocalizedUrlLocation> alternativeLocalizedLocations)
        {
            super();
            this.url = url;
            this.lastModified = lastModified;
            this.priority = priority;
            this.alternativeLocalizedLocations = alternativeLocalizedLocations;
        }

        public String getUrl()
        {
            return this.url;
        }

        public String getLastModified()
        {
            return this.lastModified;
        }

        public String getPriority()
        {
            return this.priority;
        }

        public List<NativeAlternativeLocalizedUrlLocation> getAlternativeLocalizedLocations()
        {
            return this.alternativeLocalizedLocations;
        }

    }

    public static class NativeAlternativeLocalizedUrlLocation
    {
        private String url;
        private String locale;

        public NativeAlternativeLocalizedUrlLocation(String url, String locale)
        {
            super();
            this.url = url;
            this.locale = locale;
        }

        public String getUrl()
        {
            return this.url;
        }

        public String getLocale()
        {
            return this.locale;
        }

    }
}
