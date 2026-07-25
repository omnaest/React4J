package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.service.internal.service.ContentService;
import org.omnaest.react4j.service.internal.service.ContentService.ContentFile;
import org.omnaest.react4j.service.internal.service.internal.UIComponentFactoryServiceImpl.AbstractMarkdownComponentFactory;

/**
 * Pins down that the markdown component factory hands the content file identifier down as the origin of the markdown, which is what makes a
 * {@link MarkdownIssue} name the content file that has to be fixed.<br>
 * <br>
 * The {@link ContentService} is stubbed here since it reads from the file system, which is an external boundary of this context.
 *
 * @see AbstractMarkdownComponentFactory
 * @author omnaest
 */
public class MarkdownComponentFactorySourceTest
{
    private final AtomicReference<String> capturedMarkdown = new AtomicReference<>();
    private final AtomicReference<String> capturedSource   = new AtomicReference<>();

    @Test
    public void testFromContentFileDeclaresTheContentFileAsSource() throws Exception
    {
        this.newFactory("Some markdown of the file")
            .fromContentFile("demo_content");
        assertEquals("Some markdown of the file", this.capturedMarkdown.get());
        assertEquals("demo_content", this.capturedSource.get());
    }

    @Test
    public void testFromContentFileOfAbsentFileStillDeclaresTheSource() throws Exception
    {
        this.newFactory(null)
            .fromContentFile("missing_content");
        assertEquals("", this.capturedMarkdown.get());
        assertEquals("missing_content", this.capturedSource.get());
    }

    @Test
    public void testFromRawMarkdownHasNoSource() throws Exception
    {
        this.newFactory("unused")
            .from("Some raw markdown");
        assertEquals("Some raw markdown", this.capturedMarkdown.get());
        assertNull(this.capturedSource.get());
    }

    private AbstractMarkdownComponentFactory<String> newFactory(String contentFileMarkdown)
    {
        ContentService contentService = mock(ContentService.class);
        when(contentService.findContentMarkdownFile("demo_content")).thenReturn(Optional.ofNullable(contentFileMarkdown)
                                                                                        .map(markdown -> (ContentFile) () -> markdown));
        when(contentService.findContentMarkdownFile("missing_content")).thenReturn(Optional.empty());

        return new AbstractMarkdownComponentFactory<String>(contentService) {
            @Override
            protected String from(String markdown, String source)
            {
                MarkdownComponentFactorySourceTest.this.capturedMarkdown.set(markdown);
                MarkdownComponentFactorySourceTest.this.capturedSource.set(source);
                return markdown;
            }
        };
    }

}
