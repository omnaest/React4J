/*******************************************************************************
 * Copyright 2021 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.react4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

/**
 * Classpath guard: fails whenever any class of the package {@code org.apache.ignite} or
 * {@code org.omnaest.react4j.data.datagrid} is loadable on the test classpath (a class file present on the
 * classpath is loadable).
 * <p>
 * The scan set is derived from the runtime classpath ({@code java.class.path}) plus the {@code Class-Path}
 * manifest attributes of the collected jars - never from a hardcoded list of classpath entries. Surefire forks the
 * test JVM with a manifest-only booter jar, in which case the real classpath is reachable only through that
 * manifest, hence the expansion.
 * <p>
 * The scan is validated by a denominator and two positive controls before it may report clean, so the guard cannot
 * pass vacuously: it must scan at least one class file, it must locate its own class file (proving directory
 * scanning and the path-to-classname conversion), and it must see at least one {@code org.junit.jupiter} class
 * (proving jar scanning).
 * <p>
 * Escape hatch: set the system property {@code react4j.skipClasspathGuard} to a <b>non-blank</b> stated reason
 * (e.g. {@code -Dreact4j.skipClasspathGuard=ignite needed for partitioning}) to skip this test. An absent or blank
 * value does <b>not</b> skip - the guard requires an explicit, stated reason.
 */
public class ClasspathGuardTest
{
    private static final String SKIP_PROPERTY = "react4j.skipClasspathGuard";

    /** The packages that must NOT be present on the test classpath. */
    private static final List<String> DISALLOWED_PACKAGES = List.of(
            "org.apache.ignite",
            "org.omnaest.react4j.data.datagrid"
    );

    /** Positive control for jar scanning: always on the classpath while a JUnit 5 test runs. */
    private static final String JUNIT_CONTROL_PREFIX = "org.junit.jupiter.";

    @Test
    public void testNoDisallowedPackageIsLoadableOnTestClasspath()
    {
        /* Escape hatch: skipping requires an explicit, non-blank stated reason. A blank or absent value does NOT
         * skip - the assumption below holds (test continues) in exactly those two cases and aborts only when a
         * non-blank reason was stated, recording that reason in the surefire report. */
        String skipReason = System.getProperty(SKIP_PROPERTY);
        assumeTrue(skipReason == null || skipReason.isBlank(),
                   "Skipped by explicit non-blank stated reason: '" + skipReason + "'");

        ScanResult scan = scanClasspath();

        /* The absence claim is only trustworthy if the scan proved it actually saw the classpath. */
        if (scan.scannedClassCount == 0)
        {
            fail("Classpath guard scanned no class file at all - the scan is unvalidated and must not pass. "
                 + "Raw java.class.path: [" + System.getProperty("java.class.path") + "]");
        }
        if (!scan.foundOwnTestClass)
        {
            fail("Classpath guard could not locate its own class " + ClasspathGuardTest.class.getName()
                 + " among the scanned entries - directory scanning and/or the path-to-classname conversion is "
                 + "broken; the guard is unvalidated and must not pass.");
        }
        if (!scan.foundJUnitClass)
        {
            fail("Classpath guard saw no classes under " + JUNIT_CONTROL_PREFIX
                 + " - jar scanning appears broken; the guard is unvalidated and must not pass.");
        }

        if (!scan.foundDisallowedClasses.isEmpty())
        {
            fail("Disallowed package(s) found on the test classpath: " + scan.foundDisallowedClasses
                 + ". These classes would be loadable by the application under test. Remove the offending "
                 + "dependency, or skip this guard with a non-blank stated reason via -D" + SKIP_PROPERTY
                 + "=<reason>.");
        }
    }

    @Test
    public void testDisallowedPackageDetectorRecognizesTheTargetPackages()
    {
        /* Red-direction probe: the same predicate that flags scan hits must recognise classes of the two target
         * packages (so a class made loadable in the future is caught), while sibling packages, the module's own
         * package and prefix look-alikes stay untouched. */
        assertTrue(isInDisallowedPackage("org.apache.ignite.Ignite"), "an Ignite class must be detected");
        assertTrue(isInDisallowedPackage("org.apache.ignite.internal.processors.cache.IgniteCacheProxy"), "a deep Ignite class must be detected");
        assertTrue(isInDisallowedPackage("org.omnaest.react4j.data.datagrid.DataGridRepositoryConfiguration"), "a datagrid class must be detected");
        assertTrue(isInDisallowedPackage("org.omnaest.react4j.data.datagrid.internal.DataGridRepositoryProvider"), "a deep datagrid class must be detected");
        assertFalse(isInDisallowedPackage("org.omnaest.react4j.data.inmemory.InMemoryRepositoryProvider"), "the in-memory sibling package must not be flagged");
        assertFalse(isInDisallowedPackage("org.omnaest.react4j.MockUI"), "the module's own package must not be flagged");
        assertFalse(isInDisallowedPackage("org.apache.ignited.Fake"), "a prefix look-alike must not be flagged");
        assertFalse(isInDisallowedPackage("org.omnaest.react4j.data.datagrid2.NotActual"), "a datagrid prefix look-alike must not be flagged");
    }

