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
import org.omnaest.react4j.service.internal.nodes.i18n.I18nTextValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PopoverNode extends AbstractNode implements Node
{
    @JsonProperty
    private String        type = "POPOVER";

    @JsonProperty
    private Node          content;

    @JsonProperty
    private Node          body;

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private String        placement;

    @JsonProperty
    private String        trigger;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Node getContent()
    {
        return this.content;
    }

    public PopoverNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public Node getBody()
    {
        return this.body;
    }

    public PopoverNode setBody(Node body)
    {
        this.body = body;
        return this;
    }

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public PopoverNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    public String getPlacement()
    {
        return this.placement;
    }

    public PopoverNode setPlacement(String placement)
    {
        this.placement = placement;
        return this;
    }

    public String getTrigger()
    {
        return this.trigger;
    }

    public PopoverNode setTrigger(String trigger)
    {
        this.trigger = trigger;
        return this;
    }

}
