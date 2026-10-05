package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.omnaest.react4j.domain.Text;

/**
 * plan-283 S1 guard: every {@link Text.Style} maps to a theme utility class that sets a colour in EVERY stylesheet React4J serves (the modern theme, stock
 * Bootstrap and Tabler). A class that one of them does not define would make the style a silent no-op on that preset, with no error anywhere.
 * <p>
 * The sheets are read from the installed react4j-core-ui artifact, the same ones {@code IndexHtmlController} links, so install it first.
 */
public class TextStyleThemeClassSheetGuardTest
{
    @ParameterizedTest
    @ValueSource(strings = {"/public/css/theme/react4j-modern.css", "/public/css/theme/bootstrap.min.css", "/public/css/theme/react4j-tabler.css"})
    public void testEveryTextStyleMapsToAClassThatSetsTheColourInTheShippedSheet(String sheetPath)
    {
        String css = this.shipped(sheetPath);

        for (Text.Style style : Text.Style.values())
        {
            assertTrue(this.setsColour(css, style.toCssClass()), "the class '" + style.toCssClass() + "' of the text style " + style + " sets no colour in " + sheetPath);
        }
    }

    @Test
    public void testTheGuardCanFailForAClassTheSheetDoesNotDefineAndForAClassThatSetsNoColour()
    {
        String css = ".other{color:red}.text-body-secondary-ish{color:blue}.no-colour{margin:0}";

        assertFalse(this.setsColour(css, "text-body-secondary"));
        assertFalse(this.setsColour(css, "no-colour"));
        assertTrue(this.setsColour(css, "other"));
    }

    private boolean setsColour(String css, String cssClass)
    {
        return CssSheet.parse(css)
                       .stream()
                       .anyMatch(declaration -> "color".equals(declaration.getProperty()) && Arrays.asList(declaration.getSelector()
                                                                                                                      .split(","))
                                                                                                   .contains("." + cssClass));
    }

    private String shipped(String sheetPath)
    {
        try (InputStream stream = TextStyleThemeClassSheetGuardTest.class.getResourceAsStream(sheetPath))
        {
            if (stream == null)
            {
                throw new IllegalStateException("The shipped " + sheetPath + " is not on the test classpath: install react4j-core-ui first");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot read the shipped " + sheetPath, e);
        }
    }
}
