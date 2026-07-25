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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * {@link MarkdownIssueHandler} which keeps every received {@link MarkdownIssue} instead of reporting it right away. This is the handler to use for a content
 * lint over all content files and for tests around the markdown directives.
 *
 * @see MarkdownIssueHandler#collecting()
 * @author omnaest
 */
public class MarkdownIssueCollector implements MarkdownIssueHandler
{
    private final List<MarkdownIssue> issues = new CopyOnWriteArrayList<>();

    @Override
    public void accept(MarkdownIssue issue)
    {
        this.issues.add(issue);
    }

    public List<MarkdownIssue> getIssues()
    {
        return this.issues;
    }

    public Stream<MarkdownIssue> stream()
    {
        return this.issues.stream();
    }

    public boolean hasIssues()
    {
        return !this.issues.isEmpty();
    }

    @Override
    public String toString()
    {
        return "MarkdownIssueCollector [issues=" + this.issues + "]";
    }

}
