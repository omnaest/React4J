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

public class AnkerButtonNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "ANKERBUTTON";

    @JsonProperty
    private I18nTextValue text;

    @JsonProperty
    private String link;

    @JsonProperty
    private String style;

    @JsonProperty
    private Page page;

    public static enum Page
    {
        SELF, BLANK
    }

    public Page getPage()
    {
        return this.page;
    }

    public AnkerButtonNode setPage(Page page)
    {
        this.page = page;
        return this;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    public AnkerButtonNode setText(I18nTextValue text)
    {
        this.text = text;
        return this;
    }

    public I18nTextValue getText()
    {
        return this.text;
    }

    public String getLink()
    {
        return this.link;
    }

    public AnkerButtonNode setLink(String link)
    {
        this.link = link;
        return this;
    }

    public String getStyle()
    {
        return this.style;
    }

    public AnkerButtonNode setStyle(String style)
    {
        this.style = style;
        return this;
    }

}
