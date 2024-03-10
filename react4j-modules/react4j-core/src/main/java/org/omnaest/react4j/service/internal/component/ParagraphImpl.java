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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Anker;
import org.omnaest.react4j.domain.AnkerButton;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Paragraph;
import org.omnaest.react4j.domain.UIComponent;
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
import org.omnaest.react4j.service.internal.nodes.ParagraphNode;
import org.omnaest.utils.ClassUtils;
import org.omnaest.utils.ClassUtils.Resource;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.template.TemplateUtils;

public class ParagraphImpl extends AbstractUIComponent<Paragraph> implements Paragraph
{
    private List<UIComponent<?>> elements              = new ArrayList<>();
    private boolean              bold                  = false;
    private boolean              addLineBreakAfterText = false;

    public ParagraphImpl(ComponentContext context)
    {
        super(context);
    }

    public ParagraphImpl(ComponentContext context, List<UIComponent<?>> elements, boolean bold)
    {
        super(context);
        this.elements = elements;
        this.bold = bold;
    }

    @Override
    public Paragraph addText(I18nText text)
    {
        return this.addText(null, text);
    }

    @Override
    public Paragraph addText(StandardIcon icon, I18nText text)
    {
        Composite composite = this.getUiComponentFactory()
                                  .newComposite();
        if (icon != null)
        {
            composite.addComponent(this.getUiComponentFactory()
                                       .newIcon()
                                       .from(icon));
        }
        if (text != null)
        {
            composite.addComponent(this.getUiComponentFactory()
                                       .newText()
                                       .addText(text));
        }
        this.elements.add(composite);

        if (this.addLineBreakAfterText)
        {
            this.addLineBreak();
        }

        return this;
    }

    @Override
    public Paragraph addHeading(String text, int level)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newHeading()
                              .withLevel(level)
                              .withText(text));
        return this;
    }

    @Override
    public Paragraph addLineBreak()
    {
        this.elements.add(this.getUiComponentFactory()
                              .newLineBreak());
        return this;
    }

    @Override
    public Paragraph addImage(String name, String imageName)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newImage()
                              .withName(name)
                              .withImage(imageName));
        return this;
    }

    @Override
    public Paragraph withBoldStyle()
    {
        return this.withBoldStyle(true);
    }

    @Override
    public Paragraph withBoldStyle(boolean bold)
    {
        this.bold = bold;
        return this;
    }

    @Override
    public Paragraph addTextsByClasspathResource(String resourcePath)
    {
        StringUtils.splitToStreamByLineSeparator(ClassUtils.loadResource(this, resourcePath)
                                                           .map(Resource::asString)
                                                           .orElseThrow(() -> new IllegalStateException("Resource could not be loaded: " + resourcePath)))
                   .forEach(this::addText);
        return this;
    }

    public Paragraph addTexts(String... texts)
    {
        return this.addTexts(Arrays.asList(texts));
    }

    public Paragraph addTexts(List<String> texts)
    {
        Optional.ofNullable(texts)
                .orElse(Collections.emptyList())
                .forEach(this::addText);
        return this;
    }

    @Override
    public Paragraph addText(String text)
    {
        return this.addText(this.toI18nText(text));
    }

    @Override
    public Paragraph addNonTranslatedText(String text)
    {
        return this.addText(this.toNonTranslatableI18nText(text));
    }

    @Override
    public Paragraph addText(StandardIcon icon, String text)
    {
        return this.addText(icon, this.toI18nText(text));
    }

    @Override
    public Paragraph addLink(Consumer<Anker> ankerConsumer)
    {
        Anker anker = this.getUiComponentFactory()
                          .newAnker();
        ankerConsumer.accept(anker);
        this.elements.add(anker);
        return this;
    }

    @Override
    public <E> Paragraph addLinks(Collection<E> sourceElements, BiConsumer<Anker, E> ankerAndSourceElementConsumer)
    {
        Optional.ofNullable(sourceElements)
                .orElse(Collections.emptyList())
                .forEach(sourceElement -> this.addLink(anker -> ankerAndSourceElementConsumer.accept(anker, sourceElement)));
        return this;
    }

    @Override
    public <E> Paragraph withElements(Collection<E> sourceElements, BiConsumer<Paragraph, E> paragraphAndSourceElementConsumer)
    {
        Optional.ofNullable(sourceElements)
                .orElse(Collections.emptyList())
                .forEach(sourceElement -> paragraphAndSourceElementConsumer.accept(this, sourceElement));
        return this;
    }

    @Override
    public Paragraph addLinkButton(Consumer<AnkerButton> ankerButtonConsumer)
    {
        AnkerButton anker = this.getUiComponentFactory()
                                .newAnkerButton();
        ankerButtonConsumer.accept(anker);
        this.elements.add(anker);
        return this;
    }

    @Override
    public Paragraph addComponent(UIComponent<?> component)
    {
        this.elements.add(component);
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(ParagraphImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                return new ParagraphNode().setId(ParagraphImpl.this.getId())
                                          .setBold(ParagraphImpl.this.bold)
                                          .setElements(ParagraphImpl.this.elements.stream()
                                                                                  .map(element -> renderingProcessor.process(element, location))
                                                                                  .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(ParagraphNode.class, NodeRenderType.HTML, new NodeRenderer<ParagraphNode>()
                {
                    @Override
                    public String render(ParagraphNode node, NodeRenderingProcessor nodeRenderingProcessor)
                    {
                        return TemplateUtils.builder()
                                            .useTemplateClassResource(this.getClass(), "/render/templates/html/paragraph.html")
                                            .add("elements", node.getElements()
                                                                 .stream()
                                                                 .map(nodeRenderingProcessor::render)
                                                                 .collect(Collectors.toList()))
                                            .build()
                                            .get();
                    }
                });
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return ParagraphImpl.this.elements.stream()
                                                  .map(element -> ParentLocationAndComponent.of(parentLocation, element));
            }

        };
    }

    @Override
    public UIComponentProvider<Paragraph> asTemplateProvider()
    {
        return () -> new ParagraphImpl(this.context, this.elements.stream()
                                                                  .collect(Collectors.toList()),
                                       this.bold);
    }

    @Override
    public Paragraph withLineBreakAfterEachText()
    {
        return this.withLineBreakAfterEachText(true);
    }

    @Override
    public Paragraph withLineBreakAfterEachText(boolean enabled)
    {
        this.addLineBreakAfterText = enabled;
        return this;
    }
}
