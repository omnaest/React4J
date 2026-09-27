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
import java.util.concurrent.atomic.AtomicReference;

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
 * plan-235 Slice S4, Arm A (the SUSPECTED shape) - diagnosis only, no fix authored here.
 * <p>
 * Reproduces, inside a fully isolated {@code react4j-ui-test} fixture, the framework hazard already LOCATED (but
 * not yet proven to be item 3's cause) by plan-235's survey: a {@code RerenderingContainer} content closure that
 * mutates a {@link Form} CAPTURED from an enclosing scope - built once, then reused across every re-invocation of
 * the closure - rather than constructing a fresh {@code Form} per invocation (that fresh-per-invocation shape is
 * Arm B, {@link ArmBFreshFormPerInvocationControlTest}, mirroring {@code CardCreateForm} in
 * {@code KanbanBoardServer}). {@code Form.FormImpl#data} is a Lombok {@code @Singular} ACCUMULATE-ONLY
 * {@code FormData.FormDataBuilder} - nothing ever clears it - so every additional invocation of a closure that
 * calls {@code form.addInputField(...)} on the SAME captured instance appends one more input element to the SAME
 * list.
 * <p>
 * The submit {@link Form.ButtonFormElement} is added EXACTLY ONCE, guarded by a null-check, BEFORE any input is
 * ever added - its {@code index} (and therefore its rendered {@code Location}/{@code Target}) is fixed at that
 * one construction, so it stays a stable, deterministic click target regardless of how many inputs accumulate
 * afterwards. This isolates the measured phenomenon to input-element accumulation, which is exactly what the
 * reported defect describes ("a form submit renders its input five-fold").
 * <p>
 * The content closure ALSO renders, in the SAME subtree, a paragraph per entry in a shared
 * {@code createdEntities} list that the submit button's handler appends to - this probes the SECOND half of the
 * reported symptom (plan-235 S4 AC-S4-4): whether a newly-created entity is present in the SUBMIT'S OWN response
 * or only after a reload.
 * <p>
 * <b>Constraint discipline (plan-235 S4).</b> This class is purely additive to {@code react4j-ui-test}; it does
 * not modify {@code react4j-core}, {@code react4j-core-components}, {@code react4j-core-ui} or
 * {@code KanbanBoardServer}. It authors NO fix - only measurement and a verdict, per the brief.
 */
@SpringBootTest(classes = ArmACapturedFormAccumulationReproductionTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ArmACapturedFormAccumulationReproductionTest
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
     * Measures, rather than infers: the provider-invocation count (a counter incremented at the very start of the
     * content closure) and the observed count of rendered {@code "type":"INPUT"} elements, both BEFORE any submit
     * (after the initial {@code GET /ui}) and AFTER exactly one real {@code POST /ui/event} submit driven through
     * the real Spring service graph. Also measures whether the entity the submit's handler created is present in
     * that same submit's response, and whether it is present after a subsequent reload.
     */
    @Test
    public void testCapturedFormAccumulatesInputsAcrossProviderInvocations() throws Exception
    {
        AtomicInteger providerInvocations = new AtomicInteger();
        AtomicReference<Form> capturedForm = new AtomicReference<>();
        List<String> createdEntities = new CopyOnWriteArrayList<>();

        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newRerenderingContainer()
                                                                                                   .enableStaticNodeRerendering()
                                                                                                   .withContent(f ->
                                                                                                   {
                                                                                                       providerInvocations.incrementAndGet();
                                                                                                       if (capturedForm.get() == null)
                                                                                                       {
                                                                                                           // Built ONCE - captured from this enclosing
                                                                                                           // scope for every later invocation, rather
                                                                                                           // than constructed fresh each time (Arm B).
                                                                                                           Form form = f.newForm();
                                                                                                           form.addButton(button -> button.withText("Submit")
                                                                                                                                          .onClick((data, context) ->
                                                                                                                                          {
                                                                                                                                              createdEntities.add("created-entity-"
                                                                                                                                                                  + createdEntities.size());
                                                                                                                                              return data;
                                                                                                                                          }));
                                                                                                           capturedForm.set(form);
                                                                                                       }
                                                                                                       // The suspected defect: every invocation of this
                                                                                                       // closure appends ONE MORE input to the SAME
                                                                                                       // captured Form's accumulate-only element list.
                                                                                                       capturedForm.get()
                                                                                                                   .addInputField(input -> input.withLabel("Name:")
                                                                                                                                                .withPlaceholder("Enter your name"));

                                                                                                       Composite composite = f.newComposite()
                                                                                                                              .addComponent(capturedForm.get());
                                                                                                       createdEntities.forEach(entity -> composite.addComponent(f.newParagraph()
                                                                                                                                                                 .addText(entity)));
                                                                                                       return composite;
                                                                                                   })));

        String initialUiJson = this.renderUI();
        int inputsAfterGet = this.countOccurrences(initialUiJson, "\"type\":\"INPUT\"");
        int invocationsAfterGet = providerInvocations.get();

        Target submitButtonTarget = this.extractSoleOnClickTarget(initialUiJson);

        JsonNode submitResponse = this.clickButton(submitButtonTarget);
        String submitTargetNodeJson = submitResponse.get("targetNode")
                                                    .get("node")
                                                    .toString();
        int inputsAfterSubmit = this.countOccurrences(submitTargetNodeJson, "\"type\":\"INPUT\"");
        int invocationsAfterSubmit = providerInvocations.get();
        boolean entityInSubmitResponse = createdEntities.size() == 1 && submitTargetNodeJson.contains(createdEntities.get(0));

        String reloadedUiJson = this.renderUI();
        int inputsAfterReload = this.countOccurrences(reloadedUiJson, "\"type\":\"INPUT\"");
        boolean entityInReload = createdEntities.size() == 1 && reloadedUiJson.contains(createdEntities.get(0));

        System.out.println("[plan-235 S4 Arm A] MEASURED provider invocations after initial GET /ui: " + invocationsAfterGet + "; rendered INPUT count: "
                           + inputsAfterGet);
        System.out.println("[plan-235 S4 Arm A] MEASURED provider invocations after ONE submit (cumulative): " + invocationsAfterSubmit
                           + "; rendered INPUT count in the SUBMIT's own response: " + inputsAfterSubmit);
        System.out.println("[plan-235 S4 Arm A] MEASURED createdEntities after ONE submit: " + createdEntities.size() + "; entity present in submit response: "
                           + entityInSubmitResponse + "; entity present after reload: " + entityInReload + "; INPUT count after reload: "
                           + inputsAfterReload);

        assertEquals(1, createdEntities.size(), "exactly one entity must have been created by the single submit click");
        assertFalse(createdEntities.isEmpty());
        // The mechanism itself, pinned rather than merely printed: on the captured/shared Form, the rendered INPUT
        // count tracks the cumulative provider-invocation count 1:1, at BOTH measurement points - this is what
        // would break first if the accumulate-only-builder hazard stopped firing.
        assertEquals(invocationsAfterGet, inputsAfterGet, "initial GET /ui: rendered INPUT count must equal the cumulative provider invocation count");
        assertEquals(invocationsAfterSubmit, inputsAfterSubmit,
                     "after one submit: rendered INPUT count in the response must equal the cumulative provider invocation count");
        assertTrue(entityInSubmitResponse, "co-located in the SAME subtree as the form, the created entity must be present in the submit's own response");
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
     * {@code onClick} object - which is what lets it find a FORM button's {@code button.onClick.target} exactly
     * as it would a plain {@code Button}'s.
     */
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
