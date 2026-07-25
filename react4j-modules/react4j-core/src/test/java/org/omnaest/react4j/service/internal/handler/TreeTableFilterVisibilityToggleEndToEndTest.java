package org.omnaest.react4j.service.internal.handler;

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
 * plan-77 - the decisive collapsible-filter-row seam test (backend track): proves, through a REAL
 * {@code POST /ui/event} round trip, that clicking the funnel filter-visibility toggle flips {@code filtersVisible}
 * in the SAME event response (mechanism (a), no one-roundtrip lag - mirroring
 * {@link TreeTableFilterSortEndToEndTest}/{@link TreeTableLoadMoreEndToEndTest}'s proof shape), and that the toggle's
 * {@link Target} is distinct from every per-column filter/sort {@link Target} AND the root load-more {@link Target}
 * (trap #4 guard).
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 */
@SpringBootTest(classes = TreeTableFilterVisibilityToggleEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableFilterVisibilityToggleEndToEndTest
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
     * A flat (no children) 2-row provider - filtersVisible/activeFilterCount behavior does not depend on the
     * provider's own filter/sort honoring, so this stays minimal (react4j-core cannot depend on
     * react4j-core-components' {@code InMemoryTreeTableDataProvider}).
     */
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
                                                   .collect(java.util.stream.Collectors.toList());
            return TreeTablePage.of(windowed, OptionalLong.empty());
        }
    }

    private void registerTreeTable()
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("age", "Age"))
                                                                                                   .withDataProvider(new SimpleProvider())
                                                                                                   .withWindowSize(1)));
    }

    @Test
    public void testFunnelToggleFlipsFiltersVisibleInTheSameEventResponse() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertFalse(treeTableNode.get("filtersVisible")
                                 .asBoolean(),
                    "filtersVisible must default to false (collapsed) at baseline");
        Target filterToggleTarget = this.toTarget(treeTableNode.get("filterToggleTarget"));

        // THE decisive click: firing the funnel toggle's Target must flip filtersVisible - and the SAME event's
        // response must already reflect it (mechanism (a), no one-roundtrip lag).
        JsonNode clickResponse = this.clickTarget(filterToggleTarget, Collections.emptyMap());
        assertFalse(clickResponse.isNull(), "a filter-toggle click must not return a bare null response (plan-29 regression class)");
        assertTrue(clickResponse.hasNonNull("targetNode"));
        JsonNode toggledNode = this.findTreeTableNode(clickResponse.get("targetNode")
                                                                   .get("node"));
        assertTrue(toggledNode.get("filtersVisible")
                              .asBoolean(),
                   "the SAME /ui/event response must already reflect filtersVisible=true - no one-roundtrip lag");

        // Clicking again (echoing forward the accumulated Data, exactly as a real client resubmits the FULL Data
        // snapshot on every subsequent event) must flip it back off - a server-computed cycle, mirroring the sort
        // toggle's server-computed direction cycle.
        Map<String, Object> echoedData = OBJECT_MAPPER.convertValue(clickResponse.get("dataWithContext")
                                                                                 .get("data"),
                                                                    Map.class);
        JsonNode secondClickResponse = this.clickTarget(filterToggleTarget, echoedData);
        JsonNode toggledBackNode = this.findTreeTableNode(secondClickResponse.get("targetNode")
                                                                             .get("node"));
        assertFalse(toggledBackNode.get("filtersVisible")
                                   .asBoolean(),
                    "a second click on the SAME toggle must flip filtersVisible back to false");
    }

    @Test
    public void testFunnelToggleTargetIsDistinctFromEveryColumnFilterAndSortAndLoadMoreTarget() throws Exception
    {
        this.registerTreeTable();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        Target filterToggleTarget = this.toTarget(treeTableNode.get("filterToggleTarget"));
        assertTrue(treeTableNode.get("loadMore")
                                .get("available")
                                .asBoolean(),
                   "windowSize=1 against 2 rows must offer load-more so its Target is non-empty and meaningful for this distinctness check");
        Target loadMoreTarget = this.toTarget(treeTableNode.get("loadMore")
                                                           .get("target"));

        List<Target> otherTargets = new ArrayList<>();
        otherTargets.add(loadMoreTarget);
        for (JsonNode column : treeTableNode.get("columns"))
        {
            otherTargets.add(this.toTarget(column.get("filterTarget")));
            otherTargets.add(this.toTarget(column.get("sortTarget")));
        }

        for (Target other : otherTargets)
        {
            assertNotEquals(filterToggleTarget, other, "the funnel toggle's Target must be distinct from every sort/filter/load-more Target (trap #4)");
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
