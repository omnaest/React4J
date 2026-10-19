package org.omnaest.react4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.exposureprobe.ProbeBase;
import org.omnaest.react4j.exposureprobe.ProbeExposing;
import org.omnaest.react4j.exposureprobe.ProbeNotExposing;
import org.omnaest.react4j.exposureprobe.internal.HiddenProbeType;

/**
 * plan-320 S8 (kanban card fd9f338f): a ratchet over this module's published surface. A public type outside any
 * {@code internal} package must not mention a type from an {@code internal} package on a public or protected member
 * (parameter, return, generic argument, bound, public field, superclass, interface) unless the exposure is on the
 * allowlist below. The guideline gives an {@code internal} type no compatibility claim, so every such mention is a
 * published-API breach waiting to break a consumer on the next move of that type.
 * <p>
 * The allowlist is today's known exposures, keyed on the exposed internal type and each carrying a reason that names
 * the tracking card. It may only shrink: a new exposure fails naming the exposing member and the type, and an entry
 * that no longer matches any exposure fails as stale. {@code EventHandler} was the first entry removed, by moving it to
 * {@code org.omnaest.react4j.domain}.
 * <p>
 * Test-only infrastructure with no main-source counterpart, so it sits at the base package like
 * {@code GenericCapabilityVocabularyTest}.
 */
class PublicApiInternalTypeExposureTest
{
    private static final String              TRACKING_CARD     = "cdeb180d";

    /** Exposed internal type (fully qualified) to the reason its exposure is tolerated for now. */
    private static final Map<String, String> ALLOWED_EXPOSURES = allowedExposures();

    @Test
    void noPublicMemberExposesAnInternalTypeThatIsNotOnTheAllowlist() throws IOException, ClassNotFoundException
    {
        List<Class<?>> types = discoverTypes();
        assertFalse(types.isEmpty(), "sanity check: the discovery walk must find at least one type, or this test would vacuously pass");

        List<Exposure> exposures = findExposures(types);
        List<Exposure> unallowed = exposures.stream()
                                            .filter(exposure -> !ALLOWED_EXPOSURES.containsKey(exposure.exposedType()
                                                                                                       .getName()))
                                            .collect(Collectors.toList());

        assertTrue(unallowed.isEmpty(),
                   "Public API mentions internal types that are not on the allowlist (move the type out of 'internal', or if it truly must stay, add it to ALLOWED_EXPOSURES with a reason naming a tracking card):\n"
                                        + describe(unallowed));
    }

    @Test
    void everyAllowlistEntryStillMatchesAnExposure() throws IOException, ClassNotFoundException
    {
        Set<String> exposedTypeNames = findExposures(discoverTypes()).stream()
                                                                     .map(exposure -> exposure.exposedType()
                                                                                              .getName())
                                                                     .collect(Collectors.toCollection(TreeSet::new));

        List<String> stale = ALLOWED_EXPOSURES.keySet()
                                              .stream()
                                              .filter(allowedType -> !exposedTypeNames.contains(allowedType))
                                              .sorted()
                                              .collect(Collectors.toList());

        assertTrue(stale.isEmpty(), "Stale allowlist entries (no public member exposes them any more - delete them): " + stale);
    }

    @Test
    void everyAllowlistEntryCarriesAReasonNamingTheTrackingCard()
    {
        List<String> unjustified = ALLOWED_EXPOSURES.entrySet()
                                                    .stream()
                                                    .filter(entry -> entry.getValue()
                                                                          .isBlank()
                                                                     || !entry.getValue()
                                                                              .contains(TRACKING_CARD))
                                                    .map(Map.Entry::getKey)
                                                    .sorted()
                                                    .collect(Collectors.toList());

        assertTrue(unjustified.isEmpty(), "Allowlist entries without a non-blank reason naming card " + TRACKING_CARD + ": " + unjustified);
    }

