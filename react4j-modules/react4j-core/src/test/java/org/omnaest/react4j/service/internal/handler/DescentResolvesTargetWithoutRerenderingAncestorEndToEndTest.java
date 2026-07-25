package org.omnaest.react4j.service.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.domain.context.data.Data;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.handler.domain.DataEventHandler;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * plan-78 Slice 1 walking skeleton: proves a server event handler can be resolved by DESCENDING the cached
 * component tree to a {@link org.omnaest.react4j.domain.Location} - {@link HandlerResolver} - instead of being
 * looked up in {@code EventHandlerServiceImpl.handlers}. Copies the {@code RerenderingSiblingButtonClickEndToEndTest}
 * seam-test shape (real Spring service graph, {@code GET /ui} + {@code POST /ui/event} via MockMvc, Target
 * extraction via a recursive JsonNode walker).
 *
 * @see HandlerResolver
 * @see HandlerResolverImpl
 */
@SpringBootTest(classes = DescentResolvesTargetWithoutRerenderingAncestorEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class DescentResolvesTargetWithoutRerenderingAncestorEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @Autowired
    private HandlerResolver           handlerResolver;

    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    /**
     * AC-1a: a {@code Button} with an {@code onClick} placed OUTSIDE any {@code RerenderingContainer} - the
     * exact gap the map exists to paper over ({@code registerEventHandlers} only re-runs at {@code GET /ui} or
     * via a {@code RerenderedNodeProvider} re-registration). Clicking it must fire the handler exactly once and
     * return a non-null response.
     */
    @Test
    public void testClickingButtonWithoutRerenderingAncestorFiresHandlerExactlyOnce() throws Exception
    {
        AtomicInteger clicks = new AtomicInteger();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newButton()
                                                                                                   .withName("Plain")
                                                                                                   .onClick(clicks::incrementAndGet)));

        List<Target> buttonTargets = this.extractOnClickTargets(this.renderUI());
        assertEquals(1, buttonTargets.size(), "expected exactly one rendered button with a SERVER onClick handler");
        Target buttonTarget = buttonTargets.get(0);

        // Note: unlike RerenderingSiblingButtonClickEndToEndTest's fixture, this button has NO rerendering
        // ancestor, so no RerenderedNodeProvider is registered for its Target and rerenderTargetNode legitimately
        // returns empty (its "targetNode" JSON field is null) - that is orthogonal to handler resolution/firing,
        // which is what AC-1a asserts: the overall response body is non-null and the handler fired exactly once.
        JsonNode clickResponse = this.clickButton(buttonTarget);
        assertFalse(clickResponse.isNull(), "clicking the button must not return a bare null response body");
        assertEquals(1, clicks.get(), "the button's own handler must have fired exactly once");
    }

    /**
     * AC-1b (the decisive one): proves resolution happens BY DESCENT, not by the map. Resolves the button's
     * handler through {@link HandlerResolver} DIRECTLY - built from the Target extracted out of the rendered
     * JSON - and asserts invoking the resolved handler fires the button's {@code onClick}. This never calls
     * {@code POST /ui/event} / {@code EventHandlerServiceImpl.handleEvent}, so it passes with NO dependence on
     * the registry map.
     */
    @Test
    public void testHandlerResolvesByDescentDirectlyThroughHandlerResolver() throws Exception
    {
        AtomicInteger clicks = new AtomicInteger();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newButton()
                                                                                                   .withName("DescentOnly")
                                                                                                   .onClick(clicks::incrementAndGet)));

        List<Target> buttonTargets = this.extractOnClickTargets(this.renderUI());
        assertEquals(1, buttonTargets.size(), "expected exactly one rendered button with a SERVER onClick handler");
        Target buttonTarget = buttonTargets.get(0);

        Optional<DataEventHandler> resolved = this.handlerResolver.resolve(buttonTarget, Optional.empty());
        assertTrue(resolved.isPresent(), "HandlerResolver must resolve the button's handler purely by descent");

        assertEquals(0, clicks.get(), "resolving must not itself invoke the handler");
        resolved.get()
                .invoke(Data.newInstance(), Data.newInstance());
        assertEquals(1, clicks.get(), "invoking the resolved handler must fire the button's own onClick");
    }

    /**
     * AC-2a (plan-78 Slice 2, Cliff C1-A): the FORM case (plan-78 survey finding F4) - a {@code Form}'s
     * {@code ButtonFormElement} is NOT reachable via {@code getSubComponents}
     * ({@code FormRendererImpl.getSubComponents} returns {@code Stream.empty()}). Slice 1 left this
     * {@code @Disabled} because {@code FormRendererImpl}/{@code ButtonFormElementImpl} embedded their handler
     * DTO directly inside {@code render(...)} against an {@code EventHandlerRegistry} field baked in at
     * construction time, with no substitutable seam for {@link HandlerResolverImpl}'s traversal-scoped capture
     * to intercept.
     * <p>
     * Slice 2 closes the gap with the handler-emission channel {@code RenderingProcessor.handlers()}
     * ({@link org.omnaest.react4j.domain.rendering.components.HandlerEmitter}, Cliff C1-A):
     * {@code ButtonFormElementImpl.renderNode(...)} now obtains its {@code onClick} node DTO through the
     * emitter instead of constructing it directly, and {@link HandlerResolverImpl} falls back - when
     * {@code getSubComponents} and the {@code manageEventHandler} harvest both miss - to a
     * {@code render(...)}-and-harvest pass through a traversal-scoped CAPTURING {@code HandlerEmitter} that
     * never touches the real global handler map. Resolves by descent, exactly like the plain-{@code Button}
     * case above.
     * </p>
     */
    @Test
    public void testFormButtonHandlerResolvesByDescent() throws Exception
    {
        AtomicInteger clicks = new AtomicInteger();
        // Attached to a Document via withUIContext - required so FormRendererImpl.getEffectiveContext() (which
        // has no fallback, unlike the component-level FormImpl) does not NPE at render time; mirrors
        // FileUploadEndToEndTest's precedent fixture shape.
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newForm()
                                                                                                   .withUIContext((form, context) ->
                                                                                                   {
                                                                                                       form.attachTo(context.getFirstDocument());
                                                                                                       form.addButton(button -> button.withText("Submit")
                                                                                                                                      .onClick((data, buttonContext) ->
                                                                                                                                      {
                                                                                                                                          clicks.incrementAndGet();
                                                                                                                                          return data;
                                                                                                                                      }));
                                                                                                   })));

        List<Target> buttonTargets = this.extractOnClickTargets(this.renderUI());
        assertEquals(1, buttonTargets.size(), "expected exactly one rendered form button with a SERVER onClick handler");
        Target buttonTarget = buttonTargets.get(0);

        Optional<DataEventHandler> resolved = this.handlerResolver.resolve(buttonTarget, Optional.empty());
        assertTrue(resolved.isPresent(), "HandlerResolver must resolve the form button's handler purely by descent");

        assertEquals(0, clicks.get(), "resolving must not itself invoke the handler");
        resolved.get()
                .invoke(Data.newInstance(), Data.newInstance());
        assertEquals(1, clicks.get(), "invoking the resolved handler must fire the form button's own onClick");
    }

    /**
     * plan-78 Cliff C1-A (Group A conversion, AC-3 - decisive empirical proof for DECISION 1). Converts
     * {@code ToggleButtonImpl} to obtain its {@code onChange} node-DTO {@link Handler} through
     * {@code RenderingProcessor.handlers()} while {@code manageEventHandler} STAYS in place as the sole
     * registration path for {@code GET /ui} and the {@link HandlerResolver} harvest mode (plan-78 §"DECISION
     * 1"). The production {@link org.omnaest.react4j.service.internal.service.internal.HandlerEmitterImpl}
     * therefore double-registers the SAME {@link Target} that {@code manageEventHandler} already registers
     * within one {@code GET /ui} staging cycle. This test proves that double registration COLLAPSES to
     * exactly one invocation per click - not two - because
     * {@code EventHandlerServiceImpl.registerDataEventHandler} REPLACES the Target's handler list rather than
     * appending. A regression back to append-based registration (or a real double-invocation bug) would flip
     * this assertion from 1 to 2.
     */
    @Test
    public void testClickingToggleButtonFiresOnChangeExactlyOnceDespiteDoubleRegistration() throws Exception
    {
        AtomicInteger changes = new AtomicInteger();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newToggleButton()
                                                                                                   .withText("Toggle")
                                                                                                   .onChange(changes::incrementAndGet)));

        List<Target> toggleTargets = this.extractOnChangeTargets(this.renderUI());
        assertEquals(1, toggleTargets.size(), "expected exactly one rendered toggle button with a SERVER onChange handler");
        Target toggleTarget = toggleTargets.get(0);

        JsonNode clickResponse = this.clickButton(toggleTarget);
        assertFalse(clickResponse.isNull(), "clicking the toggle button must not return a bare null response body");
        assertEquals(1, changes.get(), "the toggle button's onChange must fire EXACTLY ONCE, not twice, despite "
                                       + "manageEventHandler AND the HandlerEmitter both registering the same Target");
    }

    /**
     * plan-78 Cliff C1-A (Group B conversion, AC-4). Mirrors {@code testFormButtonHandlerResolvesByDescent}
     * above (the existing AC-1c precedent) for {@code InputFormElementImpl.submitOnEnter} - proves the
     * converted Group B site is resolvable through {@link HandlerResolver}'s render-and-harvest path, exactly
     * like the {@code ButtonFormElementImpl} case.
     */
    @Test
    public void testFormInputSubmitOnEnterHandlerResolvesByDescent() throws Exception
    {
        AtomicInteger submits = new AtomicInteger();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newForm()
                                                                                                   .withUIContext((form, context) ->
                                                                                                   {
                                                                                                       form.attachTo(context.getFirstDocument());
                                                                                                       form.addInputField(input -> input.onEnter((data, buttonContext) ->
                                                                                                       {
                                                                                                           submits.incrementAndGet();
                                                                                                           return data;
                                                                                                       }));
                                                                                                   })));

        List<Target> submitOnEnterTargets = this.extractSubmitOnEnterTargets(this.renderUI());
        assertEquals(1, submitOnEnterTargets.size(), "expected exactly one rendered input with a SERVER submitOnEnter handler");
        Target inputTarget = submitOnEnterTargets.get(0);

        Optional<DataEventHandler> resolved = this.handlerResolver.resolve(inputTarget, Optional.empty());
        assertTrue(resolved.isPresent(), "HandlerResolver must resolve the form input's submitOnEnter handler purely by descent");

        assertEquals(0, submits.get(), "resolving must not itself invoke the handler");
        resolved.get()
                .invoke(Data.newInstance(), Data.newInstance());
        assertEquals(1, submits.get(), "invoking the resolved handler must fire the form input's own onEnter handler");
    }

    private String renderUI() throws Exception
    {
        return this.mockMvc.perform(get("/ui"))
                           .andExpect(status().isOk())
                           .andReturn()
                           .getResponse()
                           .getContentAsString();
    }

    private JsonNode clickButton(Target target) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext("test-context", Collections.emptyMap(), Collections.emptyMap()));
        String requestJson = OBJECT_MAPPER.writeValueAsString(eventBody);
        String responseJson = this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(requestJson))
                                          .andExpect(status().isOk())
                                          .andReturn()
                                          .getResponse()
                                          .getContentAsString();
        return OBJECT_MAPPER.readTree(responseJson);
    }

    private List<Target> extractOnClickTargets(String json) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        List<JsonNode> onClickTargetNodes = new ArrayList<>();
        collectOnClickTargets(root, onClickTargetNodes);
        List<Target> targets = new ArrayList<>();
        for (JsonNode targetNode : onClickTargetNodes)
        {
            targets.add(OBJECT_MAPPER.treeToValue(targetNode, Target.class));
        }
        return targets;
    }

    private static void collectOnClickTargets(JsonNode node, List<JsonNode> collector)
    {
        if (node == null)
        {
            return;
        }
        if (node.isObject())
        {
            if (node.has("onClick") && node.get("onClick")
                                           .isObject())
            {
                collector.add(node.get("onClick")
                                  .get("target"));
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                collectOnClickTargets(node.get(fieldNames.next()), collector);
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                collectOnClickTargets(child, collector);
            }
        }
    }

    private List<Target> extractOnChangeTargets(String json) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        List<JsonNode> onChangeTargetNodes = new ArrayList<>();
        collectOnChangeTargets(root, onChangeTargetNodes);
        List<Target> targets = new ArrayList<>();
        for (JsonNode targetNode : onChangeTargetNodes)
        {
            targets.add(OBJECT_MAPPER.treeToValue(targetNode, Target.class));
        }
        return targets;
    }

    private static void collectOnChangeTargets(JsonNode node, List<JsonNode> collector)
    {
        if (node == null)
        {
            return;
        }
        if (node.isObject())
        {
            if (node.has("onChange") && node.get("onChange")
                                            .isObject())
            {
                collector.add(node.get("onChange")
                                  .get("target"));
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                collectOnChangeTargets(node.get(fieldNames.next()), collector);
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                collectOnChangeTargets(child, collector);
            }
        }
    }

    private List<Target> extractSubmitOnEnterTargets(String json) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        List<JsonNode> submitOnEnterTargetNodes = new ArrayList<>();
        collectSubmitOnEnterTargets(root, submitOnEnterTargetNodes);
        List<Target> targets = new ArrayList<>();
        for (JsonNode targetNode : submitOnEnterTargetNodes)
        {
            targets.add(OBJECT_MAPPER.treeToValue(targetNode, Target.class));
        }
        return targets;
    }

    private static void collectSubmitOnEnterTargets(JsonNode node, List<JsonNode> collector)
    {
        if (node == null)
        {
            return;
        }
        if (node.isObject())
        {
            if (node.has("input") && node.get("input")
                                         .isObject()
                && node.get("input")
                       .has("submitOnEnter")
                && node.get("input")
                       .get("submitOnEnter")
                       .isObject())
            {
                collector.add(node.get("input")
                                  .get("submitOnEnter")
                                  .get("target"));
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                collectSubmitOnEnterTargets(node.get(fieldNames.next()), collector);
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                collectSubmitOnEnterTargets(child, collector);
            }
        }
    }

}
