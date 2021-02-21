package org.omnaest.react4j.service.internal.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Icon;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UnsortedList;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.nodes.UnsortedListNode;

public class UnsortedListImpl extends AbstractUIComponent<UnsortedList> implements UnsortedList
{
    private List<UIComponent<?>> elements           = new ArrayList<>();
    private boolean              enableBulletPoints = false;

    public UnsortedListImpl(ComponentContext context)
    {
        super(context);
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer()
        {
            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                return locationSupport.createLocation(UnsortedListImpl.this.getId());
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location)
            {
                return new UnsortedListNode().setEnableBulletPoints(UnsortedListImpl.this.enableBulletPoints)
                                             .setElements(UnsortedListImpl.this.elements.stream()
                                                                                        .map(component -> renderingProcessor.process(component, location))
                                                                                        .collect(Collectors.toList()));
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                // TODO Auto-generated method stub
            }

            @Override
            public Stream<UIComponent<?>> getSubComponents()
            {
                return UnsortedListImpl.this.elements.stream();
            }

        };
    }

    @Override
    public UnsortedList addText(Icon.StandardIcon icon, String text)
    {
        this.elements.add(this.getUiComponentFactory()
                              .newComposite()
                              .addComponents(Arrays.asList(this.getUiComponentFactory()
                                                               .newIcon()
                                                               .from(icon),
                                                           this.getUiComponentFactory()
                                                               .newText()
                                                               .addText(text))));
        return this;
    }

    @Override
    public UnsortedList addText(String text)
    {
        return this.addText(null, text);
    }

    @Override
    public UnsortedList enableBulletPoints()
    {
        return this.enableBulletPoints(true);
    }

    @Override
    public UnsortedList enableBulletPoints(boolean enableBulletPoints)
    {
        this.enableBulletPoints = enableBulletPoints;
        return this;
    }

    @Override
    public UnsortedList addEntry(UIComponent<?> component)
    {
        this.elements.add(component);
        return this;
    }

    @Override
    public UnsortedList addEntries(List<UIComponent<?>> components)
    {
        Optional.ofNullable(components)
                .orElse(Collections.emptyList())
                .forEach(this::addEntry);
        return this;
    }

}