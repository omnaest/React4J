package org.omnaest.react4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * plan-154 W1 / AC-R1: the unbuffered upload transport's opt-in surface ({@code Form.FileUploadFormElement},
 * {@code FormFileUploadNode}) is a generic React4J capability, never a hook for one consuming application. See
 * {@code react4j-core}'s test of the same name for the full rationale and the model this is copied from
 * ({@code CommonsCrypto}'s {@code PublicApiSurfaceTest}).
 */
class GenericCapabilityVocabularyTest
{
    private static final List<String> FORBIDDEN_WORDS = List.of("vault", "securevault", "encrypt");

    @Test
    void reflectedTypeSurfaceCarriesNoApplicationSpecificVocabulary() throws IOException, ClassNotFoundException
    {
        List<Class<?>> types = discoverTypes();
        assertFalse(types.isEmpty(), "sanity check: the discovery walk must find at least one type, or this test would vacuously pass");

        List<String> violations = new ArrayList<>();
        for (Class<?> type : types)
        {
            checkIdentifier(type.getSimpleName(), "type " + type.getName(), violations);

            for (Field field : type.getDeclaredFields())
            {
                checkIdentifier(field.getName(), "field " + type.getName() + "." + field.getName(), violations);
            }

            for (Method method : type.getDeclaredMethods())
            {
                String location = "method " + type.getName() + "." + method.getName();
                checkIdentifier(method.getName(), location, violations);
                checkIdentifier(method.getReturnType()
                                      .getSimpleName(),
                                location + " return type", violations);
                for (Class<?> parameterType : method.getParameterTypes())
                {
                    checkIdentifier(parameterType.getSimpleName(), location + " parameter type", violations);
                }
            }

            for (Constructor<?> constructor : type.getDeclaredConstructors())
            {
                for (Class<?> parameterType : constructor.getParameterTypes())
                {
                    checkIdentifier(parameterType.getSimpleName(), "constructor " + type.getName() + " parameter type", violations);
                }
            }
        }

        assertTrue(violations.isEmpty(), "Application-specific vocabulary found in the reflected type surface: " + violations);
    }

    private void checkIdentifier(String identifier, String location, List<String> violations)
    {
        String lower = identifier.toLowerCase(Locale.ROOT);
        for (String forbiddenWord : FORBIDDEN_WORDS)
        {
            if (lower.contains(forbiddenWord))
            {
                violations.add(location + " ('" + identifier + "' contains '" + forbiddenWord + "')");
            }
        }
    }

    /**
     * Walks the compiled {@code .class} files under {@code target/classes/org/omnaest/react4j} (Surefire's working
     * directory is the module root) and loads every type reflectively, public or not.
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
                result.add(Class.forName(className));
            }
        }
        return result;
    }
}
