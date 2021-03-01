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

import java.util.List;

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
    private List<Node> left;

    @JsonProperty
    private List<Node> right;

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

    public List<Node> getLeft()
    {
        return this.left;
    }

    public JumbotronNode setLeft(List<Node> left)
    {
        this.left = left;
        return this;
    }

    public List<Node> getRight()
    {
        return this.right;
    }

    public JumbotronNode setRight(List<Node> right)
    {
        this.right = right;
        return this;
    }

}
