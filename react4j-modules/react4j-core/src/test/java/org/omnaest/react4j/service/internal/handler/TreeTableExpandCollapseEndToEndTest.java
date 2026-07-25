package org.omnaest.react4j.service.internal.handler;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

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

/**
 * plan-76 Slice 5 - the decisive expand/collapse seam test (AC3): proves, through a REAL {@code POST /ui/event} round
 * trip, that expanding a row (a) fires ONLY that row's handler (distinct {@link Target} from a sibling row), (b)
 * triggers a child fetch scoped to that row's {@code nodeId} as {@code parentNodeId}, and (c) inserts the fetched
 * children flattened at {@code depth + 1} with their own {@code childLoadMore}. Also proves collapse (a second click
 * on the SAME already-registered target removes the children again) and that a full {@code GET /ui} returns the
 * correct (collapsed-by-default) tree.
 * <p>
 * Mirrors {@link TreeTableLoadMoreEndToEndTest}'s structure and the same mechanism (a) lag-immune path (Cliff C1a):
 * the expand toggle handler mutates a field on the SUBMITTED {@link org.omnaest.react4j.domain.context.data.Data},
 * read back by the SAME event's post-handler re-render.
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 */
@SpringBootTest(classes = TreeTableExpandCollapseEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableExpandCollapseEndToEndTest
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

    /**
     * A provider backed by a small two-level tree: root rows "a" (expandable, children a1/a2) and "b" (expandable,
     * child b1) - honors {@code query.getParentNodeId()}/{@code getLimit()} and RECORDS every query it receives so a
     * test can assert the child fetch was scoped to exactly the clicked row's {@code nodeId} (AC3).
     */
    private static class TrackingTreeProvider implements TreeTableDataProvider
    {
        final List<TreeTableQuery>                    recordedQueries    = new ArrayList<>();
        private final Map<String, List<TreeTableRow>> childrenByParentId = new LinkedHashMap<>();

        TrackingTreeProvider()
        {
            this.childrenByParentId.put(null, List.of(TreeTableRow.of("a", Map.of("name", "A"), true), TreeTableRow.of("b", Map.of("name", "B"), true)));
            this.childrenByParentId.put("a",
                                        List.of(TreeTableRow.of("a1", Map.of("name", "A1"), false), TreeTableRow.of("a2", Map.of("name", "A2"), false)));
            this.childrenByParentId.put("b", List.of(TreeTableRow.of("b1", Map.of("name", "B1"), false)));
        }

        @Override
        public TreeTablePage fetch(TreeTableQuery query)
        {
            this.recordedQueries.add(query);
            List<TreeTableRow> siblingGroup = this.childrenByParentId.getOrDefault(query.getParentNodeId()
                                                                                        .orElse(null),
                                                                                   Collections.emptyList());
            List<TreeTableRow> window = siblingGroup.subList(0, Math.min(siblingGroup.size(), query.getLimit()));
            return TreeTablePage.of(new ArrayList<>(window), OptionalLong.empty());
        }
    }

    private TrackingTreeProvider registerTreeTable()
    {
        TrackingTreeProvider provider = new TrackingTreeProvider();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name"))
                                                                                                   .withDataProvider(provider)
                                                                                                   .withWindowSize(500)));
        return provider;
    }

    @Test
    public void testExpandingRowFiresOnlyThatRowsHandlerAndInsertsScopedChildrenAtDepthPlusOne() throws Exception
    {
        TrackingTreeProvider provider = this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertNotNull(treeTableNode, "expected to find a TREETABLE node in the rendered UI JSON");
        assertEquals(2, treeTableNode.get("rows")
                                     .size(),
                     "baseline render must show only the two collapsed root rows");

        Target targetA = this.rowTarget(treeTableNode, "a");
        Target targetB = this.rowTarget(treeTableNode, "b");
        assertNotEquals(targetA, targetB, "row a and row b must resolve to distinct positional Targets (trap #4)");

        provider.recordedQueries.clear();

        // THE decisive click: expand row "b" only.
        JsonNode clickResponse = this.clickTarget(targetB, Collections.emptyMap());
        assertFalse(clickResponse.isNull(), "expanding a row must not return a bare null response (plan-29 regression class)");
        assertTrue(clickResponse.hasNonNull("targetNode"), "response targetNode must be non-null");

        JsonNode widenedTreeTableNode = this.findTreeTableNode(clickResponse.get("targetNode")
                                                                            .get("node"));
        assertNotNull(widenedTreeTableNode);
        List<JsonNode> rows = new ArrayList<>();
        widenedTreeTableNode.get("rows")
                            .forEach(rows::add);
        assertEquals(3, rows.size(), "row a (untouched), row b (expanded), and its one flattened child b1");

        JsonNode rowANode = this.findRowByNodeId(rows, "a");
        assertFalse(rowANode.get("expanded")
                            .asBoolean(),
                    "row a's sibling handler must NOT have fired - only the clicked row's handler runs");
        assertEquals(0, rowANode.get("depth")
                                .asInt());

        JsonNode rowBNode = this.findRowByNodeId(rows, "b");
        assertTrue(rowBNode.get("expanded")
                           .asBoolean(),
                   "row b must be expanded after its own click");
        assertEquals(0, rowBNode.get("depth")
                                .asInt());
        assertTrue(rowBNode.hasNonNull("childLoadMore"), "an expanded row must carry its own childLoadMore descriptor");

        JsonNode rowB1Node = this.findRowByNodeId(rows, "b1");
        assertNotNull(rowB1Node, "b's child b1 must be flattened into the response rows");
        assertEquals(1, rowB1Node.get("depth")
                                 .asInt(),
                     "a child of an expanded row must render at depth + 1");

        // AC3 "child fetch scoped to parentNodeId": recorded queries must target parentNodeId "b" only.
        // plan-77 note: this is now 2, not 1 - the event round-trip's SECOND render pass both (a) re-registers the
        // subtree's event handlers under the post-handler Data (so a REVEALED node's handler is discoverable on the
        // very next click - the plan-77 fix) and (b) renders the response node; each independently enumerates row
        // b's children via the provider (registration walk via getSubComponents, then render via buildNode), so one
        // pass now issues two fetches for the same newly-expanded group. Accepted, documented cost - plan-77 §2.4 /
        // §5 "Known, in-scope-accepted side effects" (idempotent, same order of cost as the render already paid).
        long scopedToB = provider.recordedQueries.stream()
                                                 .filter(query -> query.getParentNodeId()
                                                                       .map("b"::equals)
                                                                       .orElse(false))
                                                 .count();
        assertEquals(2, scopedToB, "the expand click issues one registration-walk fetch and one render fetch for the newly-expanded group (plan-77)");
        long scopedToA = provider.recordedQueries.stream()
                                                 .filter(query -> query.getParentNodeId()
                                                                       .map("a"::equals)
                                                                       .orElse(false))
                                                 .count();
        assertEquals(0, scopedToA, "row a was never expanded, so no query may ever be scoped to parentNodeId 'a'");
    }

    @Test
    public void testCollapsingAnAlreadyExpandedRowRemovesItsChildrenOnTheNextClick() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        Target targetA = this.rowTarget(treeTableNode, "a");

        // Click 1: expand row "a".
        JsonNode expandResponse = this.clickTarget(targetA, Collections.emptyMap());
        JsonNode expandedTreeTableNode = this.findTreeTableNode(expandResponse.get("targetNode")
                                                                              .get("node"));
        List<JsonNode> expandedRows = new ArrayList<>();
        expandedTreeTableNode.get("rows")
                             .forEach(expandedRows::add);
        assertEquals(4, expandedRows.size(), "row a (expanded) plus its two flattened children, plus untouched row b");
        assertTrue(this.findRowByNodeId(expandedRows, "a")
                       .get("expanded")
                       .asBoolean());

        // The echoed dataWithContext.data from click 1 carries the expanded-node-id set forward (mechanism (a));
        // a real client resubmits exactly this on the next event.
        Map<String, Object> echoedData = OBJECT_MAPPER.convertValue(expandResponse.get("dataWithContext")
                                                                                  .get("data"),
                                                                    Map.class);

        // Click 2: click the SAME (already-registered) target again to collapse.
        JsonNode collapseResponse = this.clickTarget(targetA, echoedData);
        JsonNode collapsedTreeTableNode = this.findTreeTableNode(collapseResponse.get("targetNode")
                                                                                 .get("node"));
        List<JsonNode> collapsedRows = new ArrayList<>();
        collapsedTreeTableNode.get("rows")
                              .forEach(collapsedRows::add);
        assertEquals(2, collapsedRows.size(), "collapsing row a must remove its two children, leaving only the two root rows");
        assertFalse(this.findRowByNodeId(collapsedRows, "a")
                        .get("expanded")
                        .asBoolean(),
                    "row a must be collapsed again after the second click on the same target");
    }

    @Test
    public void testGetUiReturnsTheCorrectCollapsedBaselineTree() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertNotNull(treeTableNode);
        List<JsonNode> rows = new ArrayList<>();
        treeTableNode.get("rows")
                     .forEach(rows::add);
        assertEquals(2, rows.size(), "a stateless GET /ui carries no submitted Data, so no node can be pre-expanded");
        for (JsonNode row : rows)
        {
            assertFalse(row.get("expanded")
                           .asBoolean(),
                        "every row of a fresh GET /ui must be collapsed");
            assertEquals(0, row.get("depth")
                               .asInt());
            assertTrue(row.get("expandable")
                          .asBoolean(),
                       "both root rows 'a' and 'b' are expandable per the provider");
        }
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

    private JsonNode clickTarget(Target target, Map<String, Object> data) throws Exception
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

    private Target toTarget(JsonNode targetNode) throws Exception
    {
        return OBJECT_MAPPER.treeToValue(targetNode, Target.class);
    }

    private Target rowTarget(JsonNode treeTableNode, String nodeId) throws Exception
    {
        List<JsonNode> rows = new ArrayList<>();
        treeTableNode.get("rows")
                     .forEach(rows::add);
        return this.toTarget(this.findRowByNodeId(rows, nodeId)
                                 .get("target"));
    }

    private JsonNode findRowByNodeId(List<JsonNode> rows, String nodeId)
    {
        return rows.stream()
                   .filter(row -> nodeId.equals(row.get("nodeId")
                                                   .asText()))
                   .findFirst()
                   .orElse(null);
    }

    private static JsonNode findTreeTableNode(JsonNode node)
    {
        if (node == null)
        {
            return null;
        }
        if (node.isObject())
        {
            if (node.has("type") && "TREETABLE".equals(node.get("type")
                                                           .asText()))
            {
                return node;
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                JsonNode result = findTreeTableNode(node.get(fieldNames.next()));
                if (result != null)
                {
                    return result;
                }
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                JsonNode result = findTreeTableNode(child);
                if (result != null)
                {
                    return result;
                }
            }
        }
        return null;
    }
}
