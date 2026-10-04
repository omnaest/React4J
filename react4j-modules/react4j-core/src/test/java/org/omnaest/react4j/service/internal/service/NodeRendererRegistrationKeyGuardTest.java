package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.component.table.internal.renderer.node.TableNode;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.node.NodeRenderType;
import org.omnaest.react4j.domain.rendering.node.NodeRenderer;
import org.omnaest.react4j.domain.rendering.node.NodeRendererRegistry;
import org.omnaest.react4j.service.internal.nodes.AbstractNode;
import org.omnaest.react4j.service.internal.service.NodeHierarchyStaticRenderer.NodeHierarchyRenderingProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * plan-277 F3 guard at the registration seam of {@link NodeHierarchyStaticRenderer}: a node renderer must be registered under its node's real type key.
 * <p>
 * The seam: {@code NodeHierarchyRenderingProcessorImpl#register} derives the key by instantiating the node class reflectively and reading
 * {@link Node#getType()}, and falls back to the key {@code ""} when the class cannot be instantiated (a Lombok {@code @Builder}-only node without a
 * no-arg constructor). Every node whose {@code getType()} is {@code ""} (for instance {@code TabsNode.ContentElement}) is then rendered by THAT renderer,
 * which casts it to its own node class: a page holding both a Table and Tabs failed with HTTP 500 ({@code ClassCastException}), which no existing test
 * could see because the completeness meta-test never enumerated a Table. Nothing in the log says the registration went wrong.
 * <p>
 * The test enumerates every component the real {@link UIComponentFactory} offers without arguments (by reflection, so a component added later is
 * covered without touching this test), lets each real renderer register itself, and then asserts, through the REAL {@code register} method and its
 * own introspection hook ({@link NodeHierarchyRenderingProcessor#registeredNodeTypes}), that every registration yields a non-empty key and that no
 * two registrations collapse onto one key.
 */
@SpringBootTest(classes = NodeRendererRegistrationKeyGuardTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
public class NodeRendererRegistrationKeyGuardTest
{
    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    @Autowired
    private UIComponentFactoryService   uiComponentFactoryService;

    @Autowired
    private NodeHierarchyStaticRenderer nodeHierarchyStaticRenderer;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testEveryRegisteredNodeRendererResolvesToANonEmptyAndUniqueKey()
    {
        List<Registration> registrations = this.collectRegistrationsOfEveryComponentOfTheFactory();

        assertTrue(registrations.size() >= 30, "non-vacuity: the factory's components must register dozens of renderers, got " + registrations.size());
        assertTrue(registrations.stream()
                                .anyMatch(registration -> registration.nodeType == TableNode.class),
                   "non-vacuity: the Table, the component this guard was written for, must be among the registrations");

        Map<String, List<Registration>> keyToRegistrations = new LinkedHashMap<>();
        List<String> withoutRealKey = new ArrayList<>();
        for (Registration registration : registrations)
        {
            NodeHierarchyRenderingProcessor alone = this.nodeHierarchyStaticRenderer.newNodeRenderingProcessor();
            try
            {
                alone.register(registration.nodeType, registration.renderType, registration.renderer);
            }
            catch (IllegalArgumentException e)
            {
                // register fails loudly for a node class without a usable key; collected here so ONE run names every offender
                withoutRealKey.add(registration.describe() + " -> " + e.getMessage());
                continue;
            }
            Set<String> keys = alone.registeredNodeTypes(registration.renderType);
            if (keys.size() != 1 || keys.contains(""))
            {
                withoutRealKey.add(registration.describe() + " -> keys " + keys.stream().map(key -> "'" + key + "'").collect(java.util.stream.Collectors.toList()));
            }
            else
            {
                keyToRegistrations.computeIfAbsent(registration.renderType + ":" + keys.iterator()
                                                                                       .next(),
                                                   key -> new ArrayList<>())
                                  .add(registration);
            }
        }

        assertTrue(withoutRealKey.isEmpty(),
                   "node renderers registered under the empty key (they capture EVERY node whose type is empty): " + withoutRealKey
                                             + ". A node class needs a no-arg constructor (@NoArgsConstructor + @AllArgsConstructor beside @Builder).");
        // two components may legitimately register the SAME node class (the renderer is then identical); a collision is two DIFFERENT node classes on one key
        Map<String, List<String>> collisions = new LinkedHashMap<>();
        keyToRegistrations.forEach((key, owners) ->
        {
            if (owners.stream()
                      .map(owner -> owner.nodeType)
                      .distinct()
                      .count() > 1)
            {
                collisions.put(key, owners.stream()
                                          .map(Registration::describe)
                                          .collect(java.util.stream.Collectors.toList()));
            }
        });
        assertEquals(Map.of(), collisions, "two different node classes share one key, the later registration silently replaces the earlier");
    }

    /**
     * The seam fails loudly: a node class without a no-arg constructor is refused by the real {@code register}, naming the class, the render type and the
     * reason, instead of landing silently on the key {@code ""}
     */
    @Test
    public void testAnUninstantiableNodeClassIsRefusedWithAMessageNamingClassRenderTypeAndReason()
    {
        NodeHierarchyRenderingProcessor alone = this.nodeHierarchyStaticRenderer.newNodeRenderingProcessor();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> alone.register(NodeWithoutNoArgConstructor.class, NodeRenderType.HTML, (node, processor) -> ""));

        assertTrue(exception.getMessage()
                            .contains(NodeWithoutNoArgConstructor.class.getName()),
                   "the message must name the node class: " + exception.getMessage());
        assertTrue(exception.getMessage()
                            .contains(NodeRenderType.HTML.toString()),
                   "the message must name the render type: " + exception.getMessage());
        assertTrue(exception.getMessage()
                            .contains("no instantiable no-arg constructor"),
                   "the message must give the reason: " + exception.getMessage());
        assertEquals(Set.of(), alone.registeredNodeTypes(NodeRenderType.HTML), "a refused registration must leave nothing behind, least of all the empty key");
    }

    /**
     * A node class that can be instantiated but reports no type would register under {@code ""} just the same
     */
    @Test
    public void testANodeClassWithAnEmptyTypeIsRefusedWithAMessageNamingClassRenderTypeAndReason()
    {
        NodeHierarchyRenderingProcessor alone = this.nodeHierarchyStaticRenderer.newNodeRenderingProcessor();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                          () -> alone.register(NodeWithEmptyType.class, NodeRenderType.HTML, (node, processor) -> ""));

        assertTrue(exception.getMessage()
                            .contains(NodeWithEmptyType.class.getName()),
                   "the message must name the node class: " + exception.getMessage());
        assertTrue(exception.getMessage()
                            .contains(NodeRenderType.HTML.toString()),
                   "the message must name the render type: " + exception.getMessage());
        assertTrue(exception.getMessage()
                            .contains("empty string"),
                   "the message must give the reason: " + exception.getMessage());
        assertEquals(Set.of(), alone.registeredNodeTypes(NodeRenderType.HTML), "a refused registration must leave nothing behind");
    }

    /**
     * Control that the refusal is not over-eager: a well-formed node class registers under its real type
     */
    @Test
    public void testAWellFormedNodeClassStillRegistersUnderItsRealType()
    {
        NodeHierarchyRenderingProcessor alone = this.nodeHierarchyStaticRenderer.newNodeRenderingProcessor();

        alone.register(WellFormedNode.class, NodeRenderType.HTML, (node, processor) -> "");

        assertEquals(Set.of("WELLFORMED"), alone.registeredNodeTypes(NodeRenderType.HTML));
    }

    public static class NodeWithoutNoArgConstructor extends AbstractNode
    {
        public NodeWithoutNoArgConstructor(String ignored)
        {
        }

        @Override
        public String getType()
        {
            return "CONTROL";
        }
    }

    public static class NodeWithEmptyType extends AbstractNode
    {
        @Override
        public String getType()
        {
            return "";
        }
    }

    public static class WellFormedNode extends AbstractNode
    {
        @Override
        public String getType()
        {
            return "WELLFORMED";
        }
    }

    private List<Registration> collectRegistrationsOfEveryComponentOfTheFactory()
    {
        UIComponentFactory factory = this.uiComponentFactoryService.newInstanceFor(UILocale.of(Locale.ENGLISH));
        List<Registration> registrations = new ArrayList<>();
        Set<String> skipped = new TreeSet<>();
        for (Method method : UIComponentFactory.class.getMethods())
        {
            boolean isArgumentlessComponentFactoryMethod = method.getName()
                                                                 .startsWith("new")
                                                           && method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers());
            if (!isArgumentlessComponentFactoryMethod)
            {
                continue;
            }
            Object component = this.invoke(factory, method);
            if (component instanceof RenderableUIComponent)
            {
                ((RenderableUIComponent<?>) component).asRenderer()
                                                      .manageNodeRenderers(new NodeRendererRegistry() {
                                                          @Override
                                                          public <N extends Node> NodeRendererRegistry register(Class<N> nodeType, NodeRenderType renderType, NodeRenderer<N> nodeRenderer)
                                                          {
                                                              registrations.add(new Registration(method.getName(), nodeType, renderType, nodeRenderer));
                                                              return this;
                                                          }
                                                      });
            }
            else
            {
                skipped.add(method.getName());
            }
        }
        assertFalse(registrations.isEmpty());
        System.out.println("NodeRendererRegistrationKeyGuardTest: " + registrations.size() + " registrations, factory methods without a renderable component: " + skipped);
        return registrations;
    }

    private Object invoke(UIComponentFactory factory, Method method)
    {
        try
        {
            return method.invoke(factory);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("could not create the component of " + method.getName(), e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class Registration
    {
        private final String         factoryMethod;
        private final Class          nodeType;
        private final NodeRenderType renderType;
        private final NodeRenderer   renderer;

        Registration(String factoryMethod, Class nodeType, NodeRenderType renderType, NodeRenderer renderer)
        {
            this.factoryMethod = factoryMethod;
            this.nodeType = nodeType;
            this.renderType = renderType;
            this.renderer = renderer;
        }

        String describe()
        {
            return this.factoryMethod + " registers " + this.nodeType.getSimpleName() + " for " + this.renderType;
        }
    }
}
