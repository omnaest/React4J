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
