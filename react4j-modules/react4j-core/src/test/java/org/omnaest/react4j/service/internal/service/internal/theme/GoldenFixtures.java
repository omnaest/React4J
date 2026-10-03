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
package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.CssLength;
import org.omnaest.react4j.service.internal.service.FontFamily;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads the golden fixtures that {@code src/test/resources/theme/golden/regenerate-golden.js} generates from Bootstrap's own Sass, the compiled
 * theme stylesheet of the react4j-core-ui artifact, and turns fixture token sets into {@link ThemeTokens}.
 *
 * @author omnaest
 */
final class GoldenFixtures
{
    static final String               COMPILED_THEME_PATH = "/public/css/theme/react4j-modern.css";

    private static final ObjectMapper MAPPER              = new ObjectMapper();

    private GoldenFixtures()
    {
    }

    static JsonNode functionGolden()
    {
        return json("/theme/golden/bootstrap-function-golden.json");
    }

    static JsonNode sheetGolden()
    {
        return json("/theme/golden/theme-sheet-golden.json");
    }

    /**
     * @return the declarations of the compiled react4j-modern stylesheet as shipped in the react4j-core-ui artifact on the classpath
     */
    static List<CssSheet.Declaration> compiledTheme()
    {
        String css = text(COMPILED_THEME_PATH);
        List<CssSheet.Declaration> declarations = CssSheet.parse(css);
        assertTrue(declarations.size() > 2000, "the compiled theme on the classpath looks empty or stale: " + declarations.size() + " declarations");
        return declarations;
    }

    /**
     * A Sass colour text (hex, or rgb() with percentages) as a colour
     */
    static RgbColor color(String sassText)
    {
        CssValues.Parsed parsed = CssValues.parse(sassText);
        assertTrue(parsed.colors.size() == 1 && parsed.skeleton.equals("<C>"), "not a single colour: " + sassText);
        double[] channels = parsed.colors.get(0);
        return RgbColor.of(channels[0], channels[1], channels[2]);
    }

    /**
     * Asserts that the colour, written the way the renderer writes it, is the colour Sass wrote
     */
    static void assertColor(String message, String sassText, RgbColor actual)
    {
        assertNull(CssValues.describeDifference(sassText, CssText.color(actual)), message);
    }

    /**
     * Asserts that a to-rgb style triplet equals the text Sass wrote ({@code 105, 98, 233})
     */
    static void assertTriplet(String message, String sassText, int[] actual)
    {
        org.junit.jupiter.api.Assertions.assertEquals(sassText, CssText.triplet(actual), message);
    }

    static ThemeTokens tokens(JsonNode tokenNode)
    {
        ThemeTokens tokens = ThemeTokens.none();
        for (ThemeColorRole role : ThemeColorRole.values())
        {
            if (tokenNode.has(role.getCssName()))
            {
                tokens = tokens.withColor(role, RgbColor.parseHex(tokenNode.get(role.getCssName())
                                                                           .asText()));
            }
        }
        if (tokenNode.has("fontFamily"))
        {
            tokens = tokens.withFontFamily(FontFamily.parse(tokenNode.get("fontFamily")
                                                                     .asText()));
        }
        if (tokenNode.has("baseFontSize"))
        {
            tokens = tokens.withBaseFontSize(CssLength.parse(tokenNode.get("baseFontSize")
                                                                      .asText()));
        }
        if (tokenNode.has("borderRadius"))
        {
            tokens = tokens.withBorderRadius(CssLength.parse(tokenNode.get("borderRadius")
                                                                      .asText()));
        }
        if (tokenNode.has("shadowStrength"))
        {
            tokens = tokens.withShadowStrength(ShadowStrength.valueOf(tokenNode.get("shadowStrength")
                                                                               .asText()));
        }
        return tokens;
    }

    static List<String> fieldNames(JsonNode node)
    {
        List<String> names = new ArrayList<>();
        node.fieldNames()
            .forEachRemaining(names::add);
        return names;
    }

    private static JsonNode json(String path)
    {
        try
        {
            return MAPPER.readTree(text(path));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("unreadable fixture " + path, e);
        }
    }

    private static String text(String path)
    {
        try (InputStream stream = GoldenFixtures.class.getResourceAsStream(path))
        {
            assertTrue(stream != null, "missing classpath resource " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("unreadable resource " + path, e);
        }
    }
}