    /**
     * Proves the detector itself can fail: one probe type that places a hidden type at every position the detector
     * claims to cover, and one that does not mention it at all.
     */
    @Test
    void theDetectorFiresForEveryPositionAHiddenTypeCanOccupyOnAPublicSignature() throws ClassNotFoundException
    {
        Class<?> nonPublicProbe = Class.forName("org.omnaest.react4j.exposureprobe.ProbeNonPublic");
        assertFalse(Modifier.isPublic(nonPublicProbe.getModifiers()), "fixture sanity: the probe that must stay unreported has to be non-public");

        Map<String, Set<String>> exposingMembersByHiddenType = new TreeMap<>();
        for (Exposure exposure : findExposures(List.of(ProbeExposing.class, ProbeBase.class, ProbeNotExposing.class, nonPublicProbe)))
        {
            exposingMembersByHiddenType.computeIfAbsent(exposure.exposedType()
                                                                .getSimpleName(),
                                                        key -> new TreeSet<>())
                                       .add(exposure.exposingMember());
        }

        assertEquals(Set.of(HiddenProbeType.class.getSimpleName()), exposingMembersByHiddenType.keySet(), "only the hidden probe type is internal");
        assertEquals(new TreeSet<>(List.of("ProbeExposing superclass", "ProbeExposing interface", "ProbeExposing<T> type parameter bound", "ProbeExposing#publicField",
                                           "ProbeExposing#<init>(HiddenProbeType)", "ProbeExposing#parameter(HiddenProbeType)", "ProbeExposing#returnType() returns HiddenProbeType",
                                           "ProbeExposing#genericArgument(List<HiddenProbeType>)", "ProbeExposing#wildcardBound(List<? extends HiddenProbeType>)",
                                           "ProbeExposing#arrayParameter(HiddenProbeType[])", "ProbeExposing#protectedMethod(HiddenProbeType)", "ProbeExposing#typeVariableBound(T)",
                                           "ProbeExposing#get() returns HiddenProbeType")),
                     exposingMembersByHiddenType.get(HiddenProbeType.class.getSimpleName()),
                     "every position must be reported, and nothing else (private members, package-private members and non-public types are not published surface)");
    }

    private static Map<String, String> allowedExposures()
    {
        Map<String, String> allowed = new LinkedHashMap<>();
        allowed.put("org.omnaest.react4j.service.internal.handler.domain.Target", reason("Target addresses the component a client event is for; on HandlerEmitter's emit methods"));
        allowed.put("org.omnaest.react4j.service.internal.handler.domain.DataEventHandler",
                    reason("DataEventHandler is the data-carrying twin of EventHandler; on EventHandlerRegistrationSupport and HandlerEmitter"));
        allowed.put("org.omnaest.react4j.service.internal.nodes.handler.Handler", reason("Handler is what HandlerEmitter's emit methods return"));
        allowed.put("org.omnaest.react4j.component.form.internal.renderer.node.element.FormElementNode", reason("FormElementNode is what FormElement#render returns"));
        allowed.put("org.omnaest.react4j.service.internal.nodes.context.UIContextDataNode", reason("UIContextDataNode is the parameter of the raw Node#setUiContextData"));
        allowed.put("org.omnaest.react4j.service.internal.component.ComponentContext", reason("ComponentContext is the parameter of CustomUIComponentFactory#newInstance"));
        allowed.put("org.omnaest.react4j.service.internal.service.LocalizedTextResolverService$LocationAwareTextResolver",
                    reason("LocationAwareTextResolver is the parameter of ValueSource#asNode"));
        allowed.put("org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue", reason("I18nTextValue is the parameter of NodeRenderingProcessor#render"));
        allowed.put("org.omnaest.react4j.component.form.upload.internal.AbstractUploadChannel", reason("AbstractUploadChannel is the superclass of ByteArrayChannel and FileChannel"));
        return allowed;
    }

    private static String reason(String text)
    {
        return text + " - known exposure, kanban card " + TRACKING_CARD + " tracks moving or hiding it";
    }

