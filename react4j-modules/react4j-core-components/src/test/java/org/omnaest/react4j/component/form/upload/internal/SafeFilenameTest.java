package org.omnaest.react4j.component.form.upload.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SafeFilenameTest
{

    @Test
    public void testTraversalIsReducedToLeaf()
    {
        assertEquals("passwd", SafeFilename.sanitize("../../etc/passwd"));
    }

    @Test
    public void testMixedSeparatorsReducedToLeaf()
    {
        assertEquals("c", SafeFilename.sanitize("a/b\\c"));
    }

    @Test
    public void testLeadingSlashReducedToLeaf()
    {
        assertEquals("file.txt", SafeFilename.sanitize("/absolute/path/file.txt"));
    }

    @Test
    public void testEmptyFallsBackToDefault()
    {
        assertEquals("upload", SafeFilename.sanitize(""));
    }

    @Test
    public void testNullFallsBackToDefault()
    {
        assertEquals("upload", SafeFilename.sanitize(null));
    }

    @Test
    public void testDotDotAloneFallsBackToDefault()
    {
        assertEquals("upload", SafeFilename.sanitize(".."));
    }

    @Test
    public void testSanitizedNameNeverContainsSeparator()
    {
        assertFalse(SafeFilename.sanitize("../../etc/passwd")
                                .contains("/"));
        assertFalse(SafeFilename.sanitize("a/b\\c")
                                .contains("\\"));
    }

    @Test
    public void testResolveWithinDirectoryStaysUnderDirectory(@TempDir Path tempDir)
    {
        Path resolved = SafeFilename.resolveWithinDirectory(tempDir, "report.csv");
        assertTrue(resolved.startsWith(tempDir));
        assertEquals("report.csv", resolved.getFileName()
                                           .toString());
    }

    @Test
    public void testResolveWithinDirectoryRejectsTraversalEscape(@TempDir Path tempDir)
    {
        // even a maliciously crafted absolute path collapses to a safe leaf by the time it is resolved,
        // so it can never escape tempDir
        Path resolved = SafeFilename.resolveWithinDirectory(tempDir, "../../etc/passwd");
        assertTrue(resolved.startsWith(tempDir));
    }

}
