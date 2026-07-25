package org.omnaest.react4j.component.form.upload.internal;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Sanitizes a client-supplied filename into a safe leaf name (no path separators, no traversal segments) and resolves it against a fixed target directory,
 * rejecting any result that would escape that directory.
 * <p>
 * Domain-free - imports neither Spring nor any React4J UI type.
 *
 * @author omnaest
 */
public class SafeFilename
{
    private static final String  FALLBACK_NAME      = "upload";
    private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]");
    private static final Pattern LEADING_DOTS       = Pattern.compile("^\\.+");

    private SafeFilename()
    {
        super();
    }

    /**
     * Reduces the given (potentially client-controlled) filename to a safe leaf name: strips any directory components (both {@code /} and {@code \}
     * separators), replaces any remaining unsafe character, and strips leading dots (which defeats {@code ..} traversal segments). Never returns null, empty,
     * or a name containing a path separator.
     *
     * @param filename
     * @return
     */
    public static String sanitize(String filename)
    {
        if (filename == null)
        {
            return FALLBACK_NAME;
        }

        String normalized = filename.replace('\\', '/');
        int lastSeparatorIndex = normalized.lastIndexOf('/');
        String leaf = lastSeparatorIndex >= 0 ? normalized.substring(lastSeparatorIndex + 1) : normalized;

        leaf = ILLEGAL_CHARACTERS.matcher(leaf)
                                 .replaceAll("_");
        leaf = LEADING_DOTS.matcher(leaf)
                           .replaceAll("");

        return leaf.isBlank() ? FALLBACK_NAME : leaf;
    }

    /**
     * Sanitizes the given filename and resolves it against {@code directory}, verifying that the resolved path stays under that directory. This is the sole
     * defense against a client dictating an arbitrary filesystem path via the upload filename.
     *
     * @param directory
     * @param filename
     * @return
     * @throws IllegalArgumentException
     *             if the resolved path would escape {@code directory}
     */
    public static Path resolveWithinDirectory(Path directory, String filename)
    {
        Path normalizedDirectory = directory.normalize();
        Path resolved = normalizedDirectory.resolve(sanitize(filename))
                                           .normalize();
        if (!resolved.startsWith(normalizedDirectory))
        {
            throw new IllegalArgumentException("Resolved upload path escapes the target directory: " + filename);
        }
        return resolved;
    }

}
