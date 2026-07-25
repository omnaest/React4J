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

public class ModalNode extends AbstractNode implements Node
{
    @JsonProperty
    private String        type = "MODAL";

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private Node          content;

    @JsonProperty
    private Node          footer;

    @JsonProperty
    private boolean       visible;

    @JsonProperty
    private String        size;

    @JsonProperty
    private boolean       centered;

    @JsonProperty
    private Handler       onClose;

    @Override
    public String getType()
    {
        return this.type;
    }

    public I18nTextValue getTitle()
    {
        return this.title;
    }

    public ModalNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    public Node getContent()
    {
        return this.content;
    }

    public ModalNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public Node getFooter()
    {
        return this.footer;
    }

    public ModalNode setFooter(Node footer)
    {
        this.footer = footer;
        return this;
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    public ModalNode setVisible(boolean visible)
    {
        this.visible = visible;
        return this;
    }

    public String getSize()
    {
        return this.size;
    }

    public ModalNode setSize(String size)
    {
        this.size = size;
        return this;
    }

    public boolean isCentered()
    {
        return this.centered;
    }

    public ModalNode setCentered(boolean centered)
    {
        this.centered = centered;
        return this;
    }

    public Handler getOnClose()
    {
        return this.onClose;
    }

    public ModalNode setOnClose(Handler onClose)
    {
        this.onClose = onClose;
        return this;
    }

}
