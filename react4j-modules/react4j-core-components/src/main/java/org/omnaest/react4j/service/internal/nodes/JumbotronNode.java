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

public class JumbotronNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "JUMBOTRON";

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private I18nTextValue subTitle;

    @JsonProperty
    private Node content;

    @JsonProperty
    private boolean fullWidth;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public JumbotronNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    public I18nTextValue getSubTitle()
    {
        return this.subTitle;
    }

    public JumbotronNode setSubTitle(I18nTextValue subTitle)
    {
        this.subTitle = subTitle;
        return this;
    }

    public Node getContent()
    {
        return this.content;
    }

    public JumbotronNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public boolean isFullWidth()
    {
        return this.fullWidth;
    }

    public JumbotronNode setFullWidth(boolean fullWidth)
    {
        this.fullWidth = fullWidth;
        return this;
    }

}
