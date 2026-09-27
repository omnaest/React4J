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
package org.omnaest.react4j.service.internal.nodes;

import org.omnaest.react4j.domain.raw.Node;
import org.omnaest.react4j.service.internal.nodes.handler.Handler;
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @see PaginationItemNode
 */
public class BreadcrumbEntryNode extends AbstractNode implements Node
{
    @JsonProperty
    private String        type = "BREADCRUMB_ENTRY";

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private String        link;

    @JsonProperty
    private String        linkedId;

    @JsonProperty
    private boolean       active;

    @JsonProperty
    private Handler       onClick;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getText()
    {
        return this.text;
    }

    public BreadcrumbEntryNode setText(I18nTextValue text)
    {
        this.text = text;
        return this;
    }

    public String getLink()
    {
        return this.link;
    }

    public BreadcrumbEntryNode setLink(String link)
    {
        this.link = link;
        return this;
    }

    public String getLinkedId()
    {
        return this.linkedId;
    }

    public BreadcrumbEntryNode setLinkedId(String linkedId)
    {
        this.linkedId = linkedId;
        return this;
    }

    public boolean isActive()
    {
        return this.active;
    }

    public BreadcrumbEntryNode setActive(boolean active)
    {
        this.active = active;
        return this;
    }

    public Handler getOnClick()
    {
        return this.onClick;
    }

    public BreadcrumbEntryNode setOnClick(Handler onClick)
    {
        this.onClick = onClick;
        return this;
    }

}
