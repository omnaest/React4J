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
package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Text;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.i18n.I18nText;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.rendering.node.NodeRenderingProcessor;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.nodes.TextNode;

public class TextImpl extends AbstractUIComponent<Text> implements Text
{
    private List<I18nText> texts    = new ArrayList<>();
    private Style          style    = null;
    private Set<Emphasis>  emphasis = EnumSet.noneOf(Emphasis.class);

    public TextImpl(ComponentContext context)
    {
        super(context);
    }

    public TextImpl(ComponentContext context, List<I18nText> texts)
    {
        super(context);
        this.texts = texts;
    }

    public TextImpl(ComponentContext context, List<I18nText> texts, Style style)
    {
        this(context, texts);
        this.style = style;
    }

    public TextImpl(ComponentContext context, List<I18nText> texts, Style style, Set<Emphasis> emphasis)
    {
        this(context, texts, style);
        this.emphasis = emphasis.isEmpty() ? EnumSet.noneOf(Emphasis.class) : EnumSet.copyOf(emphasis);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(TextImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new TextNode().setTexts(TextImpl.this.texts.stream()
                                                                  .map(text -> TextImpl.this.getTextResolver()
                                                                                            .apply(text, location))
                                                                  .collect(Collectors.toList()))
                                     .setStyle(TextImpl.this.style)
                                     .setEmphasis(TextImpl.this.emphasis.isEmpty() ? null : new ArrayList<>(TextImpl.this.emphasis));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                NodeRenderer<TextNode> nodeRenderer = new NodeRenderer<TextNode>() {
                    @Override
                    public String render(TextNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        String texts = node.getTexts()
                                           .stream()
                                           .map(text -> nodeRenderingProcessor.render(text))
                                           .collect(Collectors.joining(" "));
                        texts = TextImpl.this.wrapIntoEmphasis(texts, node.getEmphasis());
                        return node.getStyle() == null ? texts
                                : "<span class=\"" + node.getStyle()
                                                         .toCssClass()
                                  + "\">" + texts + "</span>";
                    }

                };
                registry.register(TextNode.class, NodeRenderType.HTML, nodeRenderer);
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return Stream.empty();
            }

        };
    }

    @Override
    public Text addText(String text)
    {
        this.addText(this.toI18nText(text));
        return this;
    }

    @Override
    public Text addNonTranslatedText(String text)
    {
        this.addText(this.toNonTranslatableI18nText(text));
        return this;
    }

    @Override
    public Text addText(I18nText text)
    {
        this.texts.add(text);
        return this;
    }

    @Override
    public Text withStyle(Style style)
    {
        this.style = style;
        return this;
    }

    @Override
    public Text withEmphasis(Emphasis... emphasis)
    {
        if (emphasis != null)
        {
            Arrays.stream(emphasis)
                  .filter(Objects::nonNull)
                  .forEach(this.emphasis::add);
        }
        return this;
    }

    /**
     * Wraps the given already escaped texts into the element of each emphasis, nested in enum order with the first member outermost. No emphasis (null or
     * empty) returns the texts untouched.
     */
    private String wrapIntoEmphasis(String texts, List<Emphasis> emphasis)
    {
        String wrapped = texts;
        if (emphasis != null)
        {
            List<Emphasis> innermostFirst = emphasis.stream()
                                                    .distinct()
                                                    .sorted(Comparator.reverseOrder())
                                                    .collect(Collectors.toList());
            for (Emphasis member : innermostFirst)
            {
                wrapped = "<" + member.toElementName() + ">" + wrapped + "</" + member.toElementName() + ">";
            }
        }
        return wrapped;
    }

    @Override
    public UIComponentProvider<Text> asTemplateProvider()
    {
        return () -> new TextImpl(this.context, this.texts, this.style, this.emphasis);
    }

}
