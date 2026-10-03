package org.omnaest.react4j.service.internal.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared vocabulary of the plan-274 S3 theme tests, which drive the REAL {@link IndexHtmlController} with the REAL {@code /public/index.html}
 * template of the react4j-core-ui artifact through MockMvc. The index.html is cached for 5 seconds per controller, so each theme mode runs in its
 * own Spring context (one subclass per mode) and configures the theme once, before its first request, through the {@link ReactUIService} seam.
 */
@TestInstance(Lifecycle.PER_CLASS)
abstract class AbstractIndexHtmlThemeTest
{
    protected static final String MODERN_STYLESHEET = "/css/theme/react4j-modern.css";
    protected static final String STOCK_STYLESHEET  = "/css/theme/bootstrap.min.css";
    protected static final String APP_STYLESHEET    = "/css/color.css";

    @Autowired
    protected MockMvc             mockMvc;

    @Autowired
    protected ReactUIService      reactUIService;

    @BeforeAll
    void prepareContext()
    {
        this.reactUIService.getOrCreateDefaultRoot(reactUI ->
        {
            // an empty page is enough: the subject is the head of the index.html
        });
        this.configureTheme();
    }

    /**
     * Applies this mode's theme configuration through {@link ReactUIService#configureTheme(java.util.function.Consumer)}, before the first request
     */
    protected abstract void configureTheme();

    protected String getIndexHtml() throws Exception
    {
        return this.mockMvc.perform(get("/"))
                           .andExpect(status().isOk())
                           .andReturn()
                           .getResponse()
                           .getContentAsString();
    }

    protected void assertServedAsCss(String path) throws Exception
    {
        this.mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("text/css")));
    }

    /**
     * The attribute text of the opening {@code html} tag
     */
    protected static String htmlTag(String indexHtml)
    {
        Matcher matcher = Pattern.compile("<html[^>]*>")
                                 .matcher(indexHtml);
        assertTrue(matcher.find(), "no <html> tag in: " + indexHtml);
        return matcher.group();
    }

    /**
     * The index of the stylesheet link with exactly this path (any query string), failing if there is none
     */
    protected static int indexOfStylesheetLink(String indexHtml, String path)
    {
        Matcher matcher = Pattern.compile("<link rel=\"stylesheet\" href=\"" + Pattern.quote(path) + "(\\?[^\"]*)?\"")
                                 .matcher(indexHtml);
        assertTrue(matcher.find(), "no stylesheet link for " + path + " in: " + indexHtml);
        return matcher.start();
    }

    /**
     * The cache buster (query string value) of the stylesheet link with exactly this path
     */
    protected static String cacheBusterOf(String indexHtml, String path)
    {
        Matcher matcher = Pattern.compile("<link rel=\"stylesheet\" href=\"" + Pattern.quote(path) + "\\?([^\"]*)\"")
                                 .matcher(indexHtml);
        assertTrue(matcher.find(), "no stylesheet link with cache buster for " + path + " in: " + indexHtml);
        return matcher.group(1);
    }

    protected static void assertNoRawPlaceholderSurvives(String indexHtml)
    {
        for (String placeholder : new String[] {"<react4j-theme", "%BS_THEME%", "$RANDOM_NUMBER$", "$TITLE$", "$DESCRIPTION$", "$STATIC_HTML_CONTENT$",
                "%LOCALE%", "<hreflang"})
        {
            assertTrue(!indexHtml.contains(placeholder), "raw placeholder '" + placeholder + "' survived in: " + indexHtml);
        }
    }
}
