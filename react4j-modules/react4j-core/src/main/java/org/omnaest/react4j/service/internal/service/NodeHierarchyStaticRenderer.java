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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;
import org.omnaest.react4j.component.value.i18n.internal.node.I18nTextNode;
import org.omnaest.react4j.component.value.node.ValueNode;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.service.internal.nodes.NodeHierarchy;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.ReflectionUtils;
import org.springframework.stereotype.Component;

@Component
public class NodeHierarchyStaticRenderer
{
    public static interface NodeHierarchyRenderingProcessor extends NodeRendererRegistry
    {
        public String render(NodeHierarchy nodeHierarchy, NodeRenderType nodeRenderType);
    }

    public NodeHierarchyRenderingProcessor newNodeRenderingProcessor()
    {
        return new NodeHierarchyRenderingProcessorImpl( /* this.localeService.getRequestLocaleKey() */
                                                       LocalizedTextResolverService.DEFAULT_LOCALE_KEY);
    }

    private static class NodeHierarchyRenderingProcessorImpl implements NodeHierarchyRenderingProcessor
    {
        private Map<NodeRenderType, Map<String, NodeRenderer<Node>>> rendererTypeToNodeTypeToNodeRenderer = new HashMap<>();
        private String                                               locale;

        public NodeHierarchyRenderingProcessorImpl(String locale)
        {
            super();
            this.locale = locale;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <N extends Node> NodeHierarchyRenderingProcessorImpl register(Class<N> nodeType, NodeRenderType nodeRenderType, NodeRenderer<N> nodeRenderer)
        {
            this.rendererTypeToNodeTypeToNodeRenderer.computeIfAbsent(nodeRenderType, nrt -> new HashMap<>())
                                                     .put(ClassUtils.newInstance((Class<Node>) nodeType)
                                                                    .map(Node::getType)
                                                                    .orElse(""),
                                                          (NodeRenderer<Node>) nodeRenderer);
            return this;
        }

        @Override
        public String render(NodeHierarchy nodeHierarchy, NodeRenderType nodeRenderType)
        {
            Map<String, NodeRenderer<Node>> nodeTypeToNodeRenderer = this.rendererTypeToNodeTypeToNodeRenderer.getOrDefault(nodeRenderType,
                                                                                                                            Collections.emptyMap());

            return this.renderNode(nodeTypeToNodeRenderer, nodeHierarchy.getRoot());
        }

        private String renderNode(Map<String, NodeRenderer<Node>> nodeTypeToNodeRenderer, Node node)
        {
            NodeRenderer<Node> nodeRenderer = Optional.ofNullable(node)
                                                      .map(Node::getType)
                                                      .map(nodeTypeToNodeRenderer::get)
                                                      .orElseGet(() -> new DefaultNodeRenderer());
            NodeRenderingProcessor nodeRenderingProcessor = new NodeRenderingProcessor()
            {

                @Override
                public String render(I18nTextValue text)
                {
                    return StringEscapeUtils.escapeHtml4(Optional.ofNullable(text)
                                                                 .map(I18nTextValue::getLocaleToText)
                                                                 .map(localeToText -> localeToText.get(NodeHierarchyRenderingProcessorImpl.this.locale))
                                                                 .orElse(""));
                }

                @Override
                public String render(ValueNode value)
                {
                    if (value instanceof I18nTextNode)
                    {
                        return this.render(((I18nTextNode) value).getValue());
                    }
                    else
                    {
                        return null;
                    }
                }

                @Override
                public String render(Node node)
                {
                    return NodeHierarchyRenderingProcessorImpl.this.renderNode(nodeTypeToNodeRenderer, node);
                }
            };
            return nodeRenderer.render(node, nodeRenderingProcessor);
        }

        private static class DefaultNodeRenderer implements NodeRenderer<Node>
        {
            @SuppressWarnings("unchecked")
            @Override
            public String render(Node node, NodeRenderingProcessor nodeRenderingProcessor)
            {
                return Optional.ofNullable(node)
                               .map(iNode -> ReflectionUtils.of((Class<Node>) iNode.getClass())
                                                            .getFields()
                                                            .map(field -> field.getValueFrom(iNode))
                                                            .flatMap(value ->
                                                            {
                                                                if (value instanceof Collection)
                                                                {
                                                                    return ((Collection<?>) value).stream();
                                                                }
                                                                else
                                                                {
                                                                    return Stream.of(value);
                                                                }
                                                            })
                                                            .filter(value -> value instanceof Node)
                                                            .map(MapperUtils.identityCast(Node.class))
                                                            .map(nodeRenderingProcessor::render)
                                                            .collect(Collectors.joining()))
                               .orElse("");
            }

        }

    }

}
