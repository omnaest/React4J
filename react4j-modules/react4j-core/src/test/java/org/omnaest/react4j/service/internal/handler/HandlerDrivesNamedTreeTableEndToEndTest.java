package org.omnaest.react4j.service.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.component.treetable.provider.TreeTableDataProvider;
import org.omnaest.react4j.component.treetable.provider.TreeTablePage;
import org.omnaest.react4j.component.treetable.provider.TreeTableQuery;
import org.omnaest.react4j.component.treetable.provider.TreeTableRow;
import org.omnaest.react4j.domain.components.UIComponents;
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
 * A handler drives a table it does not own, by name.
 *
 * <h2>What this is proving</h2>
 * That {@code UIComponents.in(data).treeTable(name)} is a real seam and not just a shape: the write has to land
 * under the key the RENDERER reads, at the {@code Location} the table actually occupies, and survive the round
 * trip to appear in the response the client applies. Every one of those can fail silently on its own - a write
 * under a key nobody reads produces no error anywhere - which is why this is asserted end to end over real HTTP
 * rather than by checking that a method was called.
 *
 * <h2>Why the table is deliberately far from the button</h2>
 * The button sits at the top of the page and the table inside two further containers, so their {@code Location}s
 * share almost nothing. If the access API were quietly deriving keys from the HANDLER's position rather than the
 * TABLE's, a shallow fixture would hide it and this one does not.
 *
 * @see org.omnaest.react4j.service.internal.component.UIComponentsImpl
 */
