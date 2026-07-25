package org.omnaest.react4j.service.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
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
 * plan-30 seam / integration test: proves the corrected positional sibling {@link org.omnaest.react4j.domain.Location} disambiguation
 * (see {@link org.omnaest.react4j.service.internal.component.ChildLocationSupport}) fixes the real demo failure - clicking a
 * {@code Button} inside one of several same-type {@code RerenderingContainer} siblings under a {@code Composite} must fire ONLY that
 * button's own handler and must return a non-null response, driving the click through the REAL Spring service graph
 * ({@code ReactUIController} -&gt; {@code EventHandlerServiceImpl} -&gt; {@code RerenderingServiceImpl} -&gt; {@code ReactUIServiceImpl}) via
 * {@code GET /ui} + {@code POST /ui/event}, exactly mirroring what a real browser click does. This is the exact seam plan-29's fix
 * regressed: unit tests passed while a real click returned a bare JSON {@code null}.
 *
 * @see org.omnaest.react4j.service.internal.component.ChildLocationSupport
 * @see org.omnaest.react4j.service.internal.component.CompositeImpl
 */
@SpringBootTest(classes = RerenderingSiblingButtonClickEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class RerenderingSiblingButtonClickEndToEndTest
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
    public void testClickingOneSiblingButtonFiresOnlyItsOwnHandlerAndReturnsNonNullResponse() throws Exception
    {
        AtomicInteger firstButtonClicks = new AtomicInteger();
        AtomicInteger secondButtonClicks = new AtomicInteger();
        this.registerCompositeWithTwoRerenderingButtons(firstButtonClicks, secondButtonClicks);

        List<Target> buttonTargets = this.extractButtonTargets(this.renderUI());
        assertEquals(2, buttonTargets.size(), "expected exactly two rendered buttons with SERVER onClick handlers");
        Target firstButtonTarget = buttonTargets.get(0);
        Target secondButtonTarget = buttonTargets.get(1);
        assertNotEquals(firstButtonTarget, secondButtonTarget,
                        "same-type sibling buttons must resolve to distinct Targets (positional disambiguation)");

        JsonNode firstClickResponse = this.clickButton(firstButtonTarget);
        assertFalse(firstClickResponse.isNull(),
                    "clicking the first sibling button must not return a bare null response body (plan-29 regression)");
        assertTrue(firstClickResponse.hasNonNull("targetNode"), "response targetNode must be non-null");
        assertEquals(1, firstButtonClicks.get(), "first button's own handler must have fired");
        assertEquals(0, secondButtonClicks.get(), "second (sibling) button's handler must NOT fire on the first button's click");

        JsonNode secondClickResponse = this.clickButton(secondButtonTarget);
        assertFalse(secondClickResponse.isNull(), "clicking the second sibling button must not return a bare null response body");
        assertTrue(secondClickResponse.hasNonNull("targetNode"), "response targetNode must be non-null");
        assertEquals(1, firstButtonClicks.get(), "first button's handler must not fire again on the second button's click");
        assertEquals(1, secondButtonClicks.get(), "second button's own handler must have fired");
    }

    private void registerCompositeWithTwoRerenderingButtons(AtomicInteger firstButtonClicks, AtomicInteger secondButtonClicks)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newComposite()
                                                                                                   .addNewComponent(f -> f.newRerenderingContainer()
                                                                                                                          .withContent(f2 -> f2.newButton()
                                                                                                                                               .withName("First")
                                                                                                                                               .onClick(firstButtonClicks::incrementAndGet)))
                                                                                                   .addNewComponent(f -> f.newRerenderingContainer()
                                                                                                                          .withContent(f2 -> f2.newButton()
                                                                                                                                               .withName("Second")
                                                                                                                                               .onClick(secondButtonClicks::incrementAndGet)))));
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

    private List<Target> extractButtonTargets(String json) throws Exception
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

}
