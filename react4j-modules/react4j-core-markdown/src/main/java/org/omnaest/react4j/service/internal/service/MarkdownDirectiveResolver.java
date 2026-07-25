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
package org.omnaest.react4j.service.internal.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.domain.Button.Style;
import org.omnaest.react4j.domain.Icon.StandardIcon;
import org.omnaest.react4j.domain.RatioContainer.Ratio;
import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.domain.markdown.MarkdownIssue.Type;
import org.omnaest.react4j.domain.markdown.MarkdownIssueHandler;
import org.omnaest.utils.EnumUtils;

/**
 * Resolves the tokens of the markdown directives like <code>[BUTTON:SUCCESS:Label](link)</code> into their domain values.<br>
 * <br>
 * Every resolution falls back to a default if the token cannot be interpreted, since a content file must never break the rendering. Each fallback is reported
 * to the {@link MarkdownIssueHandler} instead of happening silently.
 *
 * @see MarkdownIssueHandler
 * @author omnaest
 */
public class MarkdownDirectiveResolver
{
    private final MarkdownIssueHandler issueHandler;
    private final String               source;
    private final Integer              line;

    /**
     * @see #MarkdownDirectiveResolver(MarkdownIssueHandler, String, Integer)
     * @param issueHandler
     * @param source
     */
    public MarkdownDirectiveResolver(MarkdownIssueHandler issueHandler, String source)
    {
        this(issueHandler, source, null);
    }

    /**
     * @param issueHandler
     *            handler the found {@link MarkdownIssue}s are reported to
     * @param source
     *            origin of the markdown content, e.g. the content file identifier. Can be null.
     * @param line
     *            line number, starting at 1, of the directive within that source. Can be null.
     */
    public MarkdownDirectiveResolver(MarkdownIssueHandler issueHandler, String source, Integer line)
    {
        super();
        this.issueHandler = Optional.ofNullable(issueHandler)
                                    .orElseGet(MarkdownIssueHandler::noOperation);
        this.source = source;
        this.line = line;
    }

    /**
     * Resolves the style token of a button directive. A missing token is the regular case of a button without an explicit style, an unknown token is reported as
     * {@link Type#UNKNOWN_BUTTON_STYLE}.
     *
     * @param rawStyle
     * @param rawToken
     * @return
     */
    public Style resolveButtonStyle(String rawStyle, String rawToken)
    {
        if (StringUtils.isBlank(rawStyle))
        {
            return Style.PRIMARY;
        }
        return Style.of(rawStyle)
                    .orElseGet(() ->
                    {
                        this.report(Type.UNKNOWN_BUTTON_STYLE, rawToken, rawStyle);
                        return Style.PRIMARY;
                    });
    }

    /**
     * Resolves the label of a button directive. A blank label is reported as {@link Type#EMPTY_BUTTON_TEXT}.
     *
     * @param rawText
     * @param rawToken
     * @return
     */
    public String resolveButtonText(String rawText, String rawToken)
    {
        if (StringUtils.isBlank(rawText))
        {
            this.report(Type.EMPTY_BUTTON_TEXT, rawToken, rawText);
            return StringUtils.defaultString(rawText);
        }
        return rawText;
    }

    /**
     * Resolves the name token of an icon directive. An unknown name is reported as {@link Type#UNKNOWN_ICON} and resolves to {@link Optional#empty()}, since
     * there is no icon to fall back to.
     *
     * @param rawIcon
     * @param rawToken
     * @return
     */
    public Optional<StandardIcon> resolveIcon(String rawIcon, String rawToken)
    {
        Optional<StandardIcon> icon = Optional.ofNullable(rawIcon)
                                              .flatMap(StandardIcon::of);
        if (!icon.isPresent())
        {
            this.report(Type.UNKNOWN_ICON, rawToken, rawIcon);
        }
        return icon;
    }

    /**
     * Resolves the ratio token of a video directive. A missing token is the regular case of a video without an explicit ratio, an unknown token is reported as
     * {@link Type#UNKNOWN_VIDEO_RATIO}.
     *
     * @param rawRatio
     * @param rawToken
     * @return
     */
    public Ratio resolveVideoRatio(String rawRatio, String rawToken)
    {
        if (StringUtils.isBlank(rawRatio))
        {
            return Ratio._16x9;
        }
        return EnumUtils.toEnumValue(rawRatio, Ratio.class)
                        .orElseGet(() ->
                        {
                            this.report(Type.UNKNOWN_VIDEO_RATIO, rawToken, rawRatio);
                            return Ratio._16x9;
                        });
    }

    private void report(Type type, String rawToken, String value)
    {
        this.issueHandler.accept(new MarkdownIssue(type, this.source, this.line, rawToken, value));
    }

}
