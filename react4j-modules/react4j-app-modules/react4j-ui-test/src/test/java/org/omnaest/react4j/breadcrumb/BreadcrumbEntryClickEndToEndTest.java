package org.omnaest.react4j.breadcrumb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.security.WebSecurityConfiguration;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * plan-262 AC-1 (S1): the walking-skeleton proof that {@code Breadcrumb.BreadcrumbEntry#onClick(EventHandler)}
 * drives the REAL UI, not merely the node tree - a page carrying a {@code Breadcrumb} with TWO non-active
 * entries, each with a DISTINCT {@code onClick}; {@code GET /ui}; the server-issued {@code Target} extracted
 * from each entry node by a recursive {@link JsonNode} walk; {@code POST /ui/event} against ONE of them; and
 * the assertion that the response is non-null and exactly the ONE corresponding handler ran.
 * <p>
 * Mirrors {@code DropTargetEndToEndTest} (react4j-core, plan-235 S1) and
 * {@code ArmBFreshFormPerInvocationControlTest} (this module, plan-235 S4) - the module's existing
 * {@code @SpringBootTest(WebEnvironment.MOCK)} + {@code @AutoConfigureMockMvc} e2e precedent - rather than
 * hand-assembling a harness with manually-set {@code @Autowired} fields, per plan-262's own instruction.
 *
 * @see org.omnaest.react4j.service.internal.component.BreadcrumbEntryImpl
 */
@SpringBootTest(classes = BreadcrumbEntryClickEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class BreadcrumbEntryClickEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @SpringBootApplication
    @EnableReactUI
    @Import(WebSecurityConfiguration.class)
    public static class TestApplication
    {
    }

    @Test
    public void testTwoNonActiveEntriesWithDistinctHandlersFireOnlyTheClickedOne() throws Exception
    {
        AtomicInteger firstClicked = new AtomicInteger(0);
        AtomicInteger secondClicked = new AtomicInteger(0);

        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newBreadcrumb()
                                                                                                   .addEntry(entry -> entry.withText("First")
                                                                                                                           .onClick(firstClicked::incrementAndGet))
                                                                                                   .addEntry(entry -> entry.withText("Second")
                                                                                                                           .onClick(secondClicked::incrementAndGet))));

        List<JsonNode> entryNodes = this.findAllBreadcrumbEntryNodes(this.renderUI());
        assertEquals(2, entryNodes.size(), "precondition: both breadcrumb entries must be present in the rendered page");

        Target firstTarget = this.extractOnClickTarget(entryNodes.get(0));
        Target secondTarget = this.extractOnClickTarget(entryNodes.get(1));
        assertNotEquals(firstTarget, secondTarget, "two sibling BreadcrumbEntry components must not collide onto the same Target");

        JsonNode response = this.postEvent(secondTarget);

        assertNotNull(response, "POST /ui/event must return a non-null response body");
        assertEquals(0, firstClicked.get(), "a click routed to the SECOND entry must not fire the FIRST entry's handler");
        assertEquals(1, secondClicked.get(), "a click routed to the SECOND entry must fire exactly that entry's handler");
    }

    private JsonNode postEvent(Target target) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext("", Collections.emptyMap(), Collections.emptyMap()));
        String requestJson = OBJECT_MAPPER.writeValueAsString(eventBody);
        String responseJson = this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(requestJson))
                                          .andExpect(status().isOk())
                                          .andReturn()
                                          .getResponse()
                                          .getContentAsString();
        assertFalse(responseJson == null || responseJson.isEmpty(), "expected a non-empty POST /ui/event response body");
        return OBJECT_MAPPER.readTree(responseJson);
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

    private Target extractOnClickTarget(JsonNode entryNode) throws Exception
    {
        JsonNode onClick = entryNode.get("onClick");
        assertNotNull(onClick, () -> "expected a rendered onClick handler on: " + entryNode);
        return OBJECT_MAPPER.treeToValue(onClick.get("target"), Target.class);
    }

    private List<JsonNode> findAllBreadcrumbEntryNodes(JsonNode root)
    {
        List<JsonNode> collected = new ArrayList<>();
        this.collectBreadcrumbEntryNodes(root, collected);
        return collected;
    }

    private void collectBreadcrumbEntryNodes(JsonNode node, List<JsonNode> collected)
    {
        if (node == null || node.isNull())
        {
            return;
        }
        if (node.isObject())
        {
            JsonNode type = node.get("type");
            if (type != null && "BREADCRUMB_ENTRY".equals(type.asText()))
            {
                collected.add(node);
            }
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();)
            {
                this.collectBreadcrumbEntryNodes(it.next()
                                                   .getValue(),
                                                 collected);
            }
        }
        else if (node.isArray())
        {
            node.forEach(child -> this.collectBreadcrumbEntryNodes(child, collected));
        }
    }
}
