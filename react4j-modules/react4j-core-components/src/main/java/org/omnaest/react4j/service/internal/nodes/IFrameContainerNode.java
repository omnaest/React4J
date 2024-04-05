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

public class IFrameContainerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "IFRAMECONTAINER";

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private String sourceLink;

    @JsonProperty
    private boolean allowFullScreen;

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public IFrameContainerNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public String getSourceLink()
    {
        return this.sourceLink;
    }

    public IFrameContainerNode setSourceLink(String sourceLink)
    {
        this.sourceLink = sourceLink;
        return this;
    }

    public boolean isAllowFullScreen()
    {
        return this.allowFullScreen;
    }

    public IFrameContainerNode setAllowFullScreen(boolean allowFullScreen)
    {
        this.allowFullScreen = allowFullScreen;
        return this;
    }

    @Override
    public String toString()
    {
        return "IFrameContainerNode [type=" + this.type + ", title=" + this.title + ", sourceLink=" + this.sourceLink + ", allowFullScreen="
                + this.allowFullScreen + "]";
    }

}
