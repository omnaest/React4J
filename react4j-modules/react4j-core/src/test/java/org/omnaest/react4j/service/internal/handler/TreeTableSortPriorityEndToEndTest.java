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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.treetable.provider.SortColumn;
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
 * plan-81 - the decisive single-vs-multi column sort mode + priority re-rank/reorder seam test (backend track):
 * proves, through a REAL {@code POST /ui/event} round trip, that (a) in single-column mode (the new default)
 * clicking a second column's sort toggle REPLACES the whole sort list, resetting the first column's
 * {@code sortPriority} back to 0, in the SAME event response (no round-trip lag, mirroring
 * {@link TreeTableFilterSortEndToEndTest}'s proof shape), that (b) in multi-column mode the priority re-rank
 * control's reorder click moves a column to its chosen 1-based position in the SAME event response, and that (c)
 * the reorder control's {@link Target} is distinct from every sort-toggle/funnel/filter/flat/load-more
 * {@link Target} (trap #4 guard).
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 */
@SpringBootTest(classes = TreeTableSortPriorityEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableSortPriorityEndToEndTest
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
     * A flat (no children), 3-sortable-column, 2-row provider (react4j-core cannot depend on
     * react4j-core-components' {@code InMemoryTreeTableDataProvider}). Row order/sorting is irrelevant to every
     * assertion in this file &mdash; only the emitted per-column {@code sortPriority}/{@code sortPriorityFieldKey}/
     * {@code sortPriorityReorderTarget} and the ordered sort field are asserted &mdash; so this stays a minimal
     * windowing-only provider, unlike {@link TreeTableFilterSortEndToEndTest}'s
     * {@code TrackingSortableFilterableProvider}.
     */
    private static class SimpleThreeColumnProvider implements TreeTableDataProvider
    {
        private final List<TreeTableRow> rows = List.of(TreeTableRow.of("a", Map.of("name", "Bob", "age", 20, "city", "Berlin"), false),
                                                        TreeTableRow.of("b", Map.of("name", "Alice", "age", 30, "city", "Amsterdam"), false));

        @Override
        public TreeTablePage fetch(TreeTableQuery query)
        {
            List<TreeTableRow> windowed = this.rows.stream()
                                                   .skip(query.getOffset())
                                                   .limit(query.getLimit())
                                                   .collect(java.util.stream.Collectors.toList());
            return TreeTablePage.of(windowed, OptionalLong.empty());
        }
    }

    private void registerTreeTable(boolean multiColumnSortEnabled, boolean flatModeToggleEnabled, int windowSize)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("age", "Age"),
                                                                                                                TreeTableColumn.of("city", "City"))
                                                                                                   .withDataProvider(new SimpleThreeColumnProvider())
                                                                                                   .withWindowSize(windowSize)
                                                                                                   .withMultiColumnSortEnabled(multiColumnSortEnabled)
                                                                                                   .withFlatModeToggleEnabled(flatModeToggleEnabled)));
    }

    /**
     * A flat 2-column ("name"/"owner"), 2-row provider used ONLY by the plan-82 bugfix seam test below - "name"
     * carries a seeded {@code withInitialSortDirection(DESCENDING)}, "owner" does not (see
     * {@link #testMultiColumnModeFirstSortToggleClickFromTheSeededBaselineBuildsOnTheInitialSort()}).
     */
    private static class NameOwnerProvider implements TreeTableDataProvider
    {
        private final List<TreeTableRow> rows = List.of(TreeTableRow.of("1", Map.of("name", "Bob", "owner", "Alice"), false),
                                                        TreeTableRow.of("2", Map.of("name", "Ann", "owner", "Zoe"), false));

        @Override
        public TreeTablePage fetch(TreeTableQuery query)
        {
            List<TreeTableRow> windowed = this.rows.stream()
                                                   .skip(query.getOffset())
                                                   .limit(query.getLimit())
                                                   .collect(java.util.stream.Collectors.toList());
            return TreeTablePage.of(windowed, OptionalLong.empty());
        }
    }

    /**
     * plan-82 bugfix (live-testing report): seeded baseline is Name DESC ({@link TreeTableColumn#withInitialSortDirection(SortColumn.SortDirection)}),
     * multi-column sort enabled - mirrors the exact showcase repro.
     */
    private void registerTreeTableWithNameSeededDescendingMultiColumnSort()
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name")
                                                                                                                               .withInitialSortDirection(SortColumn.SortDirection.DESCENDING),
                                                                                                                TreeTableColumn.of("owner", "Owner"))
                                                                                                   .withDataProvider(new NameOwnerProvider())
                                                                                                   .withWindowSize(500)
                                                                                                   .withMultiColumnSortEnabled(true)));
    }

    @Test
    public void testMultiColumnModeFirstSortToggleClickFromTheSeededBaselineBuildsOnTheInitialSort() throws Exception
    {
        this.registerTreeTableWithNameSeededDescendingMultiColumnSort();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        // Confirm the seeded baseline itself first: Name must already render DESCENDING, priority 1, BEFORE any
        // click - exactly the live-repro's starting point.
        assertEquals("DESCENDING",
                     this.column(treeTableNode, "name")
                         .get("sortDirection")
                         .asText());
        assertEquals(1, this.column(treeTableNode, "name")
                            .get("sortPriority")
                            .asInt());
        assertEquals(1, treeTableNode.get("activeSortCount")
                                     .asInt());

        Target ownerSortTarget = this.toTarget(this.column(treeTableNode, "owner")
                                                   .get("sortTarget"));

        // THE decisive first click: an empty submitted Data map, exactly the seeded baseline's very first round
        // trip - the sort field has never been written by the client yet.
        JsonNode afterOwnerClick = this.clickTarget(ownerSortTarget, Collections.emptyMap());
        assertFalse(afterOwnerClick.isNull(), "a sort toggle click must not return a bare null response (plan-29 regression class)");
        JsonNode nodeAfterOwnerClick = this.findTreeTableNode(afterOwnerClick.get("targetNode")
                                                                             .get("node"));
        assertEquals(2, nodeAfterOwnerClick.get("activeSortCount")
                                           .asInt(),
                     "the seeded Name DESC must NOT be discarded by the first sort-toggle click on Owner - BOTH columns must now be active");
        assertEquals(1, this.column(nodeAfterOwnerClick, "name")
                            .get("sortPriority")
                            .asInt(),
                     "Name must remain priority 1 - the seed must not be overwritten");
        assertEquals("DESCENDING",
                     this.column(nodeAfterOwnerClick, "name")
                         .get("sortDirection")
                         .asText());
        assertEquals(2, this.column(nodeAfterOwnerClick, "owner")
                            .get("sortPriority")
                            .asInt(),
                     "Owner must become priority 2, appended after the seeded Name");
        assertEquals("ASCENDING",
                     this.column(nodeAfterOwnerClick, "owner")
                         .get("sortDirection")
                         .asText());
    }

    @Test
    public void testSingleColumnModeSortToggleClickResetsThePreviouslyActiveColumnInTheSameEventResponse() throws Exception
    {
        // multiColumnSortEnabled defaults to false (single-column mode) - not opted in here.
        this.registerTreeTable(false, false, 500);

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertFalse(treeTableNode.get("multiColumnSortEnabled")
                                 .asBoolean(),
                    "multiColumnSortEnabled must render false by default");
        Target nameSortTarget = this.toTarget(this.column(treeTableNode, "name")
                                                  .get("sortTarget"));
        Target ageSortTarget = this.toTarget(this.column(treeTableNode, "age")
                                                 .get("sortTarget"));

        // Click 1: sort by name (becomes the sole active column, priority 1).
        JsonNode nameClickResponse = this.clickTarget(nameSortTarget, Collections.emptyMap());
        JsonNode nameSortedNode = this.findTreeTableNode(nameClickResponse.get("targetNode")
                                                                          .get("node"));
        assertEquals(1, this.column(nameSortedNode, "name")
                            .get("sortPriority")
                            .asInt());
        assertEquals(1, nameSortedNode.get("activeSortCount")
                                      .asInt());

        // Click 2: sort by age WHILE name is still active - THE decisive click: the SAME event's response must
        // already show the sort list REPLACED (single-column mode), not accumulated.
        Map<String, Object> echoedData = OBJECT_MAPPER.convertValue(nameClickResponse.get("dataWithContext")
                                                                                     .get("data"),
                                                                    Map.class);
        JsonNode ageClickResponse = this.clickTarget(ageSortTarget, echoedData);
        assertFalse(ageClickResponse.isNull(), "a sort toggle click must not return a bare null response (plan-29 regression class)");
        JsonNode ageSortedNode = this.findTreeTableNode(ageClickResponse.get("targetNode")
                                                                        .get("node"));
        assertEquals(0, this.column(ageSortedNode, "name")
                            .get("sortPriority")
                            .asInt(),
                     "single-column mode: name's sortPriority must be reset to 0 by age's click, in the SAME event response");
        assertTrue(this.column(ageSortedNode, "name")
                       .get("sortDirection")
                       .isNull(),
                   "single-column mode: name must no longer be sorted at all once age becomes the sole active column");
        assertEquals(1, this.column(ageSortedNode, "age")
                            .get("sortPriority")
                            .asInt(),
                     "age must become the sole active column, priority 1");
        assertEquals(1, ageSortedNode.get("activeSortCount")
                                     .asInt(),
                     "single-column mode: at most one column may ever be active");
    }

    @Test
    public void testMultiColumnModeReorderClickMovesColumnToChosenPriorityInTheSameEventResponse() throws Exception
    {
        this.registerTreeTable(true, false, 500);

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertTrue(treeTableNode.get("multiColumnSortEnabled")
                                .asBoolean());
        Target nameSortTarget = this.toTarget(this.column(treeTableNode, "name")
                                                  .get("sortTarget"));
        Target ageSortTarget = this.toTarget(this.column(treeTableNode, "age")
                                                 .get("sortTarget"));
        Target citySortTarget = this.toTarget(this.column(treeTableNode, "city")
                                                  .get("sortTarget"));

        // Establish [name=1, age=2, city=3] via three accumulating clicks (click order = priority order, echoing
        // forward the accumulated Data exactly as a real client resubmits the full snapshot on every event).
        JsonNode afterName = this.clickTarget(nameSortTarget, Collections.emptyMap());
        JsonNode afterNameThenAge = this.clickTarget(ageSortTarget, this.echoedData(afterName));
        JsonNode afterAllThree = this.clickTarget(citySortTarget, this.echoedData(afterNameThenAge));
        JsonNode threeSortedNode = this.findTreeTableNode(afterAllThree.get("targetNode")
                                                                       .get("node"));
        assertEquals(1, this.column(threeSortedNode, "name")
                            .get("sortPriority")
                            .asInt());
        assertEquals(2, this.column(threeSortedNode, "age")
                            .get("sortPriority")
                            .asInt());
        assertEquals(3, this.column(threeSortedNode, "city")
                            .get("sortPriority")
                            .asInt());
        assertEquals(3, threeSortedNode.get("activeSortCount")
                                       .asInt());

        JsonNode cityColumn = this.column(threeSortedNode, "city");
        String cityPriorityFieldKey = cityColumn.get("sortPriorityFieldKey")
                                                .asText();
        Target cityReorderTarget = this.toTarget(cityColumn.get("sortPriorityReorderTarget"));

        // THE decisive reorder click: write city's chosen new priority (1) into its field, then fire the reorder
        // Target (mechanism (b), mirrors the filter input's write-then-dispatch pattern) - echoing forward the
        // accumulated Data from all three prior sort clicks.
        Map<String, Object> dataForReorder = this.echoedData(afterAllThree);
        dataForReorder.put(cityPriorityFieldKey, 1);
        JsonNode reorderResponse = this.clickTarget(cityReorderTarget, dataForReorder);
        assertFalse(reorderResponse.isNull(), "a reorder click must not return a bare null response (plan-29 regression class)");
        JsonNode reorderedNode = this.findTreeTableNode(reorderResponse.get("targetNode")
                                                                       .get("node"));
        assertEquals(1, this.column(reorderedNode, "city")
                            .get("sortPriority")
                            .asInt(),
                     "the SAME /ui/event response must already reflect city moved to priority 1 - no round-trip lag");
        assertEquals(2, this.column(reorderedNode, "name")
                            .get("sortPriority")
                            .asInt(),
                     "name must shift down to priority 2");
        assertEquals(3, this.column(reorderedNode, "age")
                            .get("sortPriority")
                            .asInt(),
                     "age must shift down to priority 3");
        assertEquals("ASCENDING",
                     this.column(reorderedNode, "city")
                         .get("sortDirection")
                         .asText(),
                     "the reorder must not change any column's direction, only its position");
    }

    @Test
    public void testReorderTargetIsDistinctFromEverySortToggleFunnelFilterFlatAndLoadMoreTarget() throws Exception
    {
        this.registerTreeTable(true, true, 1);

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        Target nameSortTarget = this.toTarget(this.column(treeTableNode, "name")
                                                  .get("sortTarget"));

        // Make "name" the sole active column so its reorder control becomes routable.
        JsonNode afterName = this.clickTarget(nameSortTarget, Collections.emptyMap());
        JsonNode nameSortedNode = this.findTreeTableNode(afterName.get("targetNode")
                                                                  .get("node"));
        JsonNode nameColumn = this.column(nameSortedNode, "name");
        Target nameReorderTarget = this.toTarget(nameColumn.get("sortPriorityReorderTarget"));
        assertFalse(nameReorderTarget.isEmpty(), "the sole active column in multi mode must carry a routable sortPriorityReorderTarget");

        List<Target> otherTargets = new ArrayList<>();
        otherTargets.add(this.toTarget(nameColumn.get("sortTarget")));
        otherTargets.add(this.toTarget(nameColumn.get("filterTarget")));
        otherTargets.add(this.toTarget(nameSortedNode.get("filterToggleTarget")));
        otherTargets.add(this.toTarget(nameSortedNode.get("flatToggleTarget")));
        assertTrue(nameSortedNode.get("loadMore")
                                 .get("available")
                                 .asBoolean(),
                   "windowSize=1 against 2 rows must offer load-more so its Target is available for this distinctness check");
        otherTargets.add(this.toTarget(nameSortedNode.get("loadMore")
                                                     .get("target")));
        for (JsonNode column : nameSortedNode.get("columns"))
        {
            if (!"name".equals(column.get("key")
                                     .asText()))
            {
                otherTargets.add(this.toTarget(column.get("sortTarget")));
                otherTargets.add(this.toTarget(column.get("filterTarget")));
            }
        }

        for (Target other : otherTargets)
        {
            assertNotEquals(nameReorderTarget, other, "the reorder control's Target must be distinct from every other TreeTable control's Target (trap #4)");
        }
    }

    private Map<String, Object> echoedData(JsonNode clickResponse)
    {
        return new HashMap<>(OBJECT_MAPPER.convertValue(clickResponse.get("dataWithContext")
                                                                     .get("data"),
                                                        Map.class));
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

    private JsonNode column(JsonNode treeTableNode, String key)
    {
        for (JsonNode column : treeTableNode.get("columns"))
        {
            if (key.equals(column.get("key")
                                 .asText()))
            {
                return column;
            }
        }
        return null;
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