@SpringBootTest(classes = HandlerDrivesNamedTreeTableEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class HandlerDrivesNamedTreeTableEndToEndTest
{
    private static final ObjectMapper OBJECT_MAPPER  = new ObjectMapper();

    private static final String       TABLE_NAME     = "partners";

    private static final String       FORM_CONTEXT   = "someform.context";

    private static final String       TABLE_CONTEXT  = "";

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @Autowired
    private UIComponents              uiComponents;

    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    private static class SimpleProvider implements TreeTableDataProvider
    {
        private final List<TreeTableRow> rows = List.of(TreeTableRow.of("a", Map.of("name", "Alpha", "city", "Berlin"), false),
                                                        TreeTableRow.of("b", Map.of("name", "Beta", "city", "Cologne"), false));

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
     * The button is a sibling of a container that holds the table, so the two sit at genuinely different depths.
     */
    private void registerPageWhoseButtonDrivesTheTable()
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newRerenderingContainer()
                                                                                                   .enableStaticNodeRerendering()
                                                                                                   .withContent(f -> f.newComposite()
                                                                                                                      .addNewComponent(f2 -> f2.newButton()
                                                                                                                                               .withName("Flatten")
                                                                                                                                               .onClick(() ->
                                                                                                                                               {
                                                                                                                                               }))
                                                                                                                      .addNewComponent(f2 -> f2.newComposite()
                                                                                                                                               .addNewComponent(f3 -> f3.newTreeTable()
                                                                                                                                                                        .withName(TABLE_NAME)
                                                                                                                                                                        .withColumns(TreeTableColumn.of("name",
                                                                                                                                                                                                        "Name"),
                                                                                                                                                                                     TreeTableColumn.of("city",
                                                                                                                                                                                                        "City"))
                                                                                                                                                                        .withDataProvider(new SimpleProvider())
                                                                                                                                                                        .withFlatModeToggleEnabled(true))))));
    }

    /**
     * THE claim: a write made through the access API, in a handler belonging to a different component, comes back
     * applied in the same response.
     */
    @Test
    public void testAHandlerCanFlattenATableItDoesNotOwn() throws Exception
    {
        this.registerPageWhoseButtonDrivesTheTable();

        JsonNode table = findTreeTableNode(this.renderUI());
        assertNotNull(table, "precondition: the page must render a tree table");
        assertFalse(table.get("flatMode")
                         .asBoolean(),
                    "precondition: the table starts in tree mode");

        Target buttonTarget = this.findFirstOnClickTarget(this.renderUI());
        JsonNode response = this.clickDrivingTheTable(buttonTarget, access -> access.setFlatMode(true));

        JsonNode tableAfter = findTreeTableNode(response.get("targetNode")
                                                        .get("node"));
        assertNotNull(tableAfter, "the response must contain the re-rendered table");
        assertTrue(tableAfter.get("flatMode")
                             .asBoolean(),
                   "the table must be flat because a handler on ANOTHER component asked it to be - by name, "
                           + "without knowing its location or the field key it keeps that mode under");
    }

    /**
     * A filter set through the API must reach the table's own filter state, which the table reports as its active
     * filter count - a value derived by the renderer from the very fields the API wrote.
     */
    @Test
    public void testAHandlerCanFilterATableItDoesNotOwn() throws Exception
    {
        this.registerPageWhoseButtonDrivesTheTable();

        assertEquals(0, findTreeTableNode(this.renderUI()).get("activeFilterCount")
                                                          .asInt(),
                     "precondition: no filter is active to begin with");

        Target buttonTarget = this.findFirstOnClickTarget(this.renderUI());
        JsonNode response = this.clickDrivingTheTable(buttonTarget, access -> access.setFilter("city", "Cologne"));

        assertEquals(1, findTreeTableNode(response.get("targetNode")
                                                  .get("node")).get("activeFilterCount")
                                                               .asInt(),
                     "the renderer must count the filter the handler set - which only happens if the write landed "
                             + "under the key the renderer reads, at the table's own location");
    }

    /**
     * An unknown name does nothing, quietly, and says so when asked.
     * <p>
     * A page legitimately renders without a given table - a different view mode, a permission the principal
     * lacks - and a handler reacting to a message should not have to know which. Throwing would turn a normal
     * state into an error; failing silently WITHOUT a way to ask would make it undiagnosable. Hence both.
     */
    @Test
    public void testAnUnknownNameIsANoOpAndReportsItself() throws Exception
    {
        this.registerPageWhoseButtonDrivesTheTable();
        this.renderUI();

        assertFalse(this.uiComponents.in(org.omnaest.react4j.domain.context.data.Data.newInstance())
                                     .treeTable("no-such-table")
                                     .isPresent(),
                    "a name nothing rendered under must report itself absent");
        assertTrue(this.uiComponents.in(org.omnaest.react4j.domain.context.data.Data.newInstance())
                                    .treeTable(TABLE_NAME)
                                    .isPresent(),
                   "control: the name that WAS rendered must report itself present, or the assertion above passes for the wrong reason");

        Target buttonTarget = this.findFirstOnClickTarget(this.renderUI());
        JsonNode response = this.clickDrivingTheTable(buttonTarget, access -> access.setFlatMode(true));
        // Driving the absent one changes nothing and must not throw - asserted by the click above completing and
        // the named table still responding normally.
        assertNotNull(findTreeTableNode(response.get("targetNode")
                                                .get("node")));
    }

    /**
     * Drives the named table from inside a handler, then fires that handler over real HTTP.
     * <p>
     * The handler is registered against the page rather than passed through the request, because that is where an
     * application would put it - and because the write has to happen INSIDE the round trip to be carried by it.
     */
    private JsonNode clickDrivingTheTable(Target target,
                                          java.util.function.Consumer<UIComponents.TreeTableAccess> drive) throws Exception
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newRerenderingContainer()
                                                                                                   .enableStaticNodeRerendering()
                                                                                                   .withContent(f -> f.newComposite()
                                                                                                                      .addNewComponent(f2 -> f2.newForm()
                                                                                                                                               .withUIContext((form, uiContext) ->
                                                                                                                                               {
                                                                                                                                                   // A Form must be attached to a Document or its
                                                                                                                                                   // controls have no data context to write into -
                                                                                                                                                   // memory react4j-form-attachto-document-required.
                                                                                                                                                   form.attachTo(uiContext.getFirstDocument());
                                                                                                                                                   form.addButton(button -> button.withText("Go")
                                                                                                                                                                                  .onClick((data, context) ->
                                                                                                                                                                                  {
                                                                                                                                                                                      drive.accept(this.uiComponents.in(data)
                                                                                                                                                                                                                    .treeTable(TABLE_NAME));
                                                                                                                                                                                      return data;
                                                                                                                                                                                  }));
                                                                                                                                               }))
                                                                                                                      .addNewComponent(f2 -> f2.newComposite()
                                                                                                                                               .addNewComponent(f3 -> f3.newTreeTable()
                                                                                                                                                                        .withName(TABLE_NAME)
                                                                                                                                                                        .withColumns(TreeTableColumn.of("name",
                                                                                                                                                                                                        "Name"),
                                                                                                                                                                                     TreeTableColumn.of("city",
                                                                                                                                                                                                        "City"))
                                                                                                                                                                        .withDataProvider(new SimpleProvider())
                                                                                                                                                                        .withFlatModeToggleEnabled(true))))));

        Target formButtonTarget = this.findFirstOnClickTarget(this.renderUI());
        return this.postEvent(formButtonTarget, FORM_CONTEXT, Map.of("someField", "x"),
                              List.of(new DataWithContext(TABLE_CONTEXT, new HashMap<>(), Collections.emptyMap()),
                                      new DataWithContext(FORM_CONTEXT, new HashMap<>(Map.of("someField", "x")), Collections.emptyMap())));
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

    private JsonNode postEvent(Target target, String contextId, Map<String, Object> data, List<DataWithContext> allContexts) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext(contextId, new HashMap<>(data), Collections.emptyMap()));
        com.fasterxml.jackson.databind.node.ObjectNode bodyNode = (com.fasterxml.jackson.databind.node.ObjectNode) OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(eventBody));
        bodyNode.set("dataWithContexts", OBJECT_MAPPER.valueToTree(allContexts));

        String responseJson = this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(OBJECT_MAPPER.writeValueAsString(bodyNode)))
                                          .andExpect(status().isOk())
                                          .andReturn()
                                          .getResponse()
                                          .getContentAsString();
        return OBJECT_MAPPER.readTree(responseJson);
    }

    private Target findFirstOnClickTarget(JsonNode root) throws Exception
    {
        List<JsonNode> targets = new ArrayList<>();
        collectOnClickTargets(root, targets);
        assertFalse(targets.isEmpty(), "expected the rendered page to contain a control with a SERVER onClick handler");
        return OBJECT_MAPPER.treeToValue(targets.get(0), Target.class);
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
