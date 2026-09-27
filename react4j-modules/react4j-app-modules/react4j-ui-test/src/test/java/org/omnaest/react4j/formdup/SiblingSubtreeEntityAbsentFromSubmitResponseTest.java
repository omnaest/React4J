package org.omnaest.react4j.formdup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.data.annotations.EnableReactUIInMemoryRepository;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.security.WebSecurityConfiguration;
import org.omnaest.react4j.service.ReactUIService;
import org.omnaest.react4j.service.internal.handler.domain.DataWithContext;
import org.omnaest.react4j.service.internal.handler.domain.EventBody;
import org.omnaest.react4j.service.internal.handler.domain.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * plan-235 Slice S4 - supplementary probe for AC-S4-4 (the entity-absent-from-response half), NOT one of the
 * mandated Arm A / Arm B pair. Diagnosis only, no fix authored here.
 * <p>
 * {@link ArmACapturedFormAccumulationReproductionTest} and {@link ArmBFreshFormPerInvocationControlTest} both
 * co-locate the submit form AND the "created entities" list in ONE subtree (one {@code RerenderingContainer}), and
 * both measured the created entity as PRESENT in the submit's own response - a clean negative for the
 * "two-render-pass ordering" hypothesis (state mutated by the handler is NOT stale by the time the second render
 * pass runs, when the reader is in the SAME subtree as the writer).
 * <p>
 * This class tests the other structural candidate directly: the form and the entity list as TWO SIBLING
 * {@code RerenderingContainer}s under one {@code Composite}, exactly the shape {@code AC-BROWSER-5}
 * (plan-235 S3) already documents as significant - "a single {@code /ui/event} targets exactly one owning tree, so
 * otherwise the drop correctly persists while only one list visibly updates". A click resolves, via
 * {@code RerenderingServiceImpl#rerenderTargetNode}, to the NEAREST ancestor {@code Target} carrying a registered
 * {@code RerenderedNodeProvider} - which is the clicked button's OWN {@code RerenderingContainer}, never its
 * sibling. So the response's {@code targetNode} is structurally scoped to ONE subtree only.
 */
@SpringBootTest(classes = SiblingSubtreeEntityAbsentFromSubmitResponseTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class SiblingSubtreeEntityAbsentFromSubmitResponseTest
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc                   mockMvc;

    @Autowired
    private ReactUIService            reactUIService;

    @SpringBootApplication
    @EnableReactUI
    @EnableReactUIInMemoryRepository
    @Import(WebSecurityConfiguration.class)
    public static class TestApplication
    {
    }

    @Test
    public void testEntityCreatedBySiblingFormIsAbsentFromSubmitResponseButPresentAfterReload() throws Exception
    {
        List<String> createdEntities = new CopyOnWriteArrayList<>();

        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newComposite()
                                                                                                   .addComponent(factory.newRerenderingContainer()
                                                                                                                        .enableStaticNodeRerendering()
                                                                                                                        .withContent(f -> f.newForm()
                                                                                                                                           .addButton(button -> button.withText("Submit")
                                                                                                                                                                      .onClick((data, context) ->
                                                                                                                                                                      {
                                                                                                                                                                          createdEntities.add("created-entity-"
                                                                                                                                                                                              + createdEntities.size());
                                                                                                                                                                          return data;
                                                                                                                                                                      }))))
                                                                                                   .addComponent(factory.newRerenderingContainer()
                                                                                                                        .enableStaticNodeRerendering()
                                                                                                                        .withContent(f ->
                                                                                                                        {
                                                                                                                            Composite list = f.newComposite();
                                                                                                                            createdEntities.forEach(entity -> list.addComponent(f.newParagraph()
                                                                                                                                                                                 .addText(entity)));
                                                                                                                            return list;
                                                                                                                        }))));

        String initialUiJson = this.renderUI();
        Target submitButtonTarget = this.extractSoleOnClickTarget(initialUiJson);

        JsonNode submitResponse = this.clickButton(submitButtonTarget);
        String submitTargetNodeJson = submitResponse.get("targetNode")
                                                    .get("node")
                                                    .toString();
        assertEquals(1, createdEntities.size(), "exactly one entity must have been created by the single submit click");
        String entity = createdEntities.get(0);
        boolean entityInSubmitResponse = submitTargetNodeJson.contains(entity);

        String reloadedUiJson = this.renderUI();
        boolean entityInReload = reloadedUiJson.contains(entity);

        System.out.println("[plan-235 S4 sibling-subtree probe] entity created: " + entity);
        System.out.println("[plan-235 S4 sibling-subtree probe] entity present in the SUBMIT's own response: " + entityInSubmitResponse);
        System.out.println("[plan-235 S4 sibling-subtree probe] entity present after a GET /ui reload: " + entityInReload);
        System.out.println("[plan-235 S4 sibling-subtree probe] submit targetNode.node (the FORM's own subtree only): " + submitTargetNodeJson);

        assertFalse(entityInSubmitResponse,
                    "the entity, rendered only in the SIBLING RerenderingContainer's subtree, is structurally absent from a response scoped to the form's OWN subtree - this is the mechanism, not a defect");
        assertTrue(entityInReload, "a plain GET /ui re-renders BOTH sibling subtrees fresh, so the entity must be visible after a reload");
    }

    private String renderUI() throws Exception
    {
        return this.mockMvc.perform(get("/ui"))
                           .andExpect(status().isOk())
                           .andReturn()
                           .getResponse()
                           .getContentAsString();
    }

    private JsonNode clickButton(Target target) throws Exception
    {
        EventBody eventBody = new EventBody(target, new DataWithContext("test-context", Collections.emptyMap(), Collections.emptyMap()));
        String requestJson = OBJECT_MAPPER.writeValueAsString(eventBody);
        String responseJson = this.mockMvc.perform(post("/ui/event").contentType(MediaType.APPLICATION_JSON)
                                                                    .content(requestJson))
                                          .andExpect(status().isOk())
                                          .andReturn()
                                          .getResponse()
                                          .getContentAsString();
        return OBJECT_MAPPER.readTree(responseJson);
    }

    private Target extractSoleOnClickTarget(String json) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        List<JsonNode> onClickTargetNodes = new ArrayList<>();
        collectOnClickTargets(root, onClickTargetNodes);
        assertEquals(1, onClickTargetNodes.size(), "expected exactly one rendered onClick handler (the form's submit button)");
        return OBJECT_MAPPER.treeToValue(onClickTargetNodes.get(0), Target.class);
    }

    private static void collectOnClickTargets(JsonNode node, List<JsonNode> collector)
    {
        if (node == null)
        {
            return;
        }
        if (node.isObject())
        {
            if (node.has("onClick") && node.get("onClick")
                                           .isObject())
            {
                collector.add(node.get("onClick")
                                  .get("target"));
            }
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext())
            {
                collectOnClickTargets(node.get(fieldNames.next()), collector);
            }
        }
        else if (node.isArray())
        {
            for (JsonNode child : node)
            {
                collectOnClickTargets(child, collector);
            }
        }
    }
}
