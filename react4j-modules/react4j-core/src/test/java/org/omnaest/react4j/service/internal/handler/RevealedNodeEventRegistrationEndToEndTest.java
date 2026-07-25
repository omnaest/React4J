package org.omnaest.react4j.service.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.service.ReactUIService;
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
 * plan-77 AC-F1 - the decisive FRAMEWORK-LEVEL seam test: a {@code withDataDrivenContent} {@code RerenderingContainer}
 * whose content is a {@code Composite} that always renders an always-present TRIGGER {@code Button}, and ADDS a
 * SECOND, NEW handler-bearing REVEALED {@code Button} only once a submitted {@code Data} field is set - proving that
 * a control revealed by one event's response is live-clickable in the very NEXT event of the SAME session, i.e. that
 * its handler was actually REGISTERED during the round trip, not only rendered. (The trigger and revealed controls
 * are DISTINCT {@code Composite} children at different positional indices, so their {@code Target}s never collide -
 * mirrors {@code react4j-routing-key-must-be-stable-and-unique-use-positional-id}.)
 * <p>
 * RED on current code: {@code RerenderingContainerImpl.getSubComponents} always applies {@code Data.empty()}, and
 * {@code EventHandlerServiceImpl.handleEvent} never re-runs the registration walk at all, so the revealed button's
 * handler is never registered - clicking it does not increment its counter (its handler never runs), even though
 * {@code render()} already shows it in the DOM (mirrors the plan-76 gap: an expand test with only non-interactive
 * revealed leaves could not catch this; here the revealed node IS interactive, so "handler never fired" IS
 * observable).
 * <p>
 * GREEN once {@code ReactUIServiceImpl}'s registration walk re-runs under the submitted {@code Data} at the same
 * {@code RerenderedNodeProvider} application {@code render()} already uses (plan-77 Cliff F1/F2).
 *
 * @see org.omnaest.react4j.service.internal.component.RerenderingContainerImpl
 * @see org.omnaest.react4j.domain.rendering.UIComponentRenderer
 */
@SpringBootTest(classes = RevealedNodeEventRegistrationEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class RevealedNodeEventRegistrationEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    @Test
    public void testControlRevealedByADataDrivenRerenderMustBeLiveClickableInTheSameSession() throws Exception
    {
        AtomicInteger revealedClicks = new AtomicInteger();
        AtomicInteger triggerClicks = new AtomicInteger();
        this.registerDataDrivenContainer(revealedClicks, triggerClicks);

        // 1. GET /ui: only the always-present TRIGGER control exists - the REVEALED control does not exist yet
        // (the registration walk ran with empty Data).
        List<JsonNode> onClickTargetsAtInitialRender = this.collectOnClickTargets(this.renderUI());
        assertEquals(1, onClickTargetsAtInitialRender.size(), "expected exactly the trigger control's onClick, nothing revealed yet");
        Target triggerTarget = this.toTarget(onClickTargetsAtInitialRender.get(0));

        // 2. Click the (already-registered-at-GET/ui) trigger, submitting the Data field the content depends on -
        // this ADDS a NEW second control in THIS event's response (proves render() reacts to submitted Data; the
        // registration-walk bug this test targets is exposed only by step 3 below).
        JsonNode revealResponse = this.postEvent(triggerTarget, Map.of("reveal", true));
        assertFalse(revealResponse.isNull(), "clicking the trigger must not return a bare null response");
        assertEquals(1, triggerClicks.get(), "the trigger's own handler must have fired");
        List<JsonNode> onClickTargetsAfterReveal = this.collectOnClickTargets(revealResponse.get("targetNode")
                                                                                            .get("node"));
        assertEquals(2, onClickTargetsAfterReveal.size(), "the trigger AND the newly revealed control must both be present in the response node");
        Target revealedButtonTarget = onClickTargetsAfterReveal.stream()
                                                               .map(this::toTarget)
                                                               .filter(target -> !target.equals(triggerTarget))
                                                               .findFirst()
                                                               .orElseThrow();

        Map<String, Object> echoedData = OBJECT_MAPPER.convertValue(revealResponse.get("dataWithContext")
                                                                                  .get("data"),
                                                                    Map.class);

        // 3. THE decisive click: the revealed control's own server-issued Target, in the SAME session, echoing the
        // Data that revealed it forward (a real client resubmits exactly this on the next event).
        JsonNode clickResponse = this.postEvent(revealedButtonTarget, echoedData);
        assertFalse(clickResponse.isNull(), "clicking the revealed control must not return a bare null response (plan-29 regression class)");
        assertEquals(1, revealedClicks.get(),
                     "the revealed control's own handler must have fired - this is what proves it was actually REGISTERED, not merely rendered");
    }

    private void registerDataDrivenContainer(AtomicInteger revealedClicks, AtomicInteger triggerClicks)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newRerenderingContainer()
                                                                                                   .withDataDrivenContent(data ->
                                                                                                   {
                                                                                                       boolean revealed = data.getFieldValue("reveal")
                                                                                                                              .map(Value::asBoolean)
                                                                                                                              .orElse(false);
                                                                                                       Composite composite = factory.newComposite()
                                                                                                                                    .addNewComponent(f -> f.newButton()
                                                                                                                                                           .withName("Trigger")
                                                                                                                                                           .onClick(triggerClicks::incrementAndGet));
                                                                                                       if (revealed)
                                                                                                       {
                                                                                                           composite.addNewComponent(f -> f.newButton()
                                                                                                                                           .withName("Revealed")
                                                                                                                                           .onClick(revealedClicks::incrementAndGet));
                                                                                                       }
                                                                                                       return composite;
                                                                                                   })
                                                                                                   .enableStaticNodeRerendering()));
    }

    private JsonNode renderUI() throws Exception
    {
        String json = this.mockMvc.perform(get("/ui"))
                                  .andExpect(status().isOk())
                                  .andReturn()
                                  .getResponse()
                                  .getContentAsString();
        return OBJECT_MAPPER.readTree(json);
    }

    private JsonNode postEvent(Target target, Map<String, Object> data) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext("test-context", new HashMap<>(data), Collections.emptyMap()));
        String requestJson = OBJECT_MAPPER.writeValueAsString(eventBody);
        String responseJson = this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(requestJson))
                                          .andExpect(status().isOk())
                                          .andReturn()
                                          .getResponse()
                                          .getContentAsString();
        return OBJECT_MAPPER.readTree(responseJson);
    }

    private Target toTarget(JsonNode targetNode)
    {
        try
        {
            return OBJECT_MAPPER.treeToValue(targetNode, Target.class);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    private List<JsonNode> collectOnClickTargets(JsonNode node)
    {
        List<JsonNode> onClickTargetNodes = new ArrayList<>();
        collectOnClickTargets(node, onClickTargetNodes);
        return onClickTargetNodes;
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
}