    // ---- scanning ----------------------------------------------------------

    private static ScanResult scanClasspath()
    {
        ScanResult result = new ScanResult();
        for (File entry : classpathEntries())
        {
            if (entry.isDirectory())
            {
                scanDirectory(entry, entry, result);
            }
            else if (entry.isFile() && entry.getName()
                                            .toLowerCase()
                                            .endsWith(".jar"))
            {
                scanJar(entry, result);
            }
        }
        return result;
    }

    /**
     * The classpath entries as the running JVM sees them: {@code java.class.path}, plus the {@code Class-Path}
     * manifest attributes of any collected jar (surefire can fork the test JVM with a manifest-only booter jar,
     * in which case the real test classpath is only reachable through that manifest).
     */
    private static List<File> classpathEntries()
    {
        Set<File> entries = new LinkedHashSet<>();
        String classPath = System.getProperty("java.class.path");
        if (classPath != null)
        {
            for (String entry : classPath.split(File.pathSeparator))
            {
                File file = new File(entry);
                if (file.exists())
                {
                    entries.add(file);
                }
            }
        }
        for (File jar : new ArrayList<>(entries))
        {
            if (jar.isFile() && jar.getName()
                                   .toLowerCase()
                                   .endsWith(".jar"))
            {
                addManifestClassPathEntries(jar, entries);
            }
        }
        return new ArrayList<>(entries);
    }

    private static void addManifestClassPathEntries(File jar, Set<File> entries)
    {
        try (JarFile jarFile = new JarFile(jar))
        {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null)
            {
                return;
            }
            String classPath = manifest.getMainAttributes()
                                       .getValue(Attributes.Name.CLASS_PATH);
            if (classPath == null)
            {
                return;
            }
            for (String token : classPath.split("\\s+"))
            {
                if (token.isBlank())
                {
                    continue;
                }
                File absolute = new File(token);
                if (absolute.isAbsolute())
                {
                    if (absolute.exists())
                    {
                        entries.add(absolute);
                    }
                }
                else
                {
                    /* Manifest Class-Path entries are resolved relative to the jar itself; some launchers
                     * resolve them against the working directory instead, so accept either. */
                    File viaJarParent = new File(jar.getParentFile(), token);
                    if (viaJarParent.exists())
                    {
                        entries.add(viaJarParent);
                    }
                    File viaWorkingDir = new File(System.getProperty("user.dir"), token);
                    if (viaWorkingDir.exists())
                    {
                        entries.add(viaWorkingDir);
                    }
                }
            }
        }
        catch (IOException e)
        {
            /* Unreadable jar: ignore its manifest, the entry itself is still scanned. */
        }
    }

    private static void scanDirectory(File root, File dir, ScanResult result)
    {
        File[] files = dir.listFiles();
        if (files == null)
        {
            return;
        }
        for (File file : files)
        {
            if (file.isDirectory())
            {
                scanDirectory(root, file, result);
            }
            else if (file.isFile() && file.getName()
                                          .endsWith(".class"))
            {
                String relativePath = file.getAbsolutePath()
                                          .substring(root.getAbsolutePath()
                                                         .length());
                /* Convert the filesystem path to a fully-qualified class name. */
                String className = relativePath.replace('\\', '.')
                                               .replace('/', '.')
                                               .replaceAll("^\\.+", "")
                                               .replaceAll("\\.class$", "");
                recordClass(result, className);
            }
        }
    }

    private static void scanJar(File jarFile, ScanResult result)
    {
        try (JarFile jar = new JarFile(jarFile))
        {
            jar.stream()
               .filter(entry -> entry.getName()
                                     .endsWith(".class"))
               .map(entry -> entry.getName()
                                  .replace('/', '.')
                                  .replace('\\', '.')
                                  .replaceAll("\\.class$", ""))
               .forEach(className -> recordClass(result, className));
        }
        catch (IOException e)
        {
            /* Unreadable jar: cannot contribute classes to the scan. */
        }
    }

    private static void recordClass(ScanResult result, String className)
    {
        result.scannedClassCount++;
        if (isInDisallowedPackage(className))
        {
            result.foundDisallowedClasses.add(className);
        }
        if (className.equals(ClasspathGuardTest.class.getName()))
        {
            result.foundOwnTestClass = true;
        }
        if (className.startsWith(JUNIT_CONTROL_PREFIX))
        {
            result.foundJUnitClass = true;
        }
    }

    private static boolean isInDisallowedPackage(String className)
    {
        for (String pkg : DISALLOWED_PACKAGES)
        {
            if (className.equals(pkg) || className.startsWith(pkg + "."))
            {
                return true;
            }
        }
        return false;
    }

    /** Mutable accumulator for one scan pass. */
    private static final class ScanResult
    {
        private int scannedClassCount = 0;
        private boolean foundOwnTestClass = false;
        private boolean foundJUnitClass = false;
        private final List<String> foundDisallowedClasses = new ArrayList<>();
    }
}