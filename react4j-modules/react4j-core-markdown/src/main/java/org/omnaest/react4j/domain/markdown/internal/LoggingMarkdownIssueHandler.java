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
package org.omnaest.react4j.domain.markdown.internal;

import org.omnaest.react4j.domain.markdown.MarkdownIssue;
import org.omnaest.react4j.domain.markdown.MarkdownIssueHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link MarkdownIssueHandler} which writes every {@link MarkdownIssue} to the log.
 *
 * @see MarkdownIssueHandler#logging()
 * @author omnaest
 */
@Slf4j
public class LoggingMarkdownIssueHandler implements MarkdownIssueHandler
{
    @Override
    public void accept(MarkdownIssue issue)
    {
        log.warn(issue.getMessage());
    }

}