    private static List<Exposure> findExposures(Collection<Class<?>> types)
    {
        List<Exposure> exposures = new ArrayList<>();
        for (Class<?> type : types)
        {
            if (!isPublishedType(type))
            {
                continue;
            }

            collectTypeExposures(type, exposures);
            for (Field field : type.getDeclaredFields())
            {
                if (isPublishedMember(field.getModifiers(), type, field.isSynthetic()))
                {
                    report(exposures, type.getSimpleName() + "#" + field.getName(), field.getGenericType());
                }
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors())
            {
                if (isPublishedMember(constructor.getModifiers(), type, constructor.isSynthetic()))
                {
                    reportExecutable(exposures, type, constructor, "<init>", null);
                }
            }
            for (Method method : type.getDeclaredMethods())
            {
                if (isPublishedMember(method.getModifiers(), type, method.isSynthetic() || method.isBridge()))
                {
                    reportExecutable(exposures, type, method, method.getName(), method.getGenericReturnType());
                }
            }
        }
        return exposures.stream()
                        .distinct()
                        .sorted(Comparator.comparing((Exposure exposure) -> exposure.exposedType()
                                                                                    .getName())
                                          .thenComparing(Exposure::exposingMember))
                        .collect(Collectors.toList());
    }

    private static void collectTypeExposures(Class<?> type, List<Exposure> exposures)
    {
        if (type.getGenericSuperclass() != null)
        {
            report(exposures, type.getSimpleName() + " superclass", type.getGenericSuperclass());
        }
        for (Type genericInterface : type.getGenericInterfaces())
        {
            report(exposures, type.getSimpleName() + " interface", genericInterface);
        }
        for (TypeVariable<?> typeVariable : type.getTypeParameters())
        {
            report(exposures, type.getSimpleName() + "<" + typeVariable.getName() + "> type parameter bound", typeVariable.getBounds());
        }
    }

    private static void reportExecutable(List<Exposure> exposures, Class<?> type, Executable executable, String name, Type returnType)
    {
        String signature = type.getSimpleName() + "#" + name + "(" + Arrays.stream(executable.getGenericParameterTypes())
                                                                           .map(PublicApiInternalTypeExposureTest::simpleName)
                                                                           .collect(Collectors.joining(", "))
                           + ")" + (returnType != null && returnType != void.class ? " returns " + simpleName(returnType) : "");

        // the return type is a mention of the member as a whole, parameters likewise: all are reported under the same name
        if (returnType != null)
        {
            report(exposures, signature, returnType);
        }
        report(exposures, signature, executable.getGenericParameterTypes());
        for (TypeVariable<?> typeVariable : executable.getTypeParameters())
        {
            report(exposures, signature, typeVariable.getBounds());
        }
    }

    private static void report(List<Exposure> exposures, String exposingMember, Type... mentioningTypes)
    {
        Set<Class<?>> mentioned = new HashSet<>();
        Set<Type> visited = new HashSet<>();
        for (Type mentioningType : mentioningTypes)
        {
            collect(mentioningType, mentioned, visited);
        }
        for (Class<?> mentionedType : mentioned)
        {
            if (isInternal(mentionedType))
            {
                exposures.add(new Exposure(exposingMember, mentionedType));
            }
        }
    }

    private static void collect(Type type, Set<Class<?>> result, Set<Type> visited)
    {
        if (type == null || !visited.add(type))
        {
            return;
        }
        if (type instanceof Class<?> clazz)
        {
            if (clazz.isArray())
            {
                collect(clazz.getComponentType(), result, visited);
            }
            else
            {
                result.add(clazz);
            }
        }
        else if (type instanceof ParameterizedType parameterizedType)
        {
            collect(parameterizedType.getRawType(), result, visited);
            collect(parameterizedType.getOwnerType(), result, visited);
            for (Type argument : parameterizedType.getActualTypeArguments())
            {
                collect(argument, result, visited);
            }
        }
        else if (type instanceof GenericArrayType genericArrayType)
        {
            collect(genericArrayType.getGenericComponentType(), result, visited);
        }
        else if (type instanceof WildcardType wildcardType)
        {
            for (Type bound : wildcardType.getUpperBounds())
            {
                collect(bound, result, visited);
            }
            for (Type bound : wildcardType.getLowerBounds())
            {
                collect(bound, result, visited);
            }
        }
        else if (type instanceof TypeVariable<?> typeVariable)
        {
            for (Type bound : typeVariable.getBounds())
            {
                collect(bound, result, visited);
            }
        }
    }

