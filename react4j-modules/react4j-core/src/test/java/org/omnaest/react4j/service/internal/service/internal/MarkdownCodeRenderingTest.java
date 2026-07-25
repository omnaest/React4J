package org.omnaest.react4j.service.internal.service.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.omnaest.react4j.EnableReactUI;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Inline code and code blocks were dropped by the markdown interpreter without a trace before, since the parser did not even expose them. This test renders
 * markdown through the real ui pipeline and pins down that their content arrives at the frontend.
 *
 * @author omnaest
 */
@SpringBootTest(classes = MarkdownCodeRenderingTest.TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class MarkdownCodeRenderingTest
{
    @SpringBootApplication
    @EnableReactUI
    public static class TestApplication
    {
    }

    @Autowired
    private MockMvc        mockMvc;

    @Autowired
    private ReactUIService reactUIService;

    @Test
    public void testInlineCodeIsRendered() throws Exception
    {
        assertTrue(this.render("Some text with `inlineCodeValue()` within it.")
                       .contains("inlineCodeValue()"));
    }

    @Test
    public void testFencedCodeBlockIsRendered() throws Exception
    {
        String rendered = this.render("```java\nint fencedCodeValue = 1;\n```\n");
        assertTrue(rendered.contains("int fencedCodeValue = 1;"), rendered);
        assertTrue(rendered.contains("language-java"), rendered);
    }

    @Test
    public void testIndentedCodeBlockIsRendered() throws Exception
    {
        assertTrue(this.render("    int indentedCodeValue = 1;\n")
                       .contains("int indentedCodeValue = 1;"));
    }

    @Test
    public void testCodeBlockContentIsHtmlEscaped() throws Exception
    {
        // the code content must not be able to close the surrounding tag or to inject markup
        String rendered = this.render("```\nif (a < b) { escapeMe(); }\n```\n");
        assertTrue(rendered.contains("&lt;"), rendered);
        assertFalse(rendered.contains("(a < b)"), rendered);
    }

    /**
     * Registers the given markdown as the ui and returns what the frontend receives for it.
     */
    private String render(String markdown) throws Exception
    {
        this.reactUIService.createDefaultRoot(reactUI -> reactUI.addNewComponent(factory -> factory.newComposite()
                                                                                                   .addComponents(factory.newMarkdown()
                                                                                                                         .texts()
                                                                                                                         .from(markdown))));
        return this.mockMvc.perform(get("/ui"))
                           .andExpect(status().isOk())
                           .andReturn()
                           .getResponse()
                           .getContentAsString();
    }

}
