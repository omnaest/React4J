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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorDeriver.ButtonVariant;
import org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorDeriver.OutlineButtonVariant;
import org.omnaest.react4j.service.internal.service.internal.theme.BootstrapColorDeriver.TableVariant;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * plan-274 S4 AC4.1, level two: everything Bootstrap derives from a theme colour (text emphasis, subtle background and border for light and dark mode,
 * the link colours, the real {@code button-variant}, {@code button-outline-variant} and {@code table-variant} mixins) against the output of
 * Bootstrap's own Sass for the same input colour. No mocks, no hand typed expectations.
 */
public class BootstrapColorDeriverTest
{
    private final JsonNode golden = GoldenFixtures.functionGolden();

    @TestFactory
    public List<DynamicTest> testSubtleVariantsLinkColoursAndFocusHelpersAgainstBootstrapSass()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode colorCase : this.golden.get("colors"))
        {
            String input = colorCase.get("input")
                                    .asText();
            tests.add(DynamicTest.dynamicTest("variables derived from " + input, () ->
            {
                RgbColor color = RgbColor.parseHex(input);
                JsonNode v = colorCase.get("values");
                GoldenFixtures.assertColor("text-emphasis light", v.get("--text-emphasis")
                                                                   .asText(),
                                           BootstrapColorDeriver.textEmphasis(color, false));
                GoldenFixtures.assertColor("bg-subtle light", v.get("--bg-subtle")
                                                               .asText(),
                                           BootstrapColorDeriver.backgroundSubtle(color, false));
                GoldenFixtures.assertColor("border-subtle light", v.get("--border-subtle")
                                                                   .asText(),
                                           BootstrapColorDeriver.borderSubtle(color, false));
                GoldenFixtures.assertColor("text-emphasis dark", v.get("--text-emphasis-dark")
                                                                  .asText(),
                                           BootstrapColorDeriver.textEmphasis(color, true));
                GoldenFixtures.assertColor("bg-subtle dark", v.get("--bg-subtle-dark")
                                                              .asText(),
                                           BootstrapColorDeriver.backgroundSubtle(color, true));
                GoldenFixtures.assertColor("border-subtle dark", v.get("--border-subtle-dark")
                                                                  .asText(),
                                           BootstrapColorDeriver.borderSubtle(color, true));

                RgbColor linkLight = BootstrapColorDeriver.linkColor(color, false);
                RgbColor linkDark = BootstrapColorDeriver.linkColor(color, true);
                GoldenFixtures.assertColor("link-color light", v.get("--link-color")
                                                                .asText(),
                                           linkLight);
                GoldenFixtures.assertColor("link-hover-color light", v.get("--link-hover-color")
                                                                      .asText(),
                                           BootstrapColorDeriver.linkHoverColor(linkLight, false));
                GoldenFixtures.assertColor("link-color dark", v.get("--link-color-dark")
                                                               .asText(),
                                           linkDark);
                GoldenFixtures.assertColor("link-hover-color dark", v.get("--link-hover-color-dark")
                                                                     .asText(),
                                           BootstrapColorDeriver.linkHoverColor(linkDark, true));
                GoldenFixtures.assertColor("input focus border", v.get("--input-focus-border-color")
                                                                  .asText(),
                                           BootstrapColorDeriver.focusBorder(color));
                GoldenFixtures.assertColor("range thumb active", v.get("--range-thumb-active-bg")
                                                                  .asText(),
                                           BootstrapColorDeriver.rangeThumbActive(color));
                GoldenFixtures.assertTriplet("btn-link focus shadow", v.get("--btn-link-focus-shadow-rgb")
                                                                       .asText(),
                                             BootstrapColorDeriver.linkButtonFocusShadowRgb(color));
                GoldenFixtures.assertColor("link helper hover", v.get("--link-helper-hover")
                                                                 .asText(),
                                           BootstrapColorDeriver.linkHelperHoverColor(color));
                assertEquals(GoldenFixtures.color(v.get("--color-contrast")
                                                   .asText()),
                             BootstrapColorDeriver.textOn(color), "text-bg color");
            }));
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> testTheSolidButtonVariantAgainstTheBootstrapButtonVariantMixin()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode colorCase : this.golden.get("colors"))
        {
            String input = colorCase.get("input")
                                    .asText();
            tests.add(DynamicTest.dynamicTest("button-variant of " + input, () ->
            {
                ButtonVariant actual = BootstrapColorDeriver.buttonVariant(RgbColor.parseHex(input));
                JsonNode v = colorCase.get("buttonVariant");
                GoldenFixtures.assertColor("color", v.get("--bs-btn-color")
                                                     .asText(),
                                           actual.getColor());
                GoldenFixtures.assertColor("bg", v.get("--bs-btn-bg")
                                                  .asText(),
                                           actual.getBackground());
                GoldenFixtures.assertColor("border", v.get("--bs-btn-border-color")
                                                      .asText(),
                                           actual.getBorder());
                GoldenFixtures.assertColor("hover color", v.get("--bs-btn-hover-color")
                                                           .asText(),
                                           actual.getHoverColor());
                GoldenFixtures.assertColor("hover bg", v.get("--bs-btn-hover-bg")
                                                        .asText(),
                                           actual.getHoverBackground());
                GoldenFixtures.assertColor("hover border", v.get("--bs-btn-hover-border-color")
                                                            .asText(),
                                           actual.getHoverBorder());
                GoldenFixtures.assertTriplet("focus shadow rgb", v.get("--bs-btn-focus-shadow-rgb")
                                                                  .asText(),
                                             actual.getFocusShadowRgb());
                GoldenFixtures.assertColor("active color", v.get("--bs-btn-active-color")
                                                            .asText(),
                                           actual.getActiveColor());
                GoldenFixtures.assertColor("active bg", v.get("--bs-btn-active-bg")
                                                         .asText(),
                                           actual.getActiveBackground());
                GoldenFixtures.assertColor("active border", v.get("--bs-btn-active-border-color")
                                                             .asText(),
                                           actual.getActiveBorder());
                GoldenFixtures.assertColor("disabled color", v.get("--bs-btn-disabled-color")
                                                              .asText(),
                                           actual.getDisabledColor());
                GoldenFixtures.assertColor("disabled bg", v.get("--bs-btn-disabled-bg")
                                                           .asText(),
                                           actual.getDisabledBackground());
                GoldenFixtures.assertColor("disabled border", v.get("--bs-btn-disabled-border-color")
                                                               .asText(),
                                           actual.getDisabledBorder());
            }));
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> testTheOutlineButtonVariantAgainstTheBootstrapButtonOutlineVariantMixin()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode colorCase : this.golden.get("colors"))
        {
            String input = colorCase.get("input")
                                    .asText();
            tests.add(DynamicTest.dynamicTest("button-outline-variant of " + input, () ->
            {
                OutlineButtonVariant actual = BootstrapColorDeriver.buttonOutlineVariant(RgbColor.parseHex(input));
                JsonNode v = colorCase.get("outlineVariant");
                GoldenFixtures.assertColor("color", v.get("--bs-btn-color")
                                                     .asText(),
                                           actual.getColor());
                GoldenFixtures.assertColor("border", v.get("--bs-btn-border-color")
                                                      .asText(),
                                           actual.getBorder());
                GoldenFixtures.assertColor("hover color", v.get("--bs-btn-hover-color")
                                                           .asText(),
                                           actual.getHoverColor());
                GoldenFixtures.assertColor("hover bg", v.get("--bs-btn-hover-bg")
                                                        .asText(),
                                           actual.getHoverBackground());
                GoldenFixtures.assertColor("hover border", v.get("--bs-btn-hover-border-color")
                                                            .asText(),
                                           actual.getHoverBorder());
                GoldenFixtures.assertTriplet("focus shadow rgb", v.get("--bs-btn-focus-shadow-rgb")
                                                                  .asText(),
                                             actual.getFocusShadowRgb());
                GoldenFixtures.assertColor("active color", v.get("--bs-btn-active-color")
                                                            .asText(),
                                           actual.getActiveColor());
                GoldenFixtures.assertColor("active bg", v.get("--bs-btn-active-bg")
                                                         .asText(),
                                           actual.getActiveBackground());
                GoldenFixtures.assertColor("active border", v.get("--bs-btn-active-border-color")
                                                             .asText(),
                                           actual.getActiveBorder());
                GoldenFixtures.assertColor("disabled color", v.get("--bs-btn-disabled-color")
                                                              .asText(),
                                           actual.getDisabledColor());
                GoldenFixtures.assertColor("disabled border", v.get("--bs-btn-disabled-border-color")
                                                               .asText(),
                                           actual.getDisabledBorder());
            }));
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> testTheTableVariantAgainstTheBootstrapTableVariantMixin()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (JsonNode colorCase : this.golden.get("colors"))
        {
            String input = colorCase.get("input")
                                    .asText();
            tests.add(DynamicTest.dynamicTest("table-variant of " + input, () ->
            {
                TableVariant actual = BootstrapColorDeriver.tableVariant(RgbColor.parseHex(input));
                JsonNode v = colorCase.get("tableVariant");
                GoldenFixtures.assertColor("color", v.get("--bs-table-color")
                                                     .asText(),
                                           actual.getColor());
                GoldenFixtures.assertColor("bg", v.get("--bs-table-bg")
                                                  .asText(),
                                           actual.getBackground());
                GoldenFixtures.assertColor("border", v.get("--bs-table-border-color")
                                                      .asText(),
                                           actual.getBorderColor());
                GoldenFixtures.assertColor("striped bg", v.get("--bs-table-striped-bg")
                                                          .asText(),
                                           actual.getStripedBackground());
                GoldenFixtures.assertColor("striped color", v.get("--bs-table-striped-color")
                                                             .asText(),
                                           actual.getStripedColor());
                GoldenFixtures.assertColor("active bg", v.get("--bs-table-active-bg")
                                                         .asText(),
                                           actual.getActiveBackground());
                GoldenFixtures.assertColor("active color", v.get("--bs-table-active-color")
                                                            .asText(),
                                           actual.getActiveColor());
                GoldenFixtures.assertColor("hover bg", v.get("--bs-table-hover-bg")
                                                        .asText(),
                                           actual.getHoverBackground());
                GoldenFixtures.assertColor("hover color", v.get("--bs-table-hover-color")
                                                           .asText(),
                                           actual.getHoverColor());
            }));
        }
        return tests;
    }
}
