package org.omnaest.react4j.service.internal.service.internal.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration;
import org.omnaest.react4j.domain.configuration.ThemeConfiguration.ColorMode;
import org.omnaest.react4j.service.internal.service.ThemeSettings;

/**
 * plan-274 S3: the state side of the theme configuration (framework boundary object), tested through its public interface and the read
 * snapshot only.
 */
public class ThemeConfigurationServiceImplTest
{
    private final ThemeConfigurationServiceImpl service = new ThemeConfigurationServiceImpl();

    @Test
    public void testDefaultStateIsModernEnabledLightWithoutAddedStylesheets()
    {
        ThemeSettings settings = this.service.getSettings();

        assertTrue(settings.isEnabled());
        assertEquals(ColorMode.LIGHT, settings.getColorMode());
        assertTrue(settings.getStylesheets()
                           .isEmpty());
    }

    @Test
    public void testDisableAndUseDefaultToggleTheStateAndPreserveColorModeAndStylesheets()
    {
        this.service.colorMode(ColorMode.DARK)
                    .addStylesheet("/css/added.css")
                    .disable();

        ThemeSettings disabled = this.service.getSettings();
        assertFalse(disabled.isEnabled());

        this.service.useDefault();

        ThemeSettings reenabled = this.service.getSettings();
        assertTrue(reenabled.isEnabled());
        assertEquals(ColorMode.DARK, reenabled.getColorMode());
        assertEquals(List.of("/css/added.css"), reenabled.getStylesheets());
    }

    @Test
    public void testEveryCallReturnsTheConfigurationForChaining()
    {
        assertSame(this.service, this.service.useDefault());
        assertSame(this.service, this.service.disable());
        assertSame(this.service, this.service.colorMode(ColorMode.AUTO));
        assertSame(this.service, this.service.addStylesheet("/css/a.css"));
    }

    @Test
    public void testAddedStylesheetsKeepTheirInsertionOrder()
    {
        this.service.addStylesheet("/css/z.css")
                    .addStylesheet("/css/a.css");

        assertEquals(List.of("/css/z.css", "/css/a.css"), this.service.getSettings()
                                                                      .getStylesheets());
    }

    @Test
    public void testAddStylesheetRejectsNullAndBlankAndLeavesTheStateUntouched()
    {
        ThemeConfiguration configuration = this.service;

        assertThrows(IllegalArgumentException.class, () -> configuration.addStylesheet(null));
        assertThrows(IllegalArgumentException.class, () -> configuration.addStylesheet(""));
        assertThrows(IllegalArgumentException.class, () -> configuration.addStylesheet("   "));

        assertTrue(this.service.getSettings()
                               .getStylesheets()
                               .isEmpty());
    }

    @Test
    public void testColorModeRejectsNull()
    {
        assertThrows(IllegalArgumentException.class, () -> this.service.colorMode(null));

        assertEquals(ColorMode.LIGHT, this.service.getSettings()
                                                  .getColorMode());
    }

    @Test
    public void testAnAlreadyReturnedSnapshotIsNotModifiedByLaterChanges()
    {
        ThemeSettings before = this.service.getSettings();

        this.service.disable()
                    .colorMode(ColorMode.DARK)
                    .addStylesheet("/css/later.css");

        assertTrue(before.isEnabled());
        assertEquals(ColorMode.LIGHT, before.getColorMode());
        assertTrue(before.getStylesheets()
                         .isEmpty());
    }
}
