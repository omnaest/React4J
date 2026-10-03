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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ShadowStrength;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeSettings;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-274 S4 AC4.2: the design token setters of the theme configuration validate at the boundary. Valid values are accepted; every injection
 * attempt is rejected with an {@link IllegalArgumentException} and leaves the state exactly as it was. Tested through the public interface and the
 * read snapshot only, no mocks.
 */
public class ThemeConfigurationServiceImplTokenTest
{
    /**
     * Strings that would break out of a declaration, a rule or the style element if they were ever written into the stylesheet: every text setter
     * must reject every one of them
     */
    private static final List<String>           ATTACKS           = List.of("#fff;}</style><script>", "1px;", "Inter</style>", "a{b}", "Inter\\", "/*", "#fff</style>", "#ffffff;color:red",
                                                                            "10px;color:red", "Inter;", "a<b", "a>b", "a/b", "Inter(", "var(--x)", "url(x)", "calc(1px)", "Inter\nArial",
                                                                            "Inter\0", "Inter !important", "", "   ");

    /**
     * Strings that are no colour, although harmless: a colour is {@code #rgb} or {@code #rrggbb} and nothing else
     */
    private static final List<String>           NOT_COLORS        = List.of("red", "rgb(1,2,3)", "#12", "#ggg", "#fffffff", "#ffffffff", "fff", "transparent", "Inter");

    /**
     * Strings that are no length: a number directly followed by px or rem and nothing else
     */
    private static final List<String>           NOT_LENGTHS       = List.of("red", "-1px", "1 px", "1em", "1PX", "12", "px", "1.px", "12345px", "1.12345px", "50%", "Inter");

    /**
     * Strings outside the font family grammar although harmless: unbalanced or mixed quotes, empty entries
     */
    private static final List<String>           NOT_FONT_FAMILIES = List.of("\"Inter", "'Inter\"", "Inter,", ",Inter", "Inter, ,Arial", "\"\"", "\"Inter\"Arial",
                                                                            "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u");

    private final ThemeConfigurationServiceImpl service           = new ThemeConfigurationServiceImpl();

    @Test
    public void testValidValuesAreAcceptedAndReadBackAsTypedTokens()
    {
        this.service.primaryColor("#0f766e")
                    .secondaryColor("#ABCDEF")
                    .successColor("#0f6")
                    .infoColor("#000")
                    .warningColor("#FFF")
                    .dangerColor("#be123c")
                    .fontFamily("Inter, \"Segoe UI\", 'Helvetica Neue', system-ui, -apple-system, sans-serif")
                    .baseFontSize("1.125rem")
                    .borderRadius("12px")
                    .shadowStrength(ShadowStrength.SUBTLE);

        ThemeTokens tokens = this.service.getSettings()
                                         .getTokens();
        assertFalse(tokens.isEmpty());
        assertEquals(RgbColor.parseHex("#0f766e"), tokens.getColors()
                                                         .get(ThemeColorRole.PRIMARY));
        assertEquals(RgbColor.of(0xab, 0xcd, 0xef), tokens.getColors()
                                                          .get(ThemeColorRole.SECONDARY));
        assertEquals(RgbColor.of(0, 255, 102), tokens.getColors()
                                                     .get(ThemeColorRole.SUCCESS));
        assertEquals(6, tokens.getColors()
                              .size());
        assertEquals("Inter, \"Segoe UI\", 'Helvetica Neue', system-ui, -apple-system, sans-serif", tokens.getFontFamily()
                                                                                                          .getCss());
        assertEquals("1.125rem", tokens.getBaseFontSize()
                                       .toCss());
        assertEquals("12px", tokens.getBorderRadius()
                                   .toCss());
        assertEquals(ShadowStrength.SUBTLE, tokens.getShadowStrength());
    }

    @Test
    public void testEveryInjectionIsRejectedByEveryTextSetterAndLeavesTheStateUnchanged()
    {
        // a non empty starting state, so that "unchanged" cannot be satisfied by an empty one
        this.service.primaryColor("#4f46e5")
                    .fontFamily("Inter, sans-serif")
                    .baseFontSize("16px")
                    .borderRadius("8px")
                    .colorMode(ThemeConfiguration.ColorMode.DARK)
                    .addStylesheet("/css/added.css");
        ThemeSettings before = this.service.getSettings();

        List<String> rejections = new ArrayList<>();
        for (String attack : ATTACKS)
        {
            this.assertRejected("primaryColor", attack, this.service::primaryColor, rejections);
            this.assertRejected("secondaryColor", attack, this.service::secondaryColor, rejections);
            this.assertRejected("successColor", attack, this.service::successColor, rejections);
            this.assertRejected("infoColor", attack, this.service::infoColor, rejections);
            this.assertRejected("warningColor", attack, this.service::warningColor, rejections);
            this.assertRejected("dangerColor", attack, this.service::dangerColor, rejections);
            this.assertRejected("fontFamily", attack, this.service::fontFamily, rejections);
            this.assertRejected("baseFontSize", attack, this.service::baseFontSize, rejections);
            this.assertRejected("borderRadius", attack, this.service::borderRadius, rejections);
        }
        for (String value : NOT_COLORS)
        {
            this.assertRejected("primaryColor", value, this.service::primaryColor, rejections);
            this.assertRejected("dangerColor", value, this.service::dangerColor, rejections);
        }
        for (String value : NOT_LENGTHS)
        {
            this.assertRejected("baseFontSize", value, this.service::baseFontSize, rejections);
            this.assertRejected("borderRadius", value, this.service::borderRadius, rejections);
        }
        for (String value : NOT_FONT_FAMILIES)
        {
            this.assertRejected("fontFamily", value, this.service::fontFamily, rejections);
        }

        assertEquals(ATTACKS.size() * 9 + NOT_COLORS.size() * 2 + NOT_LENGTHS.size() * 2 + NOT_FONT_FAMILIES.size(), rejections.size(), "every setter must reject every attack");
        assertEquals(before, this.service.getSettings(), "a rejected value must not change any part of the state");
    }

