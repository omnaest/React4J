package org.omnaest.react4j.domain.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;

public class I18nHashBasedTest
{
    @Test
    public void testGetTextKey() throws Exception
    {
        Location location = Location.of(Location.of("parentid"), "subcomponentid");
        assertEquals("parentid.subcomponentid.1503998791_i_love_you__1503998791", I18nText.of(Locations.of(location), "I love you!", null)
                                                                                          .getTextKey(location));
    }
}
