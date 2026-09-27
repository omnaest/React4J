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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.form.Form;
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
 * plan-245 S0, Arm C - the REAL shape, neither Arm A nor Arm B actually reproduces.
 * <p>
 * Arm B's own javadoc CLAIMS to mirror {@code CardCreateForm} in {@code KanbanBoardServer} and does not:
 * Arm B builds a brand new {@link Form} straight from {@code factory.newForm()} INSIDE the
 * {@code RerenderingContainer} content closure, on every invocation. {@code CardCreateForm} does something
 * structurally different - it builds a {@link Composite} ONCE, OUTSIDE any closure (at page-registration
 * time), then calls {@code composite.withRerenderingUIContext((self, uiContext, data) -> { ... ;
 * self.addComponent(form); })}. {@code AbstractUIComponent#withRerenderingUIContext}
 * (react4j-core-components) clones that outer {@code Composite} on every render pass via
 * {@code this.asTemplateProvider().get()} and hands the clone to the callback as {@code self} - so the
 * hazard this arm targets is specific to {@link Composite}'s own {@code asTemplateProvider()}
 * (react4j-core-components, {@code CompositeImpl}), not to the accumulate-only {@code Form.FormData}
 * builder Arm A/B exercise. This is why Arm B stayed green while the real app was broken: it never took the
 * {@code Composite.withRerenderingUIContext} path at all.
 * <p>
 * {@code CompositeImpl.asTemplateProvider()} (before the plan-245 S0 fix) returned
 * {@code () -> new CompositeImpl(this.context, this.components)} - a SECOND facade over the SAME mutable
 * {@code ArrayList}, not a copy. So {@code self.addComponent(form)} inside the closure appends to the ONE
 * shared list belonging to the ORIGINAL, page-registered {@code Composite}, and it accumulates across every
 * render pass of that same registered instance for the rest of the session - exactly {@code CardCreateForm}
 * and {@code CardDetailDialog#renderAddDiagramForm}/{@code #renderEditForm}'s shape (all three build a
 * {@code Composite} outside and call {@code composite.withRerenderingUIContext} with a
 * {@code self.addComponent(...)} callback; verified at source, plan-245 S0's own report).
 * <p>
 * Constraint discipline (plan-245 S0): purely additive to {@code react4j-ui-test}. The fix itself lives in
 * {@code react4j-core-components}'s {@code CompositeImpl}, not here.
 */
@SpringBootTest(classes = ArmCOutsideBuiltCompositeAccumulationReproductionTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ArmCOutsideBuiltCompositeAccumulationReproductionTest
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

    /**
     * Measures the rendered {@code "type":"INPUT"} count after the initial {@code GET /ui}, after ONE
     * {@code POST /ui/event} submit, and after a SECOND submit with no reload in between - the growth
     * signature that discriminates a one-off duplication from an accumulating one. plan-245 S0's brief
     * predicted 1, 5, 9 for this exact shape; this test reports the numbers it actually measures rather than
     * assuming them.
     */
    @Test
    public void testOutsideBuiltCompositeAccumulatesInputsAcrossRenderPasses() throws Exception
    {
        AtomicInteger providerInvocations = new AtomicInteger();
        List<String> createdEntities = new CopyOnWriteArrayList<>();

        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory ->
        {
            // Built ONCE, at page-registration time - OUTSIDE the withRerenderingUIContext closure. This is
            // the structural difference from Arm B (which builds fresh INSIDE) and is exactly
            // CardCreateForm's own shape.
            Composite composite = factory.newComposite();
            return composite.withRerenderingUIContext((self, uiContext, data) ->
            {
                providerInvocations.incrementAndGet();
                Form form = factory.newForm();
                form.addInputField(input -> input.withLabel("Name:")
                                                 .withPlaceholder("Enter your name"));
                form.addButton(button -> button.withText("Submit")
                                               .onClick((d, context) ->
                                               {
                                                   createdEntities.add("created-entity-" + createdEntities.size());
                                                   return d;
                                               }));
                // The real app's own shape: mutate the CLONE handed in as `self`, never a locally-built
                // fresh Composite.
                self.addComponent(form);
            });
        }));

        String initialUiJson = this.renderUI();
        int inputsAfterGet = this.countOccurrences(initialUiJson, "\"type\":\"INPUT\"");
        int invocationsAfterGet = providerInvocations.get();

        // Deliberately NOT asserting "exactly one" here before extracting - the pre-fix defect duplicates
        // the onClick handler too (one per accumulated Form), so a strict extraction would abort the test
        // before it can even measure the growth pattern. The FIRST target is used to drive every submit;
        // every duplicate Form's button closes over the SAME handler, so any one of them exercises the
        // real submit path identically.
        Target submitButtonTarget = this.extractFirstOnClickTarget(initialUiJson);

        JsonNode firstSubmitResponse = this.clickButton(submitButtonTarget);
        String firstSubmitTargetNodeJson = firstSubmitResponse.get("targetNode")
                                                              .get("node")
                                                              .toString();
        int inputsAfterFirstSubmit = this.countOccurrences(firstSubmitTargetNodeJson, "\"type\":\"INPUT\"");
        int invocationsAfterFirstSubmit = providerInvocations.get();

        JsonNode secondSubmitResponse = this.clickButton(submitButtonTarget);
        String secondSubmitTargetNodeJson = secondSubmitResponse.get("targetNode")
                                                                .get("node")
                                                                .toString();
        int inputsAfterSecondSubmit = this.countOccurrences(secondSubmitTargetNodeJson, "\"type\":\"INPUT\"");
        int invocationsAfterSecondSubmit = providerInvocations.get();

        System.out.println("[plan-245 S0 Arm C] MEASURED provider invocations after initial GET /ui: " + invocationsAfterGet + "; rendered INPUT count: "
                           + inputsAfterGet);
        System.out.println("[plan-245 S0 Arm C] MEASURED provider invocations after FIRST submit (cumulative): " + invocationsAfterFirstSubmit
                           + "; rendered INPUT count in the FIRST submit's own response: " + inputsAfterFirstSubmit);
        System.out.println("[plan-245 S0 Arm C] MEASURED provider invocations after SECOND submit (cumulative): " + invocationsAfterSecondSubmit
                           + "; rendered INPUT count in the SECOND submit's own response: " + inputsAfterSecondSubmit);
        System.out.println("[plan-245 S0 Arm C] MEASURED createdEntities after two submits: " + createdEntities.size());

        assertEquals(2, createdEntities.size(), "exactly two entities must have been created by the two submit clicks");
        assertFalse(createdEntities.isEmpty());
        assertTrue(inputsAfterGet >= 1, "the initial GET /ui must render at least one input");
        // The fixed-behaviour assertions - RED before the plan-245 S0 fix, GREEN after it. Quote the actual
        // RED numbers observed in the report rather than trusting this comment.
        assertEquals(1, inputsAfterGet, "GET /ui: exactly one INPUT, regardless of how many times the provider has been invoked");
        assertEquals(1, inputsAfterFirstSubmit, "after ONE submit: exactly one INPUT in the submit's own response - no duplication");
        assertEquals(1, inputsAfterSecondSubmit, "after a SECOND submit with no reload: still exactly one INPUT - no accumulation, not merely no duplication");
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

    /**
     * Mirrors {@code RerenderingSiblingButtonClickEndToEndTest#extractButtonTargets} (react4j-core): a generic
     * recursive walk collecting every {@code onClick.target}, regardless of which enclosing field holds the
     * {@code onClick} object. Returns the FIRST match rather than requiring exactly one - see the call site's
     * own comment for why an exactly-one requirement here would be self-defeating against the pre-fix defect.
     */
    private Target extractFirstOnClickTarget(String json) throws Exception
    {
        JsonNode root = OBJECT_MAPPER.readTree(json);
        List<JsonNode> onClickTargetNodes = new ArrayList<>();
        collectOnClickTargets(root, onClickTargetNodes);
        assertFalse(onClickTargetNodes.isEmpty(), "expected at least one rendered onClick handler (the form's submit button)");
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

    private int countOccurrences(String haystack, String needle)
    {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) != -1)
        {
            count++;
            index += needle.length();
        }
        return count;
    }
}
