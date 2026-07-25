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
 * plan-77 AC-F2 - the decisive DOMAIN-LEVEL seam test: expands a root Tree Table row, then expands a row REVEALED by
 * that first expand (a depth-1 child row that is itself expandable) - all within ONE session via real
 * {@code POST /ui/event} round trips. {@link TreeTableExpandCollapseEndToEndTest} only ever expands ROOT rows
 * (registered at {@code GET /ui}) and its revealed children are all {@code expandable=false}, so it never exercises
 * a click on a revealed expandable node; this test is precisely that omitted case.
 * <p>
 * RED on current code: the depth-1 child row's expand-toggle handler was only ever discoverable through
 * {@code RerenderingContainerImpl.getSubComponents}'s hardcoded {@code Data.empty()} walk, which never sees a
 * revealed node - so clicking the revealed row's caret does not expand it (its handler is not registered) even
 * though the row itself is visibly rendered. GREEN once the event round-trip re-registers the container's subtree
 * under the submitted {@code Data} (plan-77 Cliff F1/F2).
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 * @see TreeTableExpandCollapseEndToEndTest
 */
@SpringBootTest(classes = TreeTableDepth2ExpandEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableDepth2ExpandEndToEndTest
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
     * A three-level tree: root row "a" (expandable) -&gt; depth-1 child "a1" (ITSELF expandable, plus sibling "a2"
     * which is not) -&gt; depth-2 grandchild "a1x". Records every query so the test can assert the grandchild fetch
     * was scoped to exactly {@code parentNodeId="a1"} (AC-F2).
     */
    private static class TrackingTreeProvider implements TreeTableDataProvider
    {
        final List<TreeTableQuery>                    recordedQueries    = new ArrayList<>();
        private final Map<String, List<TreeTableRow>> childrenByParentId = new LinkedHashMap<>();

        TrackingTreeProvider()
        {
            this.childrenByParentId.put(null, List.of(TreeTableRow.of("a", Map.of("name", "A"), true)));
            this.childrenByParentId.put("a",
                                        List.of(TreeTableRow.of("a1", Map.of("name", "A1"), true), TreeTableRow.of("a2", Map.of("name", "A2"), false)));
            this.childrenByParentId.put("a1", List.of(TreeTableRow.of("a1x", Map.of("name", "A1X"), false)));
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
    public void testExpandingARevealedDepth1ChildRowFiresItsOwnHandlerAndInsertsGrandchildrenAtDepthPlusTwo() throws Exception
    {
        TrackingTreeProvider provider = this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        Target targetA = this.rowTarget(treeTableNode, "a");

        // Event 1: expand root row "a" - reveals depth-1 child row "a1", which is itself expandable.
        JsonNode expandAResponse = this.clickTarget(targetA, Collections.emptyMap());
        assertFalse(expandAResponse.isNull(), "expanding root row a must not return a bare null response");
        JsonNode afterExpandA = this.findTreeTableNode(expandAResponse.get("targetNode")
                                                                      .get("node"));
        List<JsonNode> rowsAfterExpandA = this.rowsOf(afterExpandA);
        assertEquals(3, rowsAfterExpandA.size(), "row a (expanded) plus its two flattened children a1/a2");

        JsonNode rowA1Revealed = this.findRowByNodeId(rowsAfterExpandA, "a1");
        assertNotNull(rowA1Revealed, "revealed depth-1 child a1 must be present after expanding root a");
        assertTrue(rowA1Revealed.get("expandable")
                                .asBoolean(),
                   "a1 must itself be expandable - the depth-2 case this test targets");
        assertFalse(rowA1Revealed.get("expanded")
                                 .asBoolean(),
                    "a1 is revealed but not yet expanded");
        assertEquals(1, rowA1Revealed.get("depth")
                                     .asInt());
        Target targetA1 = this.toTarget(rowA1Revealed.get("target"));

        // The client echoes forward the Data that expanded "a" (mechanism (a)) - a real client resubmits exactly this.
        Map<String, Object> echoedData = OBJECT_MAPPER.convertValue(expandAResponse.get("dataWithContext")
                                                                                   .get("data"),
                                                                    Map.class);

        provider.recordedQueries.clear();

        // THE decisive click: expand the REVEALED (not registered at GET /ui) depth-1 child row a1.
        JsonNode expandA1Response = this.clickTarget(targetA1, echoedData);
        assertFalse(expandA1Response.isNull(), "expanding a REVEALED depth-1 row must not return a bare null response (plan-77 regression class)");
        assertTrue(expandA1Response.hasNonNull("targetNode"), "response targetNode must be non-null");

        JsonNode afterExpandA1 = this.findTreeTableNode(expandA1Response.get("targetNode")
                                                                        .get("node"));
        List<JsonNode> rowsAfterExpandA1 = this.rowsOf(afterExpandA1);
        assertEquals(4, rowsAfterExpandA1.size(), "row a, row a1 (now expanded), its grandchild a1x, and untouched a2");

        JsonNode rowA1Expanded = this.findRowByNodeId(rowsAfterExpandA1, "a1");
        assertTrue(rowA1Expanded.get("expanded")
                                .asBoolean(),
                   "a1 must be expanded - its own click's handler must have fired (proves it was actually registered)");
        assertEquals(1, rowA1Expanded.get("depth")
                                     .asInt());

        JsonNode rowA1x = this.findRowByNodeId(rowsAfterExpandA1, "a1x");
        assertNotNull(rowA1x, "a1's child a1x must be flattened into the response rows");
        assertEquals(2, rowA1x.get("depth")
                              .asInt(),
                     "a grandchild of the root must render at depth + 2");

        // plan-77 note: 2, not 1 - the event's second render pass both re-registers a1's now-expanded subtree
        // (getSubComponents, discovering a1x for the NEXT click) and renders the response (buildNode), each
        // independently fetching parentNodeId="a1". Accepted, documented cost - see TreeTableExpandCollapseEndToEndTest
        // and plan-77 §2.4 / §5.
        long scopedToA1 = provider.recordedQueries.stream()
                                                  .filter(query -> query.getParentNodeId()
                                                                        .map("a1"::equals)
                                                                        .orElse(false))
                                                  .count();
        assertEquals(2, scopedToA1, "expanding a1 issues one registration-walk fetch and one render fetch scoped to parentNodeId 'a1' (plan-77)");
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
        return this.toTarget(this.findRowByNodeId(this.rowsOf(treeTableNode), nodeId)
                                 .get("target"));
    }

    private List<JsonNode> rowsOf(JsonNode treeTableNode)
    {
        List<JsonNode> rows = new ArrayList<>();
        treeTableNode.get("rows")
                     .forEach(rows::add);
        return rows;
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
