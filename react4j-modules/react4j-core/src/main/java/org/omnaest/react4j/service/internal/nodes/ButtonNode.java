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

public class ButtonNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "BUTTON";

    @JsonProperty
    private I18nTextValue name;

    @JsonProperty
    private String style;

    @JsonProperty
    private Handler onClick;

    @Override
    public String getType()
    {
        return this.type;
    }

    public Handler getOnClick()
    {
        return this.onClick;
    }

    public ButtonNode setOnClick(Handler onClick)
    {
        this.onClick = onClick;
        return this;
    }

    public I18nTextValue getName()
    {
        return this.name;
    }

    public ButtonNode setName(I18nTextValue i18nTextValue)
    {
        this.name = i18nTextValue;
        return this;
    }

    public String getStyle()
    {
        return this.style;
    }

    public ButtonNode setStyle(String style)
    {
        this.style = style;
        return this;
    }

}
