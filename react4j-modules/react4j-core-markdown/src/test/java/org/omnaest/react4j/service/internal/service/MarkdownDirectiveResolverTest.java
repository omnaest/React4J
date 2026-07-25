package org.omnaest.react4j.service.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.RatioContainer.Ratio;
import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.domain.markdown.MarkdownIssue.Type;
import org.omnaest.react4j.domain.markdown.MarkdownIssueCollector;
import org.omnaest.react4j.domain.markdown.MarkdownIssueHandler;

/**
 * Unit test of the {@link MarkdownDirectiveResolver}, which is the spot where every markdown directive falls back to a default. Before the
 * {@link MarkdownIssueHandler} existed, a wrong token and an omitted token were indistinguishable from the outside, which is exactly what these tests pin down.
 *
 * @see MarkdownDirectiveResolver
 * @author omnaest
 */
public class MarkdownDirectiveResolverTest
{
    private static final String       SOURCE    = "discord";

    private final MarkdownIssueCollector collector = MarkdownIssueHandler.collecting();
    private final MarkdownDirectiveResolver resolver  = new MarkdownDirectiveResolver(this.collector, SOURCE);

    @Test
    public void testResolveButtonStyleOfKnownStyle() throws Exception
    {
        assertEquals(Style.SUCCESS, this.resolver.resolveButtonStyle("SUCCESS", "BUTTON:SUCCESS:Label"));
        assertEquals(Collections.emptyList(), this.collector.getIssues());
    }

    @Test
    public void testResolveButtonStyleOfOmittedStyle() throws Exception
    {
        // a button without an explicit style is the regular case and no issue at all
        assertEquals(Style.PRIMARY, this.resolver.resolveButtonStyle(null, "BUTTON:Label"));
        assertEquals(Style.PRIMARY, this.resolver.resolveButtonStyle("", "BUTTON:Label"));
        assertEquals(Collections.emptyList(), this.collector.getIssues());
    }

    @Test
    public void testResolveButtonStyleOfUnknownStyle() throws Exception
    {
        assertEquals(Style.PRIMARY, this.resolver.resolveButtonStyle("BOGUS", "BUTTON:BOGUS:Label"));
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_BUTTON_STYLE, SOURCE, "BUTTON:BOGUS:Label", "BOGUS")), this.collector.getIssues());
    }

    @Test
    public void testResolveButtonStyleIsCaseSensitive() throws Exception
    {
        // a lower case style does not resolve, which is the mistake most easily made and least visible in the rendered result
        assertEquals(Style.PRIMARY, this.resolver.resolveButtonStyle("success", "BUTTON:success:Label"));
        assertEquals(Arrays.asList(Type.UNKNOWN_BUTTON_STYLE), this.issueTypes());
    }

    @Test
    public void testResolveButtonStyleOfLabelTokenSwallowedAsStyle() throws Exception
    {
        // '[BUTTON:Contact:Us](link)' renders a button labelled 'Us' - the 'Contact' token is consumed as a style and silently dropped from the label
        assertEquals(Style.PRIMARY, this.resolver.resolveButtonStyle("Contact", "BUTTON:Contact:Us"));
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_BUTTON_STYLE, SOURCE, "BUTTON:Contact:Us", "Contact")), this.collector.getIssues());
    }

    @Test
    public void testResolveButtonText() throws Exception
    {
        assertEquals("Join us!", this.resolver.resolveButtonText("Join us!", "BUTTON:SUCCESS:Join us!"));
        assertEquals(Collections.emptyList(), this.collector.getIssues());
    }

    @Test
    public void testResolveButtonTextOfEmptyLabel() throws Exception
    {
        assertEquals("", this.resolver.resolveButtonText("", "BUTTON:SUCCESS:"));
        assertEquals(Arrays.asList(Type.EMPTY_BUTTON_TEXT), this.issueTypes());
    }

    @Test
    public void testResolveIconOfKnownIcon() throws Exception
    {
        assertEquals(Optional.of(StandardIcon.MICROSCOPE), this.resolver.resolveIcon("MICROSCOPE", "[ICON:MICROSCOPE]"));
        assertEquals(Collections.emptyList(), this.collector.getIssues());
    }

    @Test
    public void testResolveIconOfUnknownIcon() throws Exception
    {
        assertFalse(this.resolver.resolveIcon("TELESCOPE", "[ICON:TELESCOPE]")
                                 .isPresent());
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_ICON, SOURCE, "[ICON:TELESCOPE]", "TELESCOPE")), this.collector.getIssues());
    }

    @Test
    public void testResolveVideoRatioOfKnownRatio() throws Exception
    {
        assertEquals(Ratio._4x3, this.resolver.resolveVideoRatio("_4x3", "IFRAME:VIDEO_4x3:Title"));
        assertEquals(Collections.emptyList(), this.collector.getIssues());
    }

    @Test
    public void testResolveVideoRatioOfOmittedRatio() throws Exception
    {
        assertEquals(Ratio._16x9, this.resolver.resolveVideoRatio("", "IFRAME:VIDEO:Title"));
        assertEquals(Collections.emptyList(), this.collector.getIssues());
    }

    @Test
    public void testResolveVideoRatioOfUnknownRatio() throws Exception
    {
        assertEquals(Ratio._16x9, this.resolver.resolveVideoRatio("_5x4", "IFRAME:VIDEO_5x4:Title"));
        assertEquals(Arrays.asList(new MarkdownIssue(Type.UNKNOWN_VIDEO_RATIO, SOURCE, "IFRAME:VIDEO_5x4:Title", "_5x4")), this.collector.getIssues());
    }

    @Test
    public void testIssueWithoutSource() throws Exception
    {
        MarkdownIssueCollector collector = MarkdownIssueHandler.collecting();
        new MarkdownDirectiveResolver(collector, null).resolveButtonStyle("BOGUS", "BUTTON:BOGUS:Label");
        assertFalse(collector.getIssues()
                             .get(0)
                             .getSource()
                             .isPresent());
    }

    @Test
    public void testIssueMessageNamesTokenAndSource() throws Exception
    {
        this.resolver.resolveButtonStyle("BOGUS", "BUTTON:BOGUS:Label");
        String message = this.collector.getIssues()
                                       .get(0)
                                       .getMessage();
        assertTrue(message.contains("BOGUS"), message);
        assertTrue(message.contains("BUTTON:BOGUS:Label"), message);
        assertTrue(message.contains(SOURCE), message);
    }

    @Test
    public void testResolveWithoutHandlerDoesNotFail() throws Exception
    {
        // a resolver without a handler still has to resolve, since a content file must never break the rendering
        MarkdownDirectiveResolver resolver = new MarkdownDirectiveResolver(null, null);
        assertEquals(Style.PRIMARY, resolver.resolveButtonStyle("BOGUS", "BUTTON:BOGUS:Label"));
        assertEquals(Ratio._16x9, resolver.resolveVideoRatio("_5x4", "IFRAME:VIDEO_5x4:Title"));
    }

    @Test
    public void testCollectorState() throws Exception
    {
        assertFalse(this.collector.hasIssues());
        this.resolver.resolveButtonStyle("BOGUS", "BUTTON:BOGUS:Label");
        assertTrue(this.collector.hasIssues());
        assertEquals(1, this.collector.stream()
                                      .count());
    }

    private List<Type> issueTypes()
    {
        return this.collector.stream()
                             .map(MarkdownIssue::getType)
                             .collect(java.util.stream.Collectors.toList());
    }

}
