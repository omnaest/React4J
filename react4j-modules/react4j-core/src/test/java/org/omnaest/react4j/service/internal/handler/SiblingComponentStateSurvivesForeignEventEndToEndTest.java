package org.omnaest.react4j.service.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTablePage;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.component.treetable.provider.TreeTableRow;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A component must keep its own state when the event comes from a DIFFERENT component on the same page.
 *
 * <h2>The defect this exists for</h2>
 * Every component reads its state out of the submitted {@link org.omnaest.react4j.domain.context.data.Data} while
 * rendering, and each keeps it under its own context id - a {@code TreeTable}'s mode, filters, sort and window
 * live in the root ({@code ""}) context, a form's fields under the form's own. A {@code POST /ui/event} used to
 * carry only the ORIGINATING context, so every other component on the page was re-rendered from DEFAULTS. Not
 * stale - reset.
 * <p>
 * Measured live in an application that pairs a chat box with a table: submitting a chat message posted
 * {@code {"contextId":"cardimpl.formimpl","data":{"nodeStudioChatInput":"city berlin"}}} and the response carried
 * the table with {@code flatMode=false} and {@code activeFilterCount=0} - discarding a flat view the user had
 * switched on and a filter that had just been applied. Because any table-driven round trip DOES carry those
 * fields, touching the table made it look correct again, so the symptom read as "needs a refresh" rather than
 * "state lost".
 *
 * <h2>Why this shape</h2>
 * The two components are siblings under ONE {@code RerenderingContainer}, which is what makes a single round trip
 * re-render both (see memory {@code react4j-atomic-coupdate-siblings-under-one-rerenderingcontainer}) - and
 * therefore what makes the reset observable at all. That is the same wiring the application above uses.
 * <p>
 * {@link #testForeignEventWithoutTheOtherContextStillResetsIt()} is the NEGATIVE CONTROL. It pins the old
 * behaviour deliberately: with only the originating context in the payload the table still resets, which is what
 * makes the positive test above it evidence rather than coincidence. If a future change makes the two tests agree,
 * one of them is lying.
 *
 * @see org.omnaest.react4j.service.internal.handler.internal.EventHandlerServiceImpl
 */
@SpringBootTest(classes = SiblingComponentStateSurvivesForeignEventEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class SiblingComponentStateSurvivesForeignEventEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER      = new ObjectMapper();

    /** The context a TreeTable's own controls dispatch under - the root context, not a form's. */
    private static final String       TABLE_CONTEXT_ID   = "";

    /** Stands in for the chat form's context: a different id, carrying none of the table's fields. */
    private static final String       FOREIGN_CONTEXT_ID = "someform.context";

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    private static class SimpleProvider implements TreeTableDataProvider
    {
        private final List<TreeTableRow> rows = List.of(TreeTableRow.of("a", Map.of("name", "Alpha", "age", 20), false),
                                                        TreeTableRow.of("b", Map.of("name", "Beta", "age", 30), false));

        @Override
        public TreeTablePage fetch(TreeTableQuery query)
        {
            List<TreeTableRow> windowed = this.rows.stream()
                                                   .skip(query.getOffset())
                                                   .limit(query.getLimit())
                                                   .collect(Collectors.toList());
            return TreeTablePage.of(windowed, OptionalLong.empty());
        }
    }

    /**
     * A button and a table as SIBLINGS under one RerenderingContainer - the wiring under which one round trip
     * re-renders both, and therefore the only wiring under which the reset is visible.
     */
    private AtomicInteger registerButtonBesideTreeTable()
    {
        AtomicInteger clicks = new AtomicInteger();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newRerenderingContainer()
                                                                                                   .enableStaticNodeRerendering()
                                                                                                   .withContent(f -> f.newComposite()
                                                                                                                      .addNewComponent(f2 -> f2.newButton()
                                                                                                                                               .withName("Send")
                                                                                                                                               .onClick(clicks::incrementAndGet))
                                                                                                                      .addNewComponent(f2 -> f2.newTreeTable()
                                                                                                                                               .withColumns(TreeTableColumn.of("name", "Name"),
                                                                                                                                                            TreeTableColumn.of("age", "Age"))
                                                                                                                                               .withDataProvider(new SimpleProvider())
                                                                                                                                               .withFlatModeToggleEnabled(true)))));
        return clicks;
    }

    /**
     * THE claim: a click on the button must leave the table's own mode alone, because the request now carries the
     * table's context alongside the button's.
     */
    @Test
    public void testTreeTableKeepsItsFlatModeWhenASiblingButtonIsClicked() throws Exception
    {
        AtomicInteger clicks = this.registerButtonBesideTreeTable();

        JsonNode treeTableNode = findTreeTableNode(this.renderUI());
        assertFalse(treeTableNode.get("flatMode")
                                 .asBoolean(),
                    "precondition: the table starts in tree mode");
        Target flatToggleTarget = this.toTarget(treeTableNode.get("flatToggleTarget"));

        // The user switches the table to flat. This is a TABLE-driven round trip, so it carries the table's own
        // context and works today - it is the setup, not the claim.
        JsonNode toggleResponse = this.clickTarget(flatToggleTarget, TABLE_CONTEXT_ID, Collections.emptyMap(), Collections.emptyList());
        assertTrue(this.renderedTableIsFlat(toggleResponse), "precondition: the toggle must switch the table to flat");
        Map<String, Object> tableContextData = this.echoedData(toggleResponse);
        assertFalse(tableContextData.isEmpty(), "precondition: the toggle must have written the table's mode into the submitted data");

        // Now the FOREIGN event - a different component, a different context, carrying none of the table's fields
        // in its own data. What the request DOES now carry is the table's context beside it.
        Target buttonTarget = this.findFirstOnClickTarget(this.renderUI());
        JsonNode foreignResponse = this.clickTarget(buttonTarget, FOREIGN_CONTEXT_ID, Map.of("someFormField", "hello"),
                                                    List.of(new DataWithContext(TABLE_CONTEXT_ID, tableContextData, Collections.emptyMap()),
                                                            new DataWithContext(FOREIGN_CONTEXT_ID, new HashMap<>(Map.of("someFormField", "hello")),
                                                                                Collections.emptyMap())));

        System.out.println("DEBUG tableContextData = " + tableContextData);
        System.out.println("DEBUG foreignResponse dataWithContext = " + foreignResponse.get("dataWithContext"));
        assertEquals(1, clicks.get(), "precondition: the button's own handler must have fired, or this proves nothing about its round trip");
        assertTrue(this.renderedTableIsFlat(foreignResponse),
                   "the table must still be FLAT after a click on a sibling component. If it is not, the round trip "
                           + "re-rendered it from defaults because its context was not in the request - the state was not stale, it was discarded.");
    }

    /**
     * NEGATIVE CONTROL - deliberately pins the OLD behaviour so the test above is evidence, not coincidence.
     * <p>
     * Same click, same everything, except the request carries only the originating context. The table resets. This
     * is what every event looked like before, and it is why a fully green suite sat on top of a broken page: no
     * test drove an event from one component and then looked at another.
     */
    @Test
    public void testForeignEventWithoutTheOtherContextStillResetsIt() throws Exception
    {
        this.registerButtonBesideTreeTable();

        JsonNode treeTableNode = findTreeTableNode(this.renderUI());
        Target flatToggleTarget = this.toTarget(treeTableNode.get("flatToggleTarget"));
        JsonNode toggleResponse = this.clickTarget(flatToggleTarget, TABLE_CONTEXT_ID, Collections.emptyMap(), Collections.emptyList());
        assertTrue(this.renderedTableIsFlat(toggleResponse), "precondition: the toggle must switch the table to flat");

        Target buttonTarget = this.findFirstOnClickTarget(this.renderUI());
        // The old payload shape: the originating context and nothing else.
        JsonNode foreignResponse = this.clickTarget(buttonTarget, FOREIGN_CONTEXT_ID, Map.of("someFormField", "hello"), Collections.emptyList());

        assertFalse(this.renderedTableIsFlat(foreignResponse),
                    "with only the originating context in the request the table has nothing to read its mode from and falls back to the "
                            + "default - the behaviour the sibling test above exists to rule out");
    }

    private boolean renderedTableIsFlat(JsonNode eventResponse)
    {
        JsonNode table = findTreeTableNode(eventResponse.get("targetNode")
                                                        .get("node"));
        assertTrue(table != null, "the event response must contain the re-rendered TreeTable, or there is nothing to assert about");
        return table.get("flatMode")
                    .asBoolean();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> echoedData(JsonNode response) throws Exception
    {
        return OBJECT_MAPPER.convertValue(response.get("dataWithContext")
                                                  .get("data"),
                                          Map.class);
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

    private JsonNode clickTarget(Target target, String contextId, Map<String, Object> data, List<DataWithContext> allContexts) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext(contextId, new HashMap<>(data), Collections.emptyMap()));
        String requestJson = OBJECT_MAPPER.writeValueAsString(eventBody);
        if (!allContexts.isEmpty())
        {
            // Written onto the serialized body rather than through a setter: setDataWithContexts is protected, and
            // widening it for a test would expand the wire type's public surface for no production caller.
            ObjectNode bodyNode = (ObjectNode) OBJECT_MAPPER.readTree(requestJson);
            bodyNode.set("dataWithContexts", OBJECT_MAPPER.valueToTree(allContexts));
            requestJson = OBJECT_MAPPER.writeValueAsString(bodyNode);
        }
        String responseJson = this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(requestJson))
                                          .andExpect(status().isOk())
                                          .andReturn()
                                          .getResponse()
                                          .getContentAsString();
        return OBJECT_MAPPER.readTree(responseJson);
    }

    private Target toTarget(JsonNode targetNode) throws Exception
    {
        return OBJECT_MAPPER.treeToValue(targetNode, Target.class);
    }

    private Target findFirstOnClickTarget(JsonNode root) throws Exception
    {
        List<JsonNode> targets = new ArrayList<>();
        collectOnClickTargets(root, targets);
        assertFalse(targets.isEmpty(), "expected the rendered page to contain a button with a SERVER onClick handler");
        return this.toTarget(targets.get(0));
    }

    private static void collectOnClickTargets(JsonNode node, List<JsonNode> collected)
    {
        if (node == null || node.isNull())
        {
            return;
        }
        if (node.isObject())
        {
            JsonNode onClick = node.get("onClick");
            if (onClick != null && onClick.hasNonNull("target") && !onClick.get("target")
                                                                           .isEmpty())
            {
                collected.add(onClick.get("target"));
            }
            node.fields()
                .forEachRemaining(entry -> collectOnClickTargets(entry.getValue(), collected));
        }
        else if (node.isArray())
        {
            node.forEach(child -> collectOnClickTargets(child, collected));
        }
    }

    private static JsonNode findTreeTableNode(JsonNode node)
    {
        if (node == null || node.isNull())
        {
            return null;
        }
        if (node.isObject())
        {
            JsonNode type = node.get("type");
            if (type != null && "TREETABLE".equals(type.asText()))
            {
                return node;
            }
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();)
            {
                JsonNode found = findTreeTableNode(it.next()
                                                     .getValue());
                if (found != null)
                {
                    return found;
                }
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                JsonNode found = findTreeTableNode(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }
}
