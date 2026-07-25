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
package org.omnaest.react4j.service.internal.handler.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.EventHandlerRegistrationSupport;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer.ParentLocationAndComponent;
import org.omnaest.react4j.domain.rendering.components.HandlerEmitter;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.domain.ReactUIInternal;
import org.omnaest.react4j.service.internal.handler.HandlerResolver;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler.MappedData;
import org.omnaest.react4j.service.internal.handler.domain.EventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.handler.ServerHandler;
import org.omnaest.react4j.service.internal.nodes.service.RootNodeResolverService;
import org.omnaest.react4j.service.internal.service.internal.LocationSupportImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * plan-78 Slice 1 walking skeleton: resolves a {@link DataEventHandler} by descending the (cached) component
 * tree obtained from {@link RootNodeResolverService#resolveRootInternal(String)} to the {@link Location} a
 * {@link Target} denotes, instead of looking it up in the map {@code EventHandlerServiceImpl} maintains as a
 * side effect of a tree walk.
 * <p>
 * Descends only the single {@code DEFAULT_CONTEXT_PATH} root (plan-78 Cliff C2: multi-context-path apps are
 * out of scope this slice). Reproduces EXACTLY the {@link Location} computation
 * {@code ReactUIServiceImpl.registerEventHandlers} uses (root seeded with {@link Location#empty()}, each
 * child's {@link Location} computed via {@code component.asRenderer().getLocation(new
 * LocationSupportImpl(parentLocation))}), and prunes to only the branch whose {@link Location} is a prefix of
 * (or equal to) the target path - compared as the raw {@code List<String>}, never via
 * {@link Location#isParentOf(Location)}, which is broken (plan-78 F5: it never consults {@code this}, so it
 * returns {@code true} for any non-empty argument regardless of the receiver).
 * </p>
 * <p>
 * Two-mode traversal (plan-78 F4): when a component's {@code getSubComponents(...)} yields no child on the
 * target path, this falls back to harvesting AT that component, tried in order: (1) a traversal-scoped
 * CAPTURING {@link EventHandlerRegistrationSupport} passed into {@code manageEventHandler(...)} - reaches the
 * {@code manageEventHandler}-based sites (e.g. {@code ButtonImpl} and 6 siblings, plan-78 F3); (2) since
 * plan-78 Slice 2 (Cliff C1-A), a {@code render(...)}-and-harvest pass through a traversal-scoped CAPTURING
 * {@link HandlerEmitter} - reaches components that emit their handler DTO directly inside {@code render(...)}
 * against the {@link RenderingProcessor} instead (e.g. {@code FormRendererImpl} / {@code ButtonFormElementImpl}
 * for the {@code ButtonFormElement} case). Neither mode ever touches the real global handler map - both
 * capture into a traversal-local structure discarded once {@link #resolve(Target, Optional)} returns.
 * </p>
 *
 * @author omnaest
 */
@Service
public class HandlerResolverImpl implements HandlerResolver
{
    @Autowired
    protected RootNodeResolverService rootNodeResolverService;

    @Override
    public Optional<DataEventHandler> resolve(Target target, Optional<Data> data)
    {
        if (target == null || target.get() == null)
        {
            return Optional.empty();
        }
        List<String> targetPath = target.get();

        return this.rootNodeResolverService.resolveRootInternal(ReactUIService.DEFAULT_CONTEXT_PATH)
                                           .flatMap(rootInternal -> this.resolveFromRoots(rootInternal, targetPath, data));
    }

    private Optional<DataEventHandler> resolveFromRoots(ReactUIInternal rootInternal, List<String> targetPath, Optional<Data> data)
    {
        return rootInternal.getRootComponents()
                           .map(rootComponent -> this.descend(rootComponent,
                                                              rootComponent.asRenderer()
                                                                           .getLocation(new LocationSupportImpl(Location.empty())),
                                                              targetPath, data))
                           .filter(Optional::isPresent)
                           .findFirst()
                           .orElseGet(Optional::empty);
    }

    private Optional<DataEventHandler> descend(RenderableUIComponent<?> component, Location location, List<String> targetPath, Optional<Data> data)
    {
        List<String> ownPath = location.get();
        if (!isPrefixOrEqual(ownPath, targetPath))
        {
            // PRUNE: this branch cannot contain the target
            return Optional.empty();
        }
        if (ownPath.equals(targetPath))
        {
            return this.harvestAt(component, location, targetPath, data);
        }

        List<ParentLocationAndComponent> children = component.asRenderer()
                                                             .getSubComponents(location, data)
                                                             .collect(Collectors.toList());
        for (ParentLocationAndComponent child : children)
        {
            if (!(child.getComponent() instanceof RenderableUIComponent))
            {
                continue;
            }
            RenderableUIComponent<?> childComponent = (RenderableUIComponent<?>) child.getComponent();
            Location childLocation = childComponent.asRenderer()
                                                   .getLocation(new LocationSupportImpl(child.getParentLocation()));
            if (isPrefixOrEqual(childLocation.get(), targetPath))
            {
                Optional<DataEventHandler> result = this.descend(childComponent, childLocation, targetPath, data);
                if (result.isPresent())
                {
                    return result;
                }
            }
        }

        // plan-78 F4: getSubComponents yielded no child on the target path - fall back to harvesting AT this
        // component (see class javadoc for the two harvest modes tried).
        return this.harvestAt(component, location, targetPath, data);
    }

    /**
     * Tries both harvest modes at {@code component}, in order: the {@code manageEventHandler} capturing
     * support (cheap, reaches the 7 {@code manageEventHandler}-based sites), then - only if that yields
     * nothing - a {@code render(...)}-and-harvest pass through a capturing {@link HandlerEmitter} (plan-78
     * Cliff C1-A / Slice 2), which reaches components that emit their handler DTO directly inside
     * {@code render(...)} (e.g. {@code FormRendererImpl} / {@code ButtonFormElementImpl}).
     */
    private Optional<DataEventHandler> harvestAt(RenderableUIComponent<?> component, Location location, List<String> targetPath, Optional<Data> data)
    {
        return this.harvest(component)
                   .or(() -> this.harvestByRender(component, location, targetPath, data));
    }

    private Optional<DataEventHandler> harvest(RenderableUIComponent<?> component)
    {
        CapturingEventHandlerRegistrationSupport support = new CapturingEventHandlerRegistrationSupport();
        component.asRenderer()
                 .manageEventHandler(support);
        return support.getCapturedHandler();
    }

    /**
     * plan-78 Cliff C1-A / Slice 2: renders {@code component} through a traversal-scoped CAPTURING
     * {@link RenderingProcessor} whose {@link HandlerEmitter} records every {@code (Target, DataEventHandler)}
     * emitted during that single render pass into a plain map instead of the real global
     * {@code EventHandlerRegistry}, then looks up the exact {@code targetPath}. The rendered {@link Node} tree
     * itself is discarded - only the captured handler map is used. Re-executes whatever registration side
     * effects any NOT-YET-converted call site within this subtree still performs directly against its own
     * field-held registry reference (e.g. {@code FormRendererImpl}'s own {@code onChange} handler this slice) -
     * an accepted, idempotent (plan-14) consequence of converting only two of the eleven sites this slice
     * (plan-78 F3).
     */
    private Optional<DataEventHandler> harvestByRender(RenderableUIComponent<?> component, Location location, List<String> targetPath, Optional<Data> data)
    {
        CapturingRenderingProcessor capturingRenderingProcessor = new CapturingRenderingProcessor();
        component.asRenderer()
                 .render(capturingRenderingProcessor, location, data);
        return capturingRenderingProcessor.getCapturedHandler(Target.from(() -> targetPath));
    }

    private static boolean isPrefixOrEqual(List<String> candidatePrefix, List<String> path)
    {
        return candidatePrefix != null && path != null && candidatePrefix.size() <= path.size()
               && path.subList(0, candidatePrefix.size())
                      .equals(candidatePrefix);
    }

    /**
     * Traversal-scoped {@link EventHandlerRegistrationSupport} that captures whatever a component registers
     * during {@code manageEventHandler(...)} instead of writing it into the real global
     * {@code EventHandlerRegistry} map, so descent by construction never mutates the active handler map.
     */
    private static final class CapturingEventHandlerRegistrationSupport implements EventHandlerRegistrationSupport
    {
        private DataEventHandler capturedHandler;

        @Override
        public EventHandlerRegistrationSupport register(EventHandler eventHandler)
        {
            if (eventHandler != null)
            {
                this.capturedHandler = (data, internalData) ->
                {
                    eventHandler.invoke();
                    return MappedData.builder()
                                     .data(data)
                                     .internalData(internalData)
                                     .build();
                };
            }
            return this;
        }

        @Override
        public EventHandlerRegistrationSupport register(DataEventHandler eventHandler)
        {
            this.capturedHandler = eventHandler;
            return this;
        }

        @Override
        public EventHandlerRegistrationSupport registerAsRerenderingNode()
        {
            // not meaningful for a traversal-scoped capture - no-op, self-chaining
            return this;
        }

        Optional<DataEventHandler> getCapturedHandler()
        {
            return Optional.ofNullable(this.capturedHandler);
        }
    }

    /**
     * Traversal-scoped {@link RenderingProcessor} (plan-78 Cliff C1-A / Slice 2) used ONLY to drive a single
     * {@code render(...)} pass for the harvest-by-render fallback. Its {@link #handlers()} is a
     * {@link CapturingHandlerEmitter} that never touches the real global {@code EventHandlerRegistry}. Its
     * {@link #process(UIComponent, Location, Optional)} is a thin delegate (no wrapper application, no
     * ignored-component bookkeeping) sufficient for this narrow harvest purpose - the returned {@link Node}
     * tree is discarded by the caller, only the captured handler map is used.
     */
    private static final class CapturingRenderingProcessor implements RenderingProcessor
    {
        private final CapturingHandlerEmitter handlerEmitter = new CapturingHandlerEmitter();

        @Override
        public Node process(UIComponent<?> component, Location parentLocation, Optional<Data> data)
        {
            if (!(component instanceof RenderableUIComponent))
            {
                return null;
            }
            return ((RenderableUIComponent<?>) component).asRenderer()
                                                         .render(this, parentLocation, data);
        }

        @Override
        public HandlerEmitter handlers()
        {
            return this.handlerEmitter;
        }

        Optional<DataEventHandler> getCapturedHandler(Target target)
        {
            return this.handlerEmitter.getCapturedHandler(target);
        }
    }

    /**
     * Traversal-scoped {@link HandlerEmitter} that captures whatever a component emits during a
     * resolver-driven {@code render(...)} pass into a plain map instead of registering it in the real global
     * {@code EventHandlerRegistry}, so the harvest-by-render fallback never mutates the active handler map for
     * the sites it reaches through this emitter (see class javadoc).
     */
    private static final class CapturingHandlerEmitter implements HandlerEmitter
    {
        private final Map<Target, DataEventHandler> capturedHandlers = new HashMap<>();

        @Override
        public Handler emitDataEventHandler(Target target, DataEventHandler dataEventHandler)
        {
            if (dataEventHandler == null)
            {
                return null;
            }
            this.capturedHandlers.put(target, dataEventHandler);
            return new ServerHandler(target);
        }

        @Override
        public Handler emitEventHandler(Target target, EventHandler eventHandler)
        {
            if (eventHandler == null)
            {
                return null;
            }
            DataEventHandler wrapped = (data, internalData) ->
            {
                eventHandler.invoke();
                return MappedData.builder()
                                 .data(data)
                                 .internalData(internalData)
                                 .build();
            };
            this.capturedHandlers.put(target, wrapped);
            return new ServerHandler(target);
        }

        Optional<DataEventHandler> getCapturedHandler(Target target)
        {
            return Optional.ofNullable(this.capturedHandlers.get(target));
        }
    }
}
