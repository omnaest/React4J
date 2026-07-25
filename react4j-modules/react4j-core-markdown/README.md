# react4j-core-markdown

Renders markdown content into React4J UI components.

```java
factory.newMarkdown()
       .texts()
       .fromContentFile("discord");   // renders content/discord.md
```

## Markdown directives

Beside standard [CommonMark](https://commonmark.org/), the interpreter understands a small set of directives. They are written as ordinary markdown, so any
other markdown renderer still shows a working document. The directive lives in the *label* of a link, which keeps the url in the url slot.

| Directive | Example | Renders |
|---|---|---|
| `[BUTTON:Label](link)` | `[BUTTON:Join us!](mailto:join@example.org)` | link button in the primary style |
| `[BUTTON:STYLE:Label](link)` | `[BUTTON:SUCCESS:Discord server](https://discord.example)` | link button in the given style |
| `[IFRAME:Title](link)` | `[IFRAME:A map](https://map.example)` | inline frame |
| `[IFRAME:VIDEO:Title](link)` | `[IFRAME:VIDEO:Intro](https://video.example)` | video frame in the default 16x9 ratio |
| `[IFRAME:VIDEO_RATIO:Title](link)` | `[IFRAME:VIDEO_4x3:Intro](https://video.example)` | video frame in the given ratio |
| `[[?]](link)` | `[[?]](https://source.example)` | auto numbered reference link `[1]`, `[2]`, ... |
| `[ICON:NAME]Text` | `[ICON:MICROSCOPE]Our research` | text prefixed by the icon |
| `{GRID}` within a table header | `\|{GRID}First\|Second\|` | table rendered as a grid container |
| `{#locator}` within a heading | `# Our partners{#partners}` | heading with an explicit link locator |

### Vocabularies

- **Button styles** — `PRIMARY`, `SECONDARY`, `SUCCESS`, `DANGER`, `WARNING`, `INFO`, `LIGHT`, `DARK`, `LINK` (see `Button.Style`). Upper case, exact match.
- **Video ratios** — `_16x9`, `_4x3`, `_1x1`, `_21x9` (see `RatioContainer.Ratio`), written as `VIDEO_16x9`.
- **Icons** — `ENVELOPE`, `DOLLAR_SIGN`, `MICROSCOPE`, `HEARTBEAT`, `DNA` (see `Icon.StandardIcon`).

### Two traps worth knowing

- The style token is only recognized when a **second** colon follows. `[BUTTON:Contact:Us](link)` therefore renders a button labelled `Us` and drops
  `Contact` - a label containing a colon needs the explicit style form, e.g. `[BUTTON:PRIMARY:Contact:Us](link)`.
- The vocabularies are **case sensitive**. `[BUTTON:success:Label]` is not the success style.

Both are reported as a `MarkdownIssue` rather than failing silently.

## Issue reporting

A directive the interpreter cannot make sense of never breaks the rendering - it falls back to a default. Every fallback is reported to the
`MarkdownIssueHandler`, which by default writes a warning naming the content file and line:

```
Unknown button style 'BOGUS', falling back to the primary style. Note that the token is removed from the button label, too.
Directive: <BUTTON:BOGUS:Join us!> Content: <discord:14>
```

Reported cases (see `MarkdownIssue.Type`): `UNKNOWN_BUTTON_STYLE`, `EMPTY_BUTTON_TEXT`, `UNKNOWN_ICON`, `UNKNOWN_VIDEO_RATIO`.

### Routing the issues somewhere else

Declare a `MarkdownIssueHandler` bean to replace the default logging handler for the whole application:

```java
@Bean
public MarkdownIssueHandler markdownIssueHandler()
{
    return issue -> this.contentTeamNotifier.notify(issue.getMessage());
}
```

### Collecting the issues of a single interpretation

Useful for a content lint or a test:

```java
MarkdownIssueCollector collector = MarkdownIssueHandler.collecting();
markdownService.interpreterWith(uiComponentFactory)
               .withSource("discord")
               .withIssueHandler(collector)
               .parseMarkdownElements(markdown);
collector.getIssues();
```
