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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.DropTarget;
import org.omnaest.react4j.domain.DropTarget.DropEvent;
import org.omnaest.react4j.domain.DropTarget.DropRelation;
import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.rendering.components.LocationSupport;
import org.omnaest.react4j.domain.rendering.components.RenderingProcessor;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.omnaest.react4j.service.internal.nodes.DropTargetNode;

/**
 * @see DropTarget
 * @author omnaest
 */
public class DropTargetImpl extends AbstractUIComponentAndContentHolder<DropTarget> implements DropTarget
{
    private static final String FIELD_KEY_PREFIX = "droptarget.";

    private UIComponent<?>      content;
    private Consumer<DropEvent> onDropHandler;

    /**
     * Memoized by {@code getLocation(...)} below and read by {@code manageEventHandler(...)}.
     * <p>
     * This is safe rather than a stale-state hazard because {@code getLocation(...)} is a pure function of
     * this instance's own id and the caller-supplied parent {@link Location}, and a given {@link DropTargetImpl}
     * instance is only ever reached via ONE structural path in a given render. So ANY call to
     * {@code getLocation(...)} on this instance - whether from a registration walk or from the separate render
     * walk - always recomputes the identical value, and every {@link DropTargetImpl} that reaches
     * {@code manageEventHandler(...)} has had {@code getLocation(...)} called on it earlier in the same
     * request. This field therefore never holds a value stale for a DIFFERENT structural position - it either
     * holds the one correct value for this instance, or it has not been set yet.
     * <p>
     * One caveat: on {@code EventHandlerRegistrationSupportImpl.registerAsRerenderingNode()}'s
     * {@code RerenderedNodeProvider} path (see {@code ReactUIServiceImpl}), a subtree ROOT's {@link Location}
     * is passed in by the caller and never recomputed via {@code getLocation(...)} during that walk. That
     * cannot apply to this class: only a component that itself calls {@code registerAsRerenderingNode()}
     * becomes such a root, and {@link DropTargetImpl} never does.
     */
    private volatile Location   lastResolvedLocation;

    public DropTargetImpl(ComponentContext context)
    {
        super(context);
    }

    public DropTargetImpl(ComponentContext context, UIComponent<?> content, Consumer<DropEvent> onDropHandler)
    {
        super(context);
        this.content = content;
        this.onDropHandler = onDropHandler;
    }

    @Override
    public DropTarget onDrop(Consumer<DropEvent> handler)
    {
        this.onDropHandler = handler;
        return this;
    }

    @Override
    public DropTarget withContent(UIComponent<?> component)
    {
        this.content = component;
        return this;
    }