    @Test
    public void testTheInjectionsOfTheAcceptanceCriteriaAreAmongThoseTested()
    {
        for (String required : new String[] {"#fff;}</style><script>", "red", "1px;", "Inter</style>", "a{b}", "Inter\\", "/*"})
        {
            assertTrue(ATTACKS.contains(required) || NOT_COLORS.contains(required) || NOT_LENGTHS.contains(required), required);
        }
    }

    @Test
    public void testNullIsRejectedByEveryTokenSetter()
    {
        assertThrows(IllegalArgumentException.class, () -> this.service.primaryColor(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.secondaryColor(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.successColor(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.infoColor(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.warningColor(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.dangerColor(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.fontFamily(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.baseFontSize(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.borderRadius(null));
        assertThrows(IllegalArgumentException.class, () -> this.service.shadowStrength(null));

        assertTrue(this.service.getSettings()
                               .getTokens()
                               .isEmpty());
    }

    @Test
    public void testTheDefaultStateHoldsNoTokens()
    {
        assertTrue(this.service.getSettings()
                               .getTokens()
                               .isEmpty());
        assertEquals(ThemeSettings.defaults(), this.service.getSettings());
    }

    @Test
    public void testTokensSurviveDisableAndUseDefaultLikeTheColorModeAndTheStylesheets()
    {
        this.service.primaryColor("#0f766e")
                    .borderRadius("12px")
                    .disable();
        assertFalse(this.service.getSettings()
                                .isEnabled());
        assertFalse(this.service.getSettings()
                                .getTokens()
                                .isEmpty(),
                    "tokens are kept while disabled, they merely have no effect");

        this.service.useDefault();

        ThemeTokens tokens = this.service.getSettings()
                                         .getTokens();
        assertEquals(RgbColor.parseHex("#0f766e"), tokens.getColors()
                                                         .get(ThemeColorRole.PRIMARY));
        assertEquals("12px", tokens.getBorderRadius()
                                   .toCss());
    }

    @Test
    public void testALaterValueReplacesAnEarlierOneForTheSameToken()
    {
        this.service.primaryColor("#111111")
                    .primaryColor("#222222")
                    .borderRadius("4px")
                    .borderRadius("6px");

        ThemeTokens tokens = this.service.getSettings()
                                         .getTokens();
        assertEquals(RgbColor.parseHex("#222222"), tokens.getColors()
                                                         .get(ThemeColorRole.PRIMARY));
        assertEquals(1, tokens.getColors()
                              .size());
        assertEquals("6px", tokens.getBorderRadius()
                                  .toCss());
    }

    @Test
    public void testEveryTokenSetterReturnsTheConfigurationForChaining()
    {
        assertSame(this.service, this.service.primaryColor("#111"));
        assertSame(this.service, this.service.secondaryColor("#111"));
        assertSame(this.service, this.service.successColor("#111"));
        assertSame(this.service, this.service.infoColor("#111"));
        assertSame(this.service, this.service.warningColor("#111"));
        assertSame(this.service, this.service.dangerColor("#111"));
        assertSame(this.service, this.service.fontFamily("Inter"));
        assertSame(this.service, this.service.baseFontSize("16px"));
        assertSame(this.service, this.service.borderRadius("8px"));
        assertSame(this.service, this.service.shadowStrength(ShadowStrength.NONE));
    }

    @Test
    public void testAnAlreadyReturnedSnapshotKeepsItsTokensWhenTheConfigurationChangesLater()
    {
        this.service.primaryColor("#111111");
        ThemeSettings before = this.service.getSettings();

        this.service.primaryColor("#222222")
                    .dangerColor("#333333");

        assertEquals(RgbColor.parseHex("#111111"), before.getTokens()
                                                         .getColors()
                                                         .get(ThemeColorRole.PRIMARY));
        assertEquals(1, before.getTokens()
                              .getColors()
                              .size());
    }

    private void assertRejected(String setter, String value, Function<String, ThemeConfiguration> call, List<String> rejections)
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> call.apply(value), setter + "(" + value + ") must be rejected");
        // a hostile value must never be echoed into the message (which may be logged or rendered)
        if (ATTACKS.contains(value) && value.strip()
                                            .length() >= 3)
        {
            assertFalse(exception.getMessage() != null && exception.getMessage()
                                                                   .contains(value),
                        "the message must not echo the rejected value: " + exception.getMessage());
        }
        rejections.add(setter);
    }
}
