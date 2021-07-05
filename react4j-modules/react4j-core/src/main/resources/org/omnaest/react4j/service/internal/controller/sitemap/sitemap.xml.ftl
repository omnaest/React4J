<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd">
    <#list locations as location>
        <url>
            <loc>${location.url}</loc>
            <lastmod>${location.lastModified}</lastmod>
            <priority>${location.priority}</priority>
            
            <#list location.alternativeLocalizedLocations as alternative>
              <xhtml:link 
                href="${alternative.url}" 
                rel="alternate"            
                hreflang="${alternative.locale}" 
              />
            </#list>
            <#list location.canonicalLocations as canonical>
              <xhtml:link 
                href="${canonical.url}" 
                rel="canonical"            
              />
            </#list>
        </url>
    </#list>
</urlset>