package org.omnaest.react4j.service.internal.service;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.omnaest.react4j.service.internal.nodes.CardNode;
import org.omnaest.utils.JSONHelper;
import org.omnaest.utils.MapUtils;

public class NodeHierarchyStaticRendererTest
{

    @Test
    public void testRenderNode() throws Exception
    {
        Map<String, Object> map = MapUtils.<String, Object>builder()
                                          .put("type", "CARD")
                                          .put("adjust", (Object) true)
                                          .build();

        CardNode cardNode = JSONHelper.toObjectWithType(map, CardNode.class);
        assertNotNull(cardNode);

        //        System.out.println(JSONHelper.prettyPrint(cardNode));
    }

}