    @Override
    public UIComponentRenderer asRenderer()
    {
        return new UIComponentRenderer() {

            @Override
            public Location getLocation(LocationSupport locationSupport)
            {
                Location location = locationSupport.createLocation(DropTargetImpl.this.getId());
                DropTargetImpl.this.lastResolvedLocation = location;
                return location;
            }

            @Override
            public Node render(RenderingProcessor renderingProcessor, Location location, Optional<Data> data)
            {
                boolean gated = DropTargetImpl.this.onDropHandler != null && location != null;
                return new DropTargetNode().setContent(Optional.ofNullable(DropTargetImpl.this.content)
                                                               .map(content -> renderingProcessor.process(content, location))
                                                               .orElse(null))
                                           .setDragIdFieldKey(gated ? DropTargetImpl.this.dragIdFieldKey(location) : null)
                                           .setRelationFieldKey(gated ? DropTargetImpl.this.relationFieldKey(location) : null)
                                           .setDropTarget(gated ? Target.from(location) : Target.empty());
            }

            @Override
            public void manageNodeRenderers(NodeRendererRegistry registry)
            {
                registry.register(DropTargetNode.class, NodeRenderType.HTML,
                                  (node, nodeRenderingProcessor) -> "<div>" + Optional.ofNullable(node.getContent())
                                                                                      .map(nodeRenderingProcessor::render)
                                                                                      .orElse("")
                                                                    + "</div>");
            }

            @Override
            public void manageEventHandler(EventHandlerRegistrationSupport eventHandlerRegistrationSupport)
            {
                // Gated (react4j-gate-optin-handler-on-existing-node): register a handler only when the app
                // called onDrop(...). A DropTarget this was never called on registers nothing at all.
                if (DropTargetImpl.this.onDropHandler != null)
                {
                    Location location = DropTargetImpl.this.lastResolvedLocation;
                    // render() gates its live Target on ITS OWN location parameter, not on this field - if
                    // lastResolvedLocation were ever null here while onDropHandler is set, render() would
                    // still emit a live, routable Target while this method registered nothing, so a client
                    // drop would dispatch to a Target with no handler and silently do nothing. Registration
                    // and rendering must agree about whether this target is live: fail loudly here instead of
                    // skipping silently, so that disagreement is impossible to miss.
                    if (location == null)
                    {
                        throw new IllegalStateException("DropTargetImpl.manageEventHandler(...) was called with onDrop(...) configured but "
                                                        + "getLocation(...) has not run for this instance yet - getLocation(...) must run before manageEventHandler(...).");
                    }
                    String dragIdFieldKey = DropTargetImpl.this.dragIdFieldKey(location);
                    String relationFieldKey = DropTargetImpl.this.relationFieldKey(location);
                    eventHandlerRegistrationSupport.register(DropTargetImpl.this.buildDropHandler(dragIdFieldKey, relationFieldKey));
                }
            }

            @Override
            public Stream<ParentLocationAndComponent> getSubComponents(Location parentLocation)
            {
                return DropTargetImpl.this.content != null ? Stream.of(ParentLocationAndComponent.of(parentLocation, DropTargetImpl.this.content))
                        : Stream.empty();
            }

        };
    }

    /**
     * The registered handler reads the dragId/relation the CLIENT already wrote into
     * {@code dragIdFieldKey}/{@code relationFieldKey} before dispatching (write-then-dispatch, plan-235 S9.3) -
     * mirroring {@code TreeTableRendererImpl.buildFilterChangeHandler}'s shape, except this one actually acts
     * on the value rather than merely giving the target something to route to, since a drop is a genuine app
     * event rather than persisted view state. A malformed/absent value is ignored defensively rather than
     * failing the round trip - the same discipline {@code TreeTableRendererImpl.readSorts} applies to a stale
     * encoded sort entry.
     */
    private DataEventHandler buildDropHandler(String dragIdFieldKey, String relationFieldKey)
    {
        return (eventData, internalData) ->
        {
            String dragId = eventData.getFieldValue(dragIdFieldKey)
                                     .map(Value::asString)
                                     .filter(value -> value != null && !value.isEmpty())
                                     .orElse(null);
            String relationRaw = eventData.getFieldValue(relationFieldKey)
                                          .map(Value::asString)
                                          .orElse(null);
            if (dragId != null && relationRaw != null)
            {
                try
                {
                    DropRelation relation = DropRelation.valueOf(relationRaw);
                    DropTargetImpl.this.onDropHandler.accept(DropEvent.of(dragId, relation));
                }
                catch (IllegalArgumentException e)
                {
                    // malformed/stale relation value - ignore defensively rather than failing the round trip
                }
            }
            return DataEventHandler.MappedData.builder()
                                              .data(eventData)
                                              .internalData(internalData)
                                              .build();
        };
    }

    /**
     * Cliff 1' (plan-235 S9.2): generated server-side from this {@link DropTarget}'s own {@link Location} -
     * mirrors {@code TreeTableRendererImpl.filterFieldKey(Location, String)} exactly. No magic string crosses
     * the TS/Java boundary; the client writes into whatever key it was handed on the rendered node.
     */
    private String dragIdFieldKey(Location location)
    {
        return FIELD_KEY_PREFIX + String.join(".", location.get()) + ".dragId";
    }

    private String relationFieldKey(Location location)
    {
        return FIELD_KEY_PREFIX + String.join(".", location.get()) + ".relation";
    }

    @Override
    public UIComponentProvider<DropTarget> asTemplateProvider()
    {
        return () -> new DropTargetImpl(this.context, this.content, this.onDropHandler);
    }
}
