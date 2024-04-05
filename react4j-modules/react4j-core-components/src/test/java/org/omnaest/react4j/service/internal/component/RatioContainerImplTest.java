package org.omnaest.react4j.service.internal.component;

import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;
import org.omnaest.react4j.domain.RatioContainer.Ratio;
import org.omnaest.utils.EnumUtils;

/**
 * @see RatioContainerImpl
 * @author omnaest
 */
public class RatioContainerImplTest
{
    @Test
    public void testRatioEnumValueAvailable() throws Exception
    {
        Stream.of(Ratio.values())
              .forEach(ratio -> assertTrue(EnumUtils.mapByName(ratio, org.omnaest.react4j.service.internal.nodes.RatioContainerNode.Ratio.class)
                                                    .isPresent()));

    }
}
