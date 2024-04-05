package org.omnaest.react4j.component.form.internal.renderer;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omnaest.react4j.component.form.internal.data.FormData;
import org.omnaest.react4j.component.form.internal.renderer.node.FormNode;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.utils.functional.Provider;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FormRendererImpl implements UIComponentRenderer
{
    private final FormData         formData;
    private final Provider<String> idProvider;

    @Override
    public Location getLocation(LocationSupport locationSupport)
    {
        return locationSupport.createLocation(this.idProvider.get());
    }

    @Override
    public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
    {
        return new FormNode().setElements(this.formData.getElements()
                                                       .stream()
                                                       .map(element -> element.render(location))
                                                       .collect(Collectors.toList()));
    }

    @Override
    public void manageNodeRenderers(NodeRendererRegistry registry)
    {
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
}