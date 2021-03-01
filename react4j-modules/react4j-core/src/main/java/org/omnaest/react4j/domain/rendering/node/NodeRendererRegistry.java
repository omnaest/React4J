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
package org.omnaest.react4j.domain.rendering.node;

import java.util.List;
import java.util.function.Function;

import org.omnaest.react4j.domain.raw.Node;

public interface NodeRendererRegistry
{
    public <N extends Node> NodeRendererRegistry register(Class<N> nodeType, NodeRenderType renderType, NodeRenderer<N> nodeRenderer);

    @Deprecated
    public <N extends Node> NodeRendererRegistry registerChildMapper(Class<N> nodeType, NodeToChildMapper<N> childMapper);

    @Deprecated
    public <N extends Node> NodeRendererRegistry registerChildrenMapper(Class<N> nodeType, NodeToChildrenMapper<N> childrenMapper);

    public static interface NodeToChildMapper<N extends Node> extends Function<N, Node>
    {
    }

    public static interface NodeToChildrenMapper<N extends Node> extends Function<N, List<? extends Node>>
    {
    }
}
