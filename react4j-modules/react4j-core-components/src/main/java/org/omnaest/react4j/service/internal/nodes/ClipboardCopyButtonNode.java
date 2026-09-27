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

public class ClipboardCopyButtonNode extends AbstractNode implements Node
{
    @JsonProperty
    private String        type = "CLIPBOARDCOPYBUTTON";

    @JsonProperty
    private I18nTextValue label;

    @JsonProperty
    private String        text;

    @JsonProperty
    private int           clearAfterDurationMillis;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getLabel()
    {
        return this.label;
    }

    public ClipboardCopyButtonNode setLabel(I18nTextValue label)
    {
        this.label = label;
        return this;
    }

    public String getText()
    {
        return this.text;
    }

    public ClipboardCopyButtonNode setText(String text)
    {
        this.text = text;
        return this;
    }

    public int getClearAfterDurationMillis()
    {
        return this.clearAfterDurationMillis;
    }

    public ClipboardCopyButtonNode setClearAfterDurationMillis(int clearAfterDurationMillis)
    {
        this.clearAfterDurationMillis = clearAfterDurationMillis;
        return this;
    }

}
