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

import java.util.function.Consumer;

import org.omnaest.react4j.domain.markdown.internal.LoggingMarkdownIssueHandler;

/**
 * Receives every {@link MarkdownIssue} the markdown interpreter finds while rendering markdown content.<br>
 * <br>
 * A {@link MarkdownIssue} is deliberately not modelled as an {@link Exception}: a typo within a content file must never break the rendering of a page. Provide
 * an own implementation as a spring bean to route the issues somewhere else than the log, or hand one over per rendering call to collect them, e.g. for a
 * content lint or a test.
 *
 * @see #logging()
 * @see #collecting()
 * @see MarkdownIssue
 * @author omnaest
 */
@FunctionalInterface
public interface MarkdownIssueHandler extends Consumer<MarkdownIssue>
{
    @Override
    public void accept(MarkdownIssue issue);

    /**
     * Returns the default {@link MarkdownIssueHandler}, which writes every {@link MarkdownIssue} to the log.
     *
     * @return
     */
    public static MarkdownIssueHandler logging()
    {
        return new LoggingMarkdownIssueHandler();
    }

    /**
     * Returns a {@link MarkdownIssueHandler} which discards every {@link MarkdownIssue}.
     *
     * @return
     */
    public static MarkdownIssueHandler noOperation()
    {
        return issue ->
        {
            // do nothing
        };
    }

    /**
     * Returns a {@link MarkdownIssueCollector}, which keeps the {@link MarkdownIssue}s for later inspection instead of reporting them right away.
     *
     * @return
     */
    public static MarkdownIssueCollector collecting()
    {
        return new MarkdownIssueCollector();
    }

}
