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
package org.omnaest.react4j.domain.markdown;

import java.util.Objects;
import java.util.Optional;

/**
 * A problem found within the markdown content while interpreting one of the markdown directives like <code>[BUTTON:SUCCESS:Label](link)</code>.<br>
 * <br>
 * An issue is not an error: the markdown is always rendered, the interpreter just had to fall back to a default. The issue makes that fallback visible instead
 * of letting it happen silently.
 *
 * @see MarkdownIssueHandler
 * @author omnaest
 */
public class MarkdownIssue
{
    private final Type    type;
    private final String  source;
    private final Integer line;
    private final String  rawToken;
    private final String  value;

    /**
     * The kind of problem found. Every {@link Type} marks a spot where the interpreter falls back to a default that the content author cannot see in the
     * rendered result.
     *
     * @author omnaest
     */
    public static enum Type
    {
        /**
         * The style token of a <code>[BUTTON:STYLE:Label](link)</code> directive is not one of the known styles. The button falls back to the primary style and
         * the unknown token is lost from the button label.
         */
        UNKNOWN_BUTTON_STYLE,
        /**
         * A <code>[BUTTON:...](link)</code> directive without any label, which renders as an empty button.
         */
        EMPTY_BUTTON_TEXT,
        /**
         * The icon name of an <code>[ICON:NAME]</code> directive is not one of the known icons, so no icon is rendered at all.
         */
        UNKNOWN_ICON,
        /**
         * The ratio token of an <code>[IFRAME:VIDEO_16x9:Title](link)</code> directive is not one of the known ratios, so the default ratio is used.
         */
        UNKNOWN_VIDEO_RATIO
    }

    public MarkdownIssue(Type type, String source, String rawToken, String value)
    {
        this(type, source, null, rawToken, value);
    }

    public MarkdownIssue(Type type, String source, Integer line, String rawToken, String value)
    {
        super();
        this.type = type;
        this.source = source;
        this.line = line;
        this.rawToken = rawToken;
        this.value = value;
    }

    /**
     * Returns the line number, starting at 1, within the markdown source the issue was found at. Returns {@link Optional#empty()} if the origin could not be
     * determined.
     *
     * @return
     */
    public Optional<Integer> getLine()
    {
        return Optional.ofNullable(this.line);
    }

    public Type getType()
    {
        return this.type;
    }

    /**
     * Returns the origin of the markdown content, which is the content file identifier like 'discord' for the 'content/discord.md' file. Returns
     * {@link Optional#empty()} for markdown that was not read from a content file.
     *
     * @return
     */
    public Optional<String> getSource()
    {
        return Optional.ofNullable(this.source);
    }

    /**
     * Returns the raw directive the issue was found in, like <code>BUTTON:UNKNOWN:Label</code>.
     *
     * @return
     */
    public String getRawToken()
    {
        return this.rawToken;
    }

    /**
     * Returns the token that could not be interpreted, like the style name of a button directive.
     *
     * @return
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * Returns a message that names the problem, the fallback taken and the origin of the content.
     *
     * @return
     */
    public String getMessage()
    {
        StringBuilder message = new StringBuilder();
        switch (this.type)
        {
        case UNKNOWN_BUTTON_STYLE:
            message.append("Unknown button style '")
                   .append(this.value)
                   .append("', falling back to the primary style. Note that the token is removed from the button label, too.");
            break;
        case EMPTY_BUTTON_TEXT:
            message.append("Button directive without a label, which renders an empty button.");
            break;
        case UNKNOWN_ICON:
            message.append("Unknown icon '")
                   .append(this.value)
                   .append("', no icon is rendered.");
            break;
        case UNKNOWN_VIDEO_RATIO:
            message.append("Unknown video ratio '")
                   .append(this.value)
                   .append("', falling back to the default ratio.");
            break;
        default:
            message.append("Unknown issue '")
                   .append(this.value)
                   .append("'.");
            break;
        }
        message.append(" Directive: <")
               .append(this.rawToken)
               .append(">");
        this.getSource()
            .ifPresent(source -> message.append(" Content: <")
                                        .append(source)
                                        .append(this.getLine()
                                                    .map(line -> ":" + line)
                                                    .orElse(""))
                                        .append(">"));
        return message.toString();
    }

    @Override
    public String toString()
    {
        return "MarkdownIssue [type=" + this.type + ", source=" + this.source + ", line=" + this.line + ", rawToken=" + this.rawToken + ", value=" + this.value
                + "]";
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.line, this.rawToken, this.source, this.type, this.value);
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }
        if (!(object instanceof MarkdownIssue))
        {
            return false;
        }
        MarkdownIssue other = (MarkdownIssue) object;
        return Objects.equals(this.line, other.line) && Objects.equals(this.rawToken, other.rawToken) && Objects.equals(this.source, other.source)
                && this.type == other.type && Objects.equals(this.value, other.value);
    }

}
