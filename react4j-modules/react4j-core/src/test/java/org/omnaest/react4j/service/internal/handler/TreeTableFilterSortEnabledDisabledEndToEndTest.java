package org.omnaest.react4j.service.internal.handler;

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
 * plan-78 - the decisive filter/sort enable-disable seam test (backend track): proves, through a REAL
 * {@code GET /ui} + {@code POST /ui/event} round trip, that {@code TreeTable.withFilterEnabled(false)} removes the
 * funnel toggle from the rendered node AND makes its would-be {@link Target} no longer route to any registered
 * handler (a bare-null response, mirroring the plan-29 regression class this guards against) - and that the enabled
 * path (default configuration) still flips {@code filtersVisible} in the same event, unaffected by this change
 * (regression, mirrors {@link TreeTableFilterVisibilityToggleEndToEndTest}).
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 */
@SpringBootTest(classes = TreeTableFilterSortEnabledDisabledEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableFilterSortEnabledDisabledEndToEndTest
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
     * A flat (no children) 2-row provider - identical shape to
     * {@link TreeTableFilterVisibilityToggleEndToEndTest}'s {@code SimpleProvider} (react4j-core cannot depend on
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

    private void registerTreeTable(boolean filterEnabled)
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("age", "Age"))
                                                                                                   .withDataProvider(new SimpleProvider())
                                                                                                   .withWindowSize(1)
                                                                                                   .withFilterEnabled(filterEnabled)));
    }

    @Test
    public void testFilterDisabledOmitsFunnelToggleFromGetUiAndItsWouldBeTargetNoLongerRoutes() throws Exception
    {
        this.registerTreeTable(false);

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertFalse(treeTableNode.get("filterEnabled")
                                 .asBoolean(),
                    "filterEnabled must render false when withFilterEnabled(false) was configured");
        assertTrue(treeTableNode.get("filterToggleTarget")
                                .isEmpty(),
                   "GET /ui must not carry a routable filterToggleTarget when filtering is disabled - the funnel toggle is absent");

        // Reconstruct the would-be funnel-toggle Target the SAME way the enabled case's own Location is composed
        // (gridLocation + "filtertoggle" segment, see TreeTableFilterVisibilityToggleEndToEndTest and
        // TreeTableRendererImpl#createFilterOrSortLocation) - derived from the STILL-present, unaffected load-more
        // Target, since filtering being disabled must not touch load-more.
        assertTrue(treeTableNode.get("loadMore")
                                .get("available")
                                .asBoolean(),
                   "windowSize=1 against 2 rows must offer load-more so its Target is available as an anchor to derive the grid's own Location");
        Target loadMoreTarget = this.toTarget(treeTableNode.get("loadMore")
                                                           .get("target"));
        List<String> gridPath = new ArrayList<>(loadMoreTarget.get()
                                                              .subList(0, loadMoreTarget.get()
                                                                                        .size()
                                                                          - 1));
        gridPath.add("filtertoggle");
        Target wouldBeFilterToggleTarget = Target.from(() -> gridPath);

        JsonNode clickResponse = this.clickTarget(wouldBeFilterToggleTarget, Collections.emptyMap());
        assertTrue(clickResponse.isNull(),
                   "a disabled filter feature's would-be funnel-toggle Target must not route to any registered handler - a bare-null response, since no sub-component was ever constructed for it");
    }

    @Test
    public void testFilterEnabledDefaultStillFlipsFiltersVisibleInTheSameEvent() throws Exception
    {
        this.registerTreeTable(true);

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertTrue(treeTableNode.get("filterEnabled")
                                .asBoolean(),
                   "filterEnabled must render true when withFilterEnabled(true) (the default) was configured");
        assertFalse(treeTableNode.get("filtersVisible")
                                 .asBoolean(),
                    "filtersVisible must default to false (collapsed) at baseline");
        Target filterToggleTarget = this.toTarget(treeTableNode.get("filterToggleTarget"));

        JsonNode clickResponse = this.clickTarget(filterToggleTarget, Collections.emptyMap());
        assertFalse(clickResponse.isNull(), "the enabled path must not regress into a bare-null response");
        JsonNode toggledNode = this.findTreeTableNode(clickResponse.get("targetNode")
                                                                   .get("node"));
        assertTrue(toggledNode.get("filtersVisible")
                              .asBoolean(),
                   "the SAME /ui/event response must already reflect filtersVisible=true - the enabled path must remain unaffected by the new enable/disable gating");
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
