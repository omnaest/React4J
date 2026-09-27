package org.omnaest.react4j.service.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.domain.DropTarget.DropEvent;
import org.omnaest.react4j.domain.DropTarget.DropRelation;
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
 * Real {@code POST /ui/event} walking-skeleton proof for {@code Draggable}/{@code DropTarget} (plan-235 S1).
 * <p>
 * AC-S1-1 proves a registered drop handler receives the exact {@code dragId}/{@code relation} the client
 * sent, reading them out of the {@code dragIdFieldKey}/{@code relationFieldKey} the server generated -
 * asserting an observable captured side-effect ({@link AtomicReference}), never response-non-null.
 * <p>
 * AC-S1-2 is the highest-risk criterion: two SIBLING {@code DropTarget}s must compute DISTINCT
 * {@code Target}s, and a drop routed to one must fire ONLY that one's handler.
 *
 * @see org.omnaest.react4j.service.internal.component.DropTargetImpl
 */
@SpringBootTest(classes = DropTargetEndToEndTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class DropTargetEndToEndTest
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

    @Test
    public void testARegisteredDropHandlerReceivesTheExactDragIdAndRelationTheClientSent() throws Exception
    {
        AtomicReference<DropEvent> captured = new AtomicReference<>();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newDropTarget()
                                                                                                   .withContent(factory.newParagraph()
                                                                                                                       .addText("drop here"))
                                                                                                   .onDrop(captured::set)));

        JsonNode dropTargetNode = this.findFirstDropTargetNode(this.renderUI());
        String dragIdFieldKey = dropTargetNode.get("dragIdFieldKey")
                                              .asText();
        String relationFieldKey = dropTargetNode.get("relationFieldKey")
                                                .asText();
        Target target = this.toTarget(dropTargetNode.get("dropTarget"));
        assertFalse(target.isEmpty(), "precondition: a DropTarget with onDrop configured must emit a routable target");

        Map<String, Object> data = new HashMap<>();
        data.put(dragIdFieldKey, "card-1");
        data.put(relationFieldKey, "AFTER");
        this.postEvent(target, data);

        assertEquals("card-1", captured.get()
                                       .getDragId());
        assertEquals(DropRelation.AFTER, captured.get()
                                                 .getRelation());
    }

    @Test
    public void testTwoSiblingDropTargetsComputeDistinctTargetsAndADropRoutesToOnlyOne() throws Exception
    {
        AtomicReference<DropEvent> firstCaptured = new AtomicReference<>();
        AtomicReference<DropEvent> secondCaptured = new AtomicReference<>();
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newComposite()
                                                                                                   .addNewComponent(f2 -> f2.newDropTarget()
                                                                                                                            .withContent(f2.newParagraph()
                                                                                                                                           .addText("first"))
                                                                                                                            .onDrop(firstCaptured::set))
                                                                                                   .addNewComponent(f2 -> f2.newDropTarget()
                                                                                                                            .withContent(f2.newParagraph()
                                                                                                                                           .addText("second"))
                                                                                                                            .onDrop(secondCaptured::set))));

        List<JsonNode> dropTargetNodes = this.findAllDropTargetNodes(this.renderUI());
        assertEquals(2, dropTargetNodes.size(), "precondition: both sibling DropTargets must be present in the rendered page");

        Target firstTarget = this.toTarget(dropTargetNodes.get(0)
                                                          .get("dropTarget"));
        Target secondTarget = this.toTarget(dropTargetNodes.get(1)
                                                           .get("dropTarget"));
        assertFalse(firstTarget.isEmpty());
        assertFalse(secondTarget.isEmpty());
        assertNotEquals(firstTarget, secondTarget, "two sibling DropTargets must not collide onto the same Target");

        String secondDragIdFieldKey = dropTargetNodes.get(1)
                                                     .get("dragIdFieldKey")
                                                     .asText();
        String secondRelationFieldKey = dropTargetNodes.get(1)
                                                       .get("relationFieldKey")
                                                       .asText();
        Map<String, Object> data = new HashMap<>();
        data.put(secondDragIdFieldKey, "card-2");
        data.put(secondRelationFieldKey, "INTO");
        this.postEvent(secondTarget, data);

        assertNull(firstCaptured.get(), "a drop routed to the SECOND target must not fire the FIRST one's handler");
        assertEquals("card-2", secondCaptured.get()
                                             .getDragId());
        assertEquals(DropRelation.INTO, secondCaptured.get()
                                                      .getRelation());
    }

    @Test
    public void testADropTargetWithNoOnDropRegistersNoHandlerAndEmitsAnEmptyTarget() throws Exception
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newDropTarget()
                                                                                                   .withContent(factory.newParagraph()
                                                                                                                       .addText("no handler"))));

        JsonNode dropTargetNode = this.findFirstDropTargetNode(this.renderUI());
        assertTrue(this.toTarget(dropTargetNode.get("dropTarget")).isEmpty(), "no onDrop -> Target.empty(), never null");
        assertTrue(dropTargetNode.get("dragIdFieldKey")
                                 .isNull());
        assertTrue(dropTargetNode.get("relationFieldKey")
                                 .isNull());
    }

    private void postEvent(Target target, Map<String, Object> data) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext("", data, java.util.Collections.emptyMap()));
        String requestJson = OBJECT_MAPPER.writeValueAsString(eventBody);
        this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                              .content(requestJson))
                    .andExpect(status().isOk());
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

    private Target toTarget(JsonNode targetNode) throws Exception
    {
        return OBJECT_MAPPER.treeToValue(targetNode, Target.class);
    }

    private JsonNode findFirstDropTargetNode(JsonNode root)
    {
        List<JsonNode> found = this.findAllDropTargetNodes(root);
        assertFalse(found.isEmpty(), "expected the rendered page to contain a DROPTARGET node");
        return found.get(0);
    }

    private List<JsonNode> findAllDropTargetNodes(JsonNode root)
    {
        List<JsonNode> collected = new ArrayList<>();
        this.collectDropTargetNodes(root, collected);
        return collected;
    }

    private void collectDropTargetNodes(JsonNode node, List<JsonNode> collected)
    {
        if (node == null || node.isNull())
        {
            return;
        }
        if (node.isObject())
        {
            JsonNode type = node.get("type");
            if (type != null && "DROPTARGET".equals(type.asText()))
            {
                collected.add(node);
            }
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();)
            {
                this.collectDropTargetNodes(it.next()
                                              .getValue(),
                                            collected);
            }
        }
        else if (node.isArray())
        {
            node.forEach(child -> this.collectDropTargetNodes(child, collected));
        }
    }
}