    private static String simpleName(Type type)
    {
        if (type instanceof Class<?> clazz)
        {
            return clazz.getSimpleName();
        }
        if (type instanceof ParameterizedType parameterizedType)
        {
            return simpleName(parameterizedType.getRawType()) + Arrays.stream(parameterizedType.getActualTypeArguments())
                                                                      .map(PublicApiInternalTypeExposureTest::simpleName)
                                                                      .collect(Collectors.joining(", ", "<", ">"));
        }
        if (type instanceof WildcardType wildcardType)
        {
            Type[] upperBounds = wildcardType.getUpperBounds();
            return wildcardType.getLowerBounds().length > 0 ? "? super " + simpleName(wildcardType.getLowerBounds()[0])
                    : upperBounds.length > 0 && upperBounds[0] != Object.class ? "? extends " + simpleName(upperBounds[0]) : "?";
        }
        if (type instanceof GenericArrayType genericArrayType)
        {
            return simpleName(genericArrayType.getGenericComponentType()) + "[]";
        }
        return type.getTypeName();
    }

    /** A published type is public, outside every {@code internal} package, and so are all its enclosing types. */
    private static boolean isPublishedType(Class<?> type)
    {
        if (type.isAnonymousClass() || type.isLocalClass() || type.isSynthetic() || isInternal(type))
        {
            return false;
        }
        for (Class<?> current = type; current != null; current = current.getEnclosingClass())
        {
            if (!Modifier.isPublic(current.getModifiers()))
            {
                return false;
            }
        }
        return true;
    }

    /** Public members are published; protected members are too, but only where the class can be subclassed from outside. */
    private static boolean isPublishedMember(int modifiers, Class<?> owner, boolean synthetic)
    {
        if (synthetic)
        {
            return false;
        }
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) && !Modifier.isFinal(owner.getModifiers());
    }

    private static boolean isInternal(Class<?> type)
    {
        Package typePackage = type.getPackage();
        return typePackage != null && ("." + typePackage.getName() + ".").contains(".internal.");
    }

    private static String describe(List<Exposure> exposures)
    {
        Map<String, List<String>> membersByExposedType = new TreeMap<>();
        for (Exposure exposure : exposures)
        {
            membersByExposedType.computeIfAbsent(exposure.exposedType()
                                                         .getName(),
                                                 key -> new ArrayList<>())
                                .add(exposure.exposingMember());
        }
        return membersByExposedType.entrySet()
                                   .stream()
                                   .map(entry -> "  " + entry.getKey() + " is exposed by: " + String.join("; ", entry.getValue()))
                                   .collect(Collectors.joining("\n"));
    }

    /**
     * Walks the compiled {@code .class} files under {@code target/classes/org/omnaest/react4j} (Surefire's working
     * directory is the module root) and loads every type reflectively without initialising it.
     */
    private List<Class<?>> discoverTypes() throws IOException, ClassNotFoundException
    {
        Path classesRoot = Paths.get("target", "classes");
        Path react4jRoot = classesRoot.resolve(Paths.get("org", "omnaest", "react4j"));
        assertTrue(Files.isDirectory(react4jRoot), "expected compiled classes directory to exist: " + react4jRoot.toAbsolutePath());

        List<Class<?>> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(react4jRoot))
        {
            List<Path> classFiles = walk.filter(path -> path.toString()
                                                            .endsWith(".class")
                                                        && !path.getFileName()
                                                                .toString()
                                                                .equals("package-info.class"))
                                        .collect(Collectors.toList());
            for (Path classFile : classFiles)
            {
                String className = classesRoot.relativize(classFile)
                                              .toString()
                                              .replace(File.separatorChar, '.')
                                              .replaceAll("\\.class$", "");
                result.add(Class.forName(className, false, getClass().getClassLoader()));
            }
        }
        return result;
    }

    private record Exposure(String exposingMember, Class<?> exposedType) {
    }

}
