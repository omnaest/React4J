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
import org.omnaest.react4j.component.treetable.provider.ColumnFilter;
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
 * plan-76 Slice 6 - the decisive per-column-filter + multi-column-sort seam test (AC4): proves, through a REAL
 * {@code POST /ui/event} round trip, that (a) a filter-input change and (b) a sort-toggle click each re-render
 * reading the new state from the SUBMITTED {@link org.omnaest.react4j.domain.context.data.Data} and re-querying
 * the SAME event (no one-roundtrip lag, mirroring {@link TreeTableLoadMoreEndToEndTest} /
 * {@link TreeTableExpandCollapseEndToEndTest}'s mechanism (a) proof), and that multi-column sort order
 * (primary/secondary, index 0 = primary) is honored through the query. Also proves each header control (filter,
 * sort - per column) routes to its OWN distinct {@link Target} (trap #4 guard).
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 */
@SpringBootTest(classes = TreeTableFilterSortEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableFilterSortEndToEndTest
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
     * A flat (no children) 4-row provider honoring {@code query.getFilters()}/{@code query.getSorts()} exactly like
     * {@code InMemoryTreeTableDataProvider} (react4j-core-components cannot be depended on from react4j-core test
     * scope, so this is a small hand-rolled equivalent) and RECORDING every query it receives.
     * <p>
     * Rows: a(name=Bob, age=20), b(name=Alice, age=20) [age TIE, name TIE-BREAKER], c(name=Cherry, age=10),
     * d(name=Date, age=30).
     */
    private static class TrackingSortableFilterableProvider implements TreeTableDataProvider
    {
        final List<TreeTableQuery>       recordedQueries = new ArrayList<>();
        private final List<TreeTableRow> rows            = List.of(TreeTableRow.of("a", Map.of("name", "Bob", "age", 20), false),
                                                                   TreeTableRow.of("b", Map.of("name", "Alice", "age", 20), false),
                                                                   TreeTableRow.of("c", Map.of("name", "Cherry", "age", 10), false),
                                                                   TreeTableRow.of("d", Map.of("name", "Date", "age", 30), false));

        @Override
        public TreeTablePage fetch(TreeTableQuery query)
        {
            this.recordedQueries.add(query);

            List<TreeTableRow> filtered = new ArrayList<>(this.rows);
            for (ColumnFilter filter : query.getFilters())
            {
                filtered.removeIf(row -> !String.valueOf(row.getCells()
                                                            .get(filter.getColumnKey()))
                                                .contains(String.valueOf(filter.getValue())));
            }

            if (!query.getSorts()
                      .isEmpty())
            {
                java.util.Comparator<TreeTableRow> comparator = null;
                for (SortColumn sort : query.getSorts())
                {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    java.util.Comparator<TreeTableRow> columnComparator = java.util.Comparator.comparing(row -> (Comparable) row.getCells()
                                                                                                                                .get(sort.getColumnKey()));
                    if (sort.getDirection() == SortColumn.SortDirection.DESCENDING)
                    {
                        columnComparator = columnComparator.reversed();
                    }
                    comparator = comparator == null ? columnComparator : comparator.thenComparing(columnComparator);
                }
                filtered.sort(comparator);
            }

            return TreeTablePage.of(filtered, OptionalLong.empty());
        }
    }

    private TrackingSortableFilterableProvider registerTreeTable()
    {
        return this.registerTreeTable(false);
    }

    /**
     * @param multiColumnSortEnabled
     *            plan-81: {@code withMultiColumnSortEnabled(boolean)} defaults to {@code false} (single-column
     *            mode) - a test asserting genuinely MULTI-column sort behavior (primary+secondary accumulating
     *            across two clicks) must opt in explicitly.
     */
    private TrackingSortableFilterableProvider registerTreeTable(boolean multiColumnSortEnabled)
    {
        TrackingSortableFilterableProvider provider = new TrackingSortableFilterableProvider();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("age", "Age"))
                                                                                                   .withDataProvider(provider)
                                                                                                   .withWindowSize(500)
                                                                                                   .withMultiColumnSortEnabled(multiColumnSortEnabled)));
        return provider;
    }

    @Test
    public void testFilterInputChangeReQueriesAndReflectsFilteredRowsInTheSameEvent() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertEquals(4, this.rows(treeTableNode)
                            .size(),
                     "baseline render must show all 4 unfiltered rows");

        JsonNode nameColumn = this.column(treeTableNode, "name");
        assertTrue(nameColumn.get("filterValue")
                             .isNull(),
                   "no filter must be active at baseline");
        String filterFieldKey = nameColumn.get("filterFieldKey")
                                          .asText();
        Target filterTarget = this.toTarget(nameColumn.get("filterTarget"));

        // THE decisive click: fire the filter control's Target with the filter value already written into the
        // submitted Data under filterFieldKey (Cliff C1a mechanism (b) - the client writes the raw value directly).
        // "at" is contained ONLY in "Date" (Bob/Alice/Cherry do not contain "at").
        JsonNode clickResponse = this.clickTarget(filterTarget, Map.of(filterFieldKey, "at"));
        assertFalse(clickResponse.isNull(), "a filter change must not return a bare null response (plan-29 regression class)");
        assertTrue(clickResponse.hasNonNull("targetNode"));

        JsonNode filteredTreeTableNode = this.findTreeTableNode(clickResponse.get("targetNode")
                                                                             .get("node"));
        List<JsonNode> filteredRows = this.rows(filteredTreeTableNode);
        assertEquals(1, filteredRows.size(),
                     "the SAME /ui/event response must already reflect the filtered rows (mechanism (a)/(b), no one-roundtrip lag) - only 'Date' contains 'at'");
        assertEquals("d", filteredRows.get(0)
                                      .get("nodeId")
                                      .asText());
        assertEquals("at",
                     this.column(filteredTreeTableNode, "name")
                         .get("filterValue")
                         .asText(),
                     "the emitted filterValue must echo the currently-active filter text");
    }

    @Test
    public void testSortToggleClickReQueriesAndReflectsSortedRowsInTheSameEvent() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        JsonNode ageColumn = this.column(treeTableNode, "age");
        assertTrue(ageColumn.get("sortDirection")
                            .isNull(),
                   "no sort must be active at baseline");
        Target ageSortTarget = this.toTarget(ageColumn.get("sortTarget"));

        // THE decisive click: toggle the age column's sort - SAME event's response must already show sorted rows.
        JsonNode clickResponse = this.clickTarget(ageSortTarget, Collections.emptyMap());
        assertFalse(clickResponse.isNull(), "a sort toggle must not return a bare null response (plan-29 regression class)");

        JsonNode sortedTreeTableNode = this.findTreeTableNode(clickResponse.get("targetNode")
                                                                           .get("node"));
        List<JsonNode> sortedRows = this.rows(sortedTreeTableNode);
        List<String> orderedIds = new ArrayList<>();
        sortedRows.forEach(row -> orderedIds.add(row.get("nodeId")
                                                    .asText()));
        assertEquals(List.of("c", "a", "b", "d"), orderedIds,
                     "SAME event's response must reflect ASCENDING age order (10, 20, 20, 30) - no round-trip lag");
        assertEquals("ASCENDING",
                     this.column(sortedTreeTableNode, "age")
                         .get("sortDirection")
                         .asText(),
                     "the emitted per-column sort-direction indicator must reflect the new ASCENDING sort");
    }

    @Test
    public void testMultiColumnSortOrderIsHonoredThroughTheQueryPrimaryThenSecondary() throws Exception
    {
        // plan-81: genuinely multi-column (primary+secondary accumulating across two clicks) requires opting in -
        // withMultiColumnSortEnabled defaults to false (single-column mode).
        this.registerTreeTable(true);

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        Target ageSortTarget = this.toTarget(this.column(treeTableNode, "age")
                                                 .get("sortTarget"));
        Target nameSortTarget = this.toTarget(this.column(treeTableNode, "name")
                                                  .get("sortTarget"));
        assertNotEquals(ageSortTarget, nameSortTarget, "each column's sort toggle must resolve to a distinct positional Target (trap #4)");

        // Click 1: sort by age (becomes PRIMARY, ASCENDING).
        JsonNode ageClickResponse = this.clickTarget(ageSortTarget, Collections.emptyMap());
        Map<String, Object> echoedData = OBJECT_MAPPER.convertValue(ageClickResponse.get("dataWithContext")
                                                                                    .get("data"),
                                                                    Map.class);

        // Click 2: sort by name (becomes SECONDARY, ASCENDING) - echoing forward click 1's accumulated Data, exactly
        // as a real client resubmits the FULL Data snapshot on every subsequent event.
        JsonNode nameClickResponse = this.clickTarget(nameSortTarget, echoedData);
        JsonNode multiSortedNode = this.findTreeTableNode(nameClickResponse.get("targetNode")
                                                                           .get("node"));
        List<String> orderedIds = new ArrayList<>();
        this.rows(multiSortedNode)
            .forEach(row -> orderedIds.add(row.get("nodeId")
                                              .asText()));

        // Primary (age ASC) groups rows as [10, 20, 20, 30] = [c, {a,b}, d]; secondary (name ASC) resolves the
        // age=20 tie by name: Alice (b) before Bob (a) - proving BOTH the primary grouping AND the secondary
        // tie-break are honored, in click order (index 0 = primary).
        assertEquals(List.of("c", "b", "a", "d"), orderedIds,
                     "multi-column sort must honor age ASC as primary and name ASC as secondary tie-breaker, in click order");

        assertEquals("ASCENDING",
                     this.column(multiSortedNode, "age")
                         .get("sortDirection")
                         .asText());
        assertEquals("ASCENDING",
                     this.column(multiSortedNode, "name")
                         .get("sortDirection")
                         .asText());
    }

    @Test
    public void testFilterAndSortControlsEachRouteToTheirOwnDistinctTargetAndDoNotCrossAffectEachOther() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        JsonNode nameColumn = this.column(treeTableNode, "name");
        JsonNode ageColumn = this.column(treeTableNode, "age");
        Target nameFilterTarget = this.toTarget(nameColumn.get("filterTarget"));
        Target nameSortTarget = this.toTarget(nameColumn.get("sortTarget"));
        Target ageFilterTarget = this.toTarget(ageColumn.get("filterTarget"));
        Target ageSortTarget = this.toTarget(ageColumn.get("sortTarget"));

        assertNotEquals(nameFilterTarget, nameSortTarget, "the SAME column's filter and sort controls must resolve to distinct Targets (trap #4)");
        assertNotEquals(nameFilterTarget, ageFilterTarget, "different columns' filter controls must resolve to distinct Targets (trap #4)");
        assertNotEquals(nameSortTarget, ageSortTarget, "different columns' sort toggles must resolve to distinct Targets (trap #4)");

        // Firing ONLY the sort toggle must never introduce a filter value.
        JsonNode sortOnlyResponse = this.clickTarget(ageSortTarget, Collections.emptyMap());
        JsonNode sortOnlyNode = this.findTreeTableNode(sortOnlyResponse.get("targetNode")
                                                                       .get("node"));
        assertTrue(this.column(sortOnlyNode, "name")
                       .get("filterValue")
                       .isNull(),
                   "clicking a sort toggle must not activate any filter");

        // Firing ONLY the filter control must never introduce a sort.
        String nameFilterFieldKey = nameColumn.get("filterFieldKey")
                                              .asText();
        JsonNode filterOnlyResponse = this.clickTarget(nameFilterTarget, Map.of(nameFilterFieldKey, "e"));
        JsonNode filterOnlyNode = this.findTreeTableNode(filterOnlyResponse.get("targetNode")
                                                                           .get("node"));
        assertTrue(this.column(filterOnlyNode, "age")
                       .get("sortDirection")
                       .isNull(),
                   "clicking a filter control must not activate any sort");
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

    private List<JsonNode> rows(JsonNode treeTableNode)
    {
        List<JsonNode> rows = new ArrayList<>();
        treeTableNode.get("rows")
                     .forEach(rows::add);
        return rows;
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
