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

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiagramViewerNode extends AbstractNode implements Node
{
    @JsonProperty
    private String  type = "DIAGRAMVIEWER";

    @JsonProperty
    private String  svg;

    @JsonProperty
    private String  maxHeight;

    @JsonProperty
    private String  width;

    @JsonProperty
    private String  height;

    @JsonProperty
    private boolean interactive;

    @Override
    public String getType()
    {
        return this.type;
    }

    public String getSvg()
    {
        return this.svg;
    }

    public DiagramViewerNode setSvg(String svg)
    {
        this.svg = svg;
        return this;
    }

    public String getMaxHeight()
    {
        return this.maxHeight;
    }

    public DiagramViewerNode setMaxHeight(String maxHeight)
    {
        this.maxHeight = maxHeight;
        return this;
    }

    public String getWidth()
    {
        return this.width;
    }

    public DiagramViewerNode setWidth(String width)
    {
        this.width = width;
        return this;
    }

    public String getHeight()
    {
        return this.height;
    }

    public DiagramViewerNode setHeight(String height)
    {
        this.height = height;
        return this;
    }

    public boolean isInteractive()
    {
        return this.interactive;
    }

    public DiagramViewerNode setInteractive(boolean interactive)
    {
        this.interactive = interactive;
        return this;
    }

    @Override
    public String toString()
    {
        return "DiagramViewerNode [type=" + this.type + ", svg=" + this.svg + ", maxHeight=" + this.maxHeight + ", width=" + this.width + ", height="
               + this.height + ", interactive=" + this.interactive + "]";
    }

}
