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

public class OffcanvasNode extends AbstractNode implements Node
{
    @JsonProperty
    private String        type = "OFFCANVAS";

    @JsonProperty
    private I18nTextValue title;

    @JsonProperty
    private Node          content;

    @JsonProperty
    private boolean       visible;

    @JsonProperty
    private String        placement;

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

    public OffcanvasNode setTitle(I18nTextValue title)
    {
        this.title = title;
        return this;
    }

    public Node getContent()
    {
        return this.content;
    }

    public OffcanvasNode setContent(Node content)
    {
        this.content = content;
        return this;
    }

    public boolean isVisible()
    {
        return this.visible;
    }

    public OffcanvasNode setVisible(boolean visible)
    {
        this.visible = visible;
        return this;
    }

    public String getPlacement()
    {
        return this.placement;
    }

    public OffcanvasNode setPlacement(String placement)
    {
        this.placement = placement;
        return this;
    }

    public Handler getOnClose()
    {
        return this.onClose;
    }

    public OffcanvasNode setOnClose(Handler onClose)
    {
        this.onClose = onClose;
        return this;
    }

}
