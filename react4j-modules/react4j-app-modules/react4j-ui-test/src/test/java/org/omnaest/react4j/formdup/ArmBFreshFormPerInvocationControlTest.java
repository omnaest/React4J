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
 * plan-235 Slice S4, Arm B (the CONTROL) - diagnosis only, no fix authored here.
 * <p>
 * Mirrors {@link ArmACapturedFormAccumulationReproductionTest} exactly, except the {@code RerenderingContainer}
 * content closure builds a FRESH {@link Form} via {@code factory.newForm()} on EVERY invocation, which plan-235's
 * survey already established does NOT hit the accumulate-only {@code FormData.FormDataBuilder} hazard, because a
 * fresh {@code FormImpl} carries a fresh, empty builder every time.
 * <p>
 * <b>CORRECTED (plan-245 S0) - this class does NOT mirror {@code CardCreateForm}, and the claim that it did was
 * wrong.</b> The earlier wording here said this arm's shape was "the same shape {@code CardCreateForm} uses in
 * {@code KanbanBoardServer}" because {@code CardCreateForm} "builds a fresh {@code factory.newForm()} inside its
 * render method" - true only in the narrow sense that {@code CardCreateForm.render} is itself called once per
 * dialog build. The claim missed the actual structural shape: {@code CardCreateForm} builds a {@code Composite}
 * ONCE, OUTSIDE any closure, then calls {@code composite.withRerenderingUIContext((self, uiContext, data) -> {
 * ... ; self.addComponent(form); })} - a completely different code path from this arm's raw
 * {@code factory.newRerenderingContainer().withContent(...)} with a fresh {@code Form} built directly inside the
 * closure. That structural difference is exactly why this arm stayed GREEN while the real app was duplicating
 * forms 1-&gt;5: it never exercised {@code Composite.withRerenderingUIContext}, so it could never have hit the
 * {@code CompositeImpl.asTemplateProvider()} shared-list defect that plan-245 S0 found and fixed (a completely
 * different hazard from the {@code FormData.FormDataBuilder} one this arm's own javadoc above correctly rules
 * out). {@link ArmCOutsideBuiltCompositeAccumulationReproductionTest} is the arm that actually mirrors
 * {@code CardCreateForm}'s real shape - see its own javadoc for the full mechanism and the measured numbers.
 * <p>
 * Same submit-creates-an-entity probe as Arm A, for the SAME AC-S4-4 comparison.
 */
@SpringBootTest(classes = ArmBFreshFormPerInvocationControlTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ArmBFreshFormPerInvocationControlTest
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
    public void testFreshFormPerInvocationDoesNotAccumulateInputs() throws Exception
    {
        AtomicInteger providerInvocations = new AtomicInteger();
        List<String> createdEntities = new CopyOnWriteArrayList<>();

        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newRerenderingContainer()
                                                                                                   .enableStaticNodeRerendering()
                                                                                                   .withContent(f ->
                                                                                                   {
                                                                                                       providerInvocations.incrementAndGet();
                                                                                                       // Control: a BRAND NEW Form (and therefore a
                                                                                                       // brand new, empty FormData.FormDataBuilder) on
                                                                                                       // EVERY invocation - mirrors CardCreateForm.
                                                                                                       Form form = f.newForm();
                                                                                                       form.addInputField(input -> input.withLabel("Name:")
                                                                                                                                        .withPlaceholder("Enter your name"));
                                                                                                       form.addButton(button -> button.withText("Submit")
                                                                                                                                      .onClick((data, context) ->
                                                                                                                                      {
                                                                                                                                          createdEntities.add("created-entity-"
                                                                                                                                                              + createdEntities.size());
                                                                                                                                          return data;
                                                                                                                                      }));

                                                                                                       Composite composite = f.newComposite()
                                                                                                                              .addComponent(form);
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

        System.out.println("[plan-235 S4 Arm B] MEASURED provider invocations after initial GET /ui: " + invocationsAfterGet + "; rendered INPUT count: "
                           + inputsAfterGet);
        System.out.println("[plan-235 S4 Arm B] MEASURED provider invocations after ONE submit (cumulative): " + invocationsAfterSubmit
                           + "; rendered INPUT count in the SUBMIT's own response: " + inputsAfterSubmit);
        System.out.println("[plan-235 S4 Arm B] MEASURED createdEntities after ONE submit: " + createdEntities.size() + "; entity present in submit response: "
                           + entityInSubmitResponse + "; entity present after reload: " + entityInReload + "; INPUT count after reload: "
                           + inputsAfterReload);

        assertEquals(1, createdEntities.size(), "exactly one entity must have been created by the single submit click");
        assertEquals(1, inputsAfterGet, "the FRESH-form control must render exactly one input on the initial GET /ui too, regardless of invocation count");
        assertEquals(1, inputsAfterSubmit, "the FRESH-form control must render exactly one input, never duplicated, regardless of invocation count");
        assertFalse(createdEntities.isEmpty());
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
