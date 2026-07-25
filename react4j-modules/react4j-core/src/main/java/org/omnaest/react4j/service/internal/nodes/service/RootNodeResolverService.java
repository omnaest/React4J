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
package org.omnaest.react4j.service.internal.nodes.service;

import java.util.Optional;

import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.service.internal.domain.ReactUIInternal;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;

/**
 * Resolves a node
 *
 * @author omnaest
 */
public interface RootNodeResolverService
{
    public NodeHierarchy resolveNodeHierarchy(String contextPath);

    public NodeHierarchy resolveDefaultNodeHierarchy();

    public String renderDefaultNodeHierarchyAsStatic(NodeRenderType nodeRenderType);

    /**
     * plan-78 Slice 1: exposes the (cached, or freshly rebuilt - see plan-78 F1) {@link ReactUIInternal} for a
     * context path, so a {@link org.omnaest.react4j.service.internal.handler.HandlerResolver} can descend its
     * component tree directly instead of relying on a side-effect-populated lookup map. Empty when no UI has
     * been registered for the given context path.
     *
     * @param contextPath
     * @return
     */
    public Optional<ReactUIInternal> resolveRootInternal(String contextPath);
}
