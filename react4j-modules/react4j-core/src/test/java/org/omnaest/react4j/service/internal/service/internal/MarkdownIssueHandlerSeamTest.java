package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.i18n.UILocale;
import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.domain.markdown.MarkdownIssue.Type;
import org.omnaest.react4j.domain.markdown.MarkdownIssueCollector;
import org.omnaest.react4j.domain.markdown.MarkdownIssueHandler;
import org.omnaest.react4j.service.internal.service.MarkdownService;
import org.omnaest.react4j.service.internal.service.UIComponentFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Seam test between the markdown directive grammar of the {@link MarkdownServiceImpl} and the
 * {@link org.omnaest.react4j.service.internal.service.MarkdownDirectiveResolver} behind it: drives real markdown through the real spring service graph and
 * asserts that a directive the interpreter cannot make sense of reaches the {@link MarkdownIssueHandler} instead of being swallowed.
 *
 * @see MarkdownIssueHandler
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownIssueHandlerSeamTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
public class MarkdownIssueHandlerSeamTest
{
    private static final String       SOURCE = "discord";

    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    @Autowired
    private MarkdownService           markdownService;

    @Autowired
    private UIComponentFactoryService uiComponentFactoryService;

    @Test
    public void testUnknownButtonStyleIsReported() throws Exception
    {
        MarkdownIssueCollector collector = this.interpret("[BUTTON:BOGUS:Join us!](http://link.example)");
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_BUTTON_STYLE, SOURCE, 1, "BUTTON:BOGUS:Join us!", "BOGUS")), collector.getIssues());
    }

    @Test
    public void testKnownButtonStyleIsNoIssue() throws Exception
    {
        // exactly the markup used within the productive content files, which must not produce any noise
        assertEquals(Collections.emptyList(), this.interpret("[BUTTON:SUCCESS:Discord server](https://discord.example)")
                                                  .getIssues());
    }

    @Test
    public void testButtonWithoutStyleIsNoIssue() throws Exception
    {
        assertEquals(Collections.emptyList(), this.interpret("[BUTTON:Join us!](mailto:join.us@example.org)")
                                                  .getIssues());
    }

    @Test
    public void testPlainLinkIsNoIssue() throws Exception
    {
        assertEquals(Collections.emptyList(), this.interpret("[Just a label](http://link.example)")
                                                  .getIssues());
    }

    @Test
    public void testLabelWithColonSwallowedAsStyleIsReported() throws Exception
    {
        // '[BUTTON:Contact:Us](link)' renders a button labelled 'Us', the 'Contact' token disappears from the label without a trace
        MarkdownIssueCollector collector = this.interpret("[BUTTON:Contact:Us](http://link.example)");
        assertEquals(Arrays.asList(Type.UNKNOWN_BUTTON_STYLE), this.issueTypes(collector));
        assertEquals("Contact", collector.getIssues()
                                         .get(0)
                                         .getValue());
    }

    @Test
    public void testUnknownIconIsReported() throws Exception
    {
        MarkdownIssueCollector collector = this.interpret("[ICON:TELESCOPE]Some text");
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_ICON, SOURCE, 1, "[ICON:TELESCOPE]", "TELESCOPE")), collector.getIssues());
    }

    @Test
    public void testKnownIconIsNoIssue() throws Exception
    {
        assertEquals(Collections.emptyList(), this.interpret("[ICON:MICROSCOPE]Some text")
                                                  .getIssues());
    }

    @Test
    public void testUnknownVideoRatioIsReported() throws Exception
    {
        MarkdownIssueCollector collector = this.interpret("[IFRAME:VIDEO_5x4:A title](http://video.example)");
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_VIDEO_RATIO, SOURCE, 1, "IFRAME:VIDEO_5x4:A title", "_5x4")), collector.getIssues());
    }

    @Test
    public void testKnownVideoRatioIsNoIssue() throws Exception
    {
        assertEquals(Collections.emptyList(), this.interpret("[IFRAME:VIDEO_4x3:A title](http://video.example)")
                                                  .getIssues());
    }

    @Test
    public void testIssueNamesTheLineOfTheDirective() throws Exception
    {
        String markdown = "# Some title\n" + "\n" + "A first paragraph.\n" + "\n" + "[BUTTON:BOGUS:Join us!](http://link.example)\n";
        MarkdownIssueCollector collector = this.interpret(markdown);
        MarkdownIssue issue = collector.getIssues()
                                       .get(0);
        assertEquals(Optional.of(5), issue.getLine());
        assertTrue(issue.getMessage()
                        .contains(SOURCE + ":5"),
                   issue.getMessage());
    }

    @Test
    public void testIssueOfIconNamesTheLineOfTheDirective() throws Exception
    {
        MarkdownIssueCollector collector = this.interpret("Intro paragraph.\n\n[ICON:TELESCOPE]Some text\n");
        assertEquals(Optional.of(3), collector.getIssues()
                                              .get(0)
                                              .getLine());
    }

    @Test
    public void testMarkdownIsStillRenderedDespiteTheIssue() throws Exception
    {
        // the fallback is the point: the page renders, the issue only makes the fallback visible
        MarkdownIssueCollector collector = MarkdownIssueHandler.collecting();
        List<UIComponent<?>> components = this.newInterpreter(collector)
                                              .parseMarkdownElements("[BUTTON:BOGUS:Join us!](http://link.example)");
        assertFalse(components.isEmpty());
        assertTrue(collector.hasIssues());
    }

    @Test
    public void testIssueOfMarkdownWithoutSource() throws Exception
    {
        MarkdownIssueCollector collector = MarkdownIssueHandler.collecting();
        this.markdownService.interpreterWith(this.newUIComponentFactory())
                            .withIssueHandler(collector)
                            .parseMarkdownElements("[BUTTON:BOGUS:Join us!](http://link.example)");
        assertFalse(collector.getIssues()
                             .get(0)
                             .getSource()
                             .isPresent());
    }

    private MarkdownIssueCollector interpret(String markdown)
    {
        MarkdownIssueCollector collector = MarkdownIssueHandler.collecting();
        this.newInterpreter(collector)
            .parseMarkdownElements(markdown);
        return collector;
    }

    private MarkdownService.FactoryLoadedMarkdownInterpreter newInterpreter(MarkdownIssueCollector collector)
    {
        return this.markdownService.interpreterWith(this.newUIComponentFactory())
                                   .withSource(SOURCE)
                                   .withIssueHandler(collector);
    }

    private org.omnaest.react4j.domain.UIComponentFactory newUIComponentFactory()
    {
        return this.uiComponentFactoryService.newInstanceFor(UILocale.of(Locale.ENGLISH));
    }

    private List<Type> issueTypes(MarkdownIssueCollector collector)
    {
        return collector.stream()
                        .map(MarkdownIssue::getType)
                        .collect(Collectors.toList());
    }

}
