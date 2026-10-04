package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ThemePreset;
import org.omnaest.react4j.service.internal.service.RgbColor;
import org.omnaest.react4j.service.internal.service.ThemeColorRole;
import org.omnaest.react4j.service.internal.service.ThemeSettings;
import org.omnaest.react4j.service.internal.service.ThemeTokens;

/**
 * plan-277 T2, AC3: the preset transitions of the state side of the theme configuration (D1), asserted through the read snapshot only. The
 * pre-existing transitions stay in {@link ThemeConfigurationServiceImplTest} untouched.
 */
public class ThemeConfigurationServiceImplPresetTest
{
    private final ThemeConfigurationServiceImpl service = new ThemeConfigurationServiceImpl();

    @Test
    public void testDefaultPresetIsModern()
    {
        assertEquals(ThemePreset.MODERN, this.service.getSettings()
                                                     .getPreset());
    }

    @Test
    public void testPresetSetsThePresetAndNeverChangesEnabledNorTheRest()
    {
        this.service.colorMode(ColorMode.DARK)
                    .addStylesheet("/css/added.css")
                    .primaryColor("#0f766e");
        ThemeSettings before = this.service.getSettings();

        this.service.preset(ThemePreset.TABLER);

        ThemeSettings after = this.service.getSettings();
        assertEquals(ThemePreset.TABLER, after.getPreset());
        assertEquals(before.toBuilder()
                           .preset(ThemePreset.TABLER)
                           .build(),
                     after,
                     "only the preset changes");
        assertTrue(after.isEnabled());
    }

    @Test
    public void testPresetOnADisabledThemeStaysDisabled()
    {
        this.service.disable()
                    .preset(ThemePreset.TABLER);

        ThemeSettings settings = this.service.getSettings();
        assertFalse(settings.isEnabled(), "preset(X) never changes enabled");
        assertEquals(ThemePreset.TABLER, settings.getPreset());
    }

    @Test
    public void testPresetRejectsNullAndLeavesTheSnapshotUnchanged()
    {
        this.service.preset(ThemePreset.TABLER)
                    .colorMode(ColorMode.AUTO);
        ThemeSettings before = this.service.getSettings();

        assertThrows(IllegalArgumentException.class, () -> this.service.preset(null));

        assertEquals(before, this.service.getSettings());
        assertEquals(ThemePreset.TABLER, this.service.getSettings()
                                                     .getPreset());
    }

    @Test
    public void testDisableKeepsThePresetTheColorModeTheSheetsAndTheTokens()
    {
        this.service.preset(ThemePreset.TABLER)
                    .colorMode(ColorMode.DARK)
                    .addStylesheet("/css/added.css")
                    .primaryColor("#0f766e");
        ThemeSettings before = this.service.getSettings();

        this.service.disable();

        ThemeSettings disabled = this.service.getSettings();
        assertFalse(disabled.isEnabled());
        assertEquals(ThemePreset.TABLER, disabled.getPreset());
        assertEquals(ColorMode.DARK, disabled.getColorMode());
        assertEquals(List.of("/css/added.css"), disabled.getStylesheets());
        assertEquals(before.getTokens(), disabled.getTokens());
        assertEquals(ThemeTokens.none()
                                .withColor(ThemeColorRole.PRIMARY, RgbColor.parseHex("#0f766e")),
                     disabled.getTokens());
    }

    @Test
    public void testDisableThenPresetStaysDisabled()
    {
        this.service.disable()
                    .preset(ThemePreset.TABLER);

        assertFalse(this.service.getSettings()
                                .isEnabled());
    }

    @Test
    public void testUseDefaultEnablesSelectsModernAndKeepsTheColorModeTheSheetsAndTheTokens()
    {
        this.service.preset(ThemePreset.TABLER)
                    .colorMode(ColorMode.DARK)
                    .addStylesheet("/css/added.css")
                    .primaryColor("#0f766e");
        ThemeSettings before = this.service.getSettings();

        this.service.useDefault();

        ThemeSettings after = this.service.getSettings();
        assertTrue(after.isEnabled());
        assertEquals(ThemePreset.MODERN, after.getPreset());
        assertEquals(ColorMode.DARK, after.getColorMode());
        assertEquals(List.of("/css/added.css"), after.getStylesheets());
        assertEquals(before.getTokens(), after.getTokens());
    }

    @Test
    public void testUseDefaultAfterDisableOnTablerGivesEnabledModern()
    {
        this.service.preset(ThemePreset.TABLER)
                    .disable()
                    .useDefault();

        ThemeSettings settings = this.service.getSettings();
        assertTrue(settings.isEnabled());
        assertEquals(ThemePreset.MODERN, settings.getPreset());
    }

    @Test
    public void testTheDocumentedWayBackToTablerAfterDisableIsUseDefaultThenPresetTabler()
    {
        this.service.preset(ThemePreset.TABLER)
                    .disable()
                    .useDefault()
                    .preset(ThemePreset.TABLER);

        ThemeSettings settings = this.service.getSettings();
        assertTrue(settings.isEnabled());
        assertEquals(ThemePreset.TABLER, settings.getPreset());
    }

    @Test
    public void testPresetReturnsTheConfigurationForChaining()
    {
        assertSame(this.service, this.service.preset(ThemePreset.TABLER));
    }
}
