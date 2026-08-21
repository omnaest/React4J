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
package org.omnaest.react4j.service.internal.service.internal;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent.UIComponentWrapper;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.internal.handler.EventHandlerRegistry;
import org.omnaest.react4j.service.internal.nodes.context.UIContextDataNode;
import org.omnaest.utils.MapperUtils;
import org.omnaest.utils.SetUtils;
import org.omnaest.utils.stream.FilterMapper;

/**
 * @author omnaest
 */
public class RenderingProcessorImpl implements RenderingProcessor
{
    private UIComponentFactory componentFactory;
    private HandlerEmitter     handlerEmitter;

    public RenderingProcessorImpl(UIComponentFactory componentFactory, EventHandlerRegistry eventHandlerRegistry)
    {
        this.componentFactory = componentFactory;
        this.handlerEmitter = new HandlerEmitterImpl(eventHandlerRegistry);
    }

    @Override
    public HandlerEmitter handlers()
    {
        return this.handlerEmitter;
    }

    @Override
    public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
    {
        Set<UIComponent<?>> ignoredComponents = Collections.emptySet();
        return this.process(component, parentLocation, data, ignoredComponents);
    }

    public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data, Set<UIComponent<?>> ignoredComponents)
    {
        FilterMapper<UIComponent<?>, RenderableUIComponent<?>> renderableUIComponentFilterMapper = MapperUtils.filterMapper(iComponent -> iComponent instanceof RenderableUIComponent,
                                                                                                                            iComponent -> (RenderableUIComponent<?>) iComponent);

        Set<UIComponent<?>> currentIgnoredComponents = SetUtils.toNew(ignoredComponents);
        return Optional.ofNullable(component)
                       .filter(renderableUIComponentFilterMapper)
                       .map(renderableUIComponentFilterMapper)
                       .map(this.createComponentWrapperMapper(currentIgnoredComponents))
                       .filter(renderableUIComponentFilterMapper)
                       .map(renderableUIComponentFilterMapper)
                       .map(this.createComponentRenderer(parentLocation, data, currentIgnoredComponents))
                       .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private Function<RenderableUIComponent<?>, UIComponent<?>> createComponentWrapperMapper(Set<UIComponent<?>> currentIgnoredComponents)
    {
        return iComponent ->
        {
            if (currentIgnoredComponents.contains(iComponent))
            {
                return iComponent;
            }
            else
            {
                UIComponentWrapper<UIComponent<?>> uiComponentWrapper = (UIComponentWrapper<UIComponent<?>>) iComponent.getWrapper();
                UIComponent<?> wrapperComponent = uiComponentWrapper.apply(this.componentFactory, iComponent);

                if (wrapperComponent != iComponent)
                {
                    currentIgnoredComponents.add(iComponent);
                }

                return wrapperComponent;
            }
        };
    }

    private Function<RenderableUIComponent<?>, Node> createComponentRenderer(Location parentLocation, Optional<Data> data, Set<UIComponent<?>> currentIgnoredComponents)
    {
        return component ->
        {
            LocationSupport locationSupport = new LocationSupportImpl(parentLocation);
            UIComponentRenderer renderer = component.asRenderer();
            Location location = renderer.getLocation(locationSupport);
            Node node = renderer.render(this.createFilteringRenderingProcessor(currentIgnoredComponents, data), location, data);

            component.getUIContextInitialDataIfPresent()
                     .ifPresent(uiContextData -> node.setUiContextData(UIContextDataNode.builder()
                                                                                        .contextId(uiContextData.getContextIdCreator()
                                                                                                                .apply(location))
                                                                                        .data(uiContextData.getData()

                                                                                                           .toMap())
                                                                                        .internalData(uiContextData.getInternalData()
                                                                                                                   .toMap())
                                                                                        .build()));
            return node;
        };
    }

    /**
     * @param ambientData
     *            the submitted {@link Data} the component being rendered received, inherited by any child it
     *            processes without naming data of its own - see {@link #process(UIComponent, Location)} below.
     */
    private RenderingProcessor createFilteringRenderingProcessor(Set<UIComponent<?>> currentIgnoredComponents, Optional<Data> ambientData)
    {
        return new RenderingProcessor() {
            @Override
            public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
            {
                return RenderingProcessorImpl.this.process(component, parentLocation, data, currentIgnoredComponents);
            }

            /**
             * The submitted {@link Data} flows DOWN the tree: a child rendered without explicit data inherits
             * its parent's rather than starting from nothing.
             * <p>
             * <b>Why this override exists.</b> The interface default hard-codes {@code Optional.empty()}, and
             * roughly thirty renderers call this two-argument form to process their children - every container,
             * card, composite, grid and scroll region in the library. So the submitted Data reached only the one
             * component an event resolved to, and every descendant rendered as though nothing had been
             * submitted: it looked its own fields up in an empty map and fell back to its defaults.
             * <p>
             * <b>Why it went unnoticed.</b> A component whose own control is clicked IS the resolved node, so it
             * always saw its data. The loss showed only when the event came from somewhere else on the page -
             * and then touching the component directly appeared to "fix" it, which reads as a refresh problem
             * rather than a lost one. Measured live: submitting a chat message re-rendered the table beside it
             * in default mode with no filters, discarding a view the user had chosen and a filter that had just
             * been applied.
             * <p>
             * Overriding here rather than at each of the thirty call sites keeps the rule in one place: pass
             * data explicitly to re-root it (as {@code RerenderingContainerImpl} does for its own content), say
             * nothing to inherit it.
             */
            @Override
            public Node process(UIComponent<?> component, Location parentLocation)
            {
                return this.process(component, parentLocation, ambientData);
            }

            @Override
            public HandlerEmitter handlers()
            {
                // plan-78 Cliff C1-A: every component's render(...) receives THIS filtering wrapper, never
                // RenderingProcessorImpl directly - without this override handlers() would silently fall back
                // to the interface's no-op default for every rendered component.
                return RenderingProcessorImpl.this.handlers();
            }
        };
    }

}
