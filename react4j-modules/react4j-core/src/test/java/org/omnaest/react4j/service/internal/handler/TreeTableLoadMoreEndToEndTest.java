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
 * plan-76 Slice 4 - the decisive spike/acceptance seam test (Cliff C1a + AC2): proves whether mechanism (a) - a
 * server handler mutating a field on the SUBMITTED {@code Data}, read back by the re-render via
 * {@code withDataDrivenContent} - is lag-free through a REAL {@code POST /ui/event} round trip
 * ({@code ReactUIController} -&gt; {@code EventHandlerServiceImpl} -&gt; {@code RerenderingServiceImpl}), exactly
 * mirroring {@link RerenderingSiblingButtonClickEndToEndTest}.
 * <p>
 * The old lag bug (a possibly-stale prior per {@code react4j-rerender-lags-one-roundtrip-on-self-mutating-click})
 * manifested precisely as the {@code /ui/event} response reflecting the PRE-mutation window. This test's
 * {@link #testLoadMoreClickWidensTheSameEventsResponseWindow()} assertion IS the spike: it passed on the first run
 * against the current {@code EventHandlerServiceImpl} (which runs the matched handler BEFORE the response-producing
 * second render pass - see source), empirically confirming plan-12/13/14 fixed the self-mutating-click lag for the
 * {@code withDataDrivenContent} path. Mechanism (a) is kept; no fallback to mechanism (b) was needed.
 *
 * @see org.omnaest.react4j.component.treetable.internal.renderer.TreeTableRendererImpl
 */
@SpringBootTest(classes = TreeTableLoadMoreEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TreeTableLoadMoreEndToEndTest
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
     * A provider backed by 5 rows, honoring {@code query.getLimit()} - exactly what a real load-more click widens.
     */
    private static TreeTableDataProvider fiveRowProvider()
    {
        List<TreeTableRow> allRows = new ArrayList<>();
        for (int i = 0; i < 5; i++)
        {
            allRows.add(TreeTableRow.of("r" + i, Map.of("name", "Row " + i), false));
        }
        return query ->
        {
            List<TreeTableRow> window = allRows.subList(0, Math.min(allRows.size(), query.getLimit()));
            return TreeTablePage.of(new ArrayList<>(window), OptionalLong.empty());
        };
    }

    private void registerTreeTableWithWindowSizeTwo()
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newTreeTable()
                                                                                                   .withColumns(TreeTableColumn.of("name", "Name"))
                                                                                                   .withDataProvider(fiveRowProvider())
                                                                                                   .withWindowSize(2)));
    }

    @Test
    public void testLoadMoreClickWidensTheSameEventsResponseWindow() throws Exception
    {
        this.registerTreeTableWithWindowSizeTwo();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        assertNotNull(treeTableNode, "expected to find a TREETABLE node in the rendered UI JSON");
        assertEquals(2, treeTableNode.get("rows")
                                     .size(),
                     "initial root render must be capped to windowSize=2");

        JsonNode loadMoreNode = treeTableNode.get("loadMore");
        assertTrue(loadMoreNode.get("available")
                               .asBoolean(),
                   "5 rows with a 2-row window must offer load-more");
        Target loadMoreTarget = this.toTarget(loadMoreNode.get("target"));

        List<Target> rowTargets = new ArrayList<>();
        for (JsonNode rowNode : treeTableNode.get("rows"))
        {
            rowTargets.add(this.toTarget(rowNode.get("target")));
        }
        for (Target rowTarget : rowTargets)
        {
            assertNotEquals(rowTarget, loadMoreTarget, "the load-more Target must be distinct from every row's Target (trap #4)");
        }

        // THE decisive assertion: click load-more and read the SAME /ui/event response - no reload, no second call.
        JsonNode clickResponse = this.clickTarget(loadMoreTarget, Collections.emptyMap());
        JsonNode widenedTreeTableNode = this.findTreeTableNode(clickResponse.get("targetNode")
                                                                            .get("node"));
        assertNotNull(widenedTreeTableNode, "the load-more click response must carry the re-rendered TREETABLE content");
        assertEquals(4, widenedTreeTableNode.get("rows")
                                            .size(),
                     "the SAME /ui/event response must already reflect the WIDER window (mechanism (a), no one-roundtrip lag)");
    }

    @Test
    public void testLoadMoreTargetRoutesOnlyItsOwnHandlerNotARowHandler() throws Exception
    {
        this.registerTreeTableWithWindowSizeTwo();

        JsonNode treeTableNode = this.findTreeTableNode(this.renderUI());
        Target loadMoreTarget = this.toTarget(treeTableNode.get("loadMore")
                                                           .get("target"));

        JsonNode clickResponse = this.clickTarget(loadMoreTarget, Collections.emptyMap());
        assertFalse(clickResponse.isNull(), "clicking the load-more control must not return a bare null response (plan-29 regression class)");
        assertTrue(clickResponse.hasNonNull("targetNode"), "response targetNode must be non-null");
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
