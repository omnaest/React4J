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

public class ImageIndexNode extends AbstractNode implements Node
{
    @JsonProperty
    private String type = "IMAGEINDEX";

    @JsonProperty
    private List<Entry> entries;

    @Override
    public String getType()
    {
        return this.type;
    }

    public List<Entry> getEntries()
    {
        return this.entries;
    }

    public ImageIndexNode setEntries(List<Entry> entries)
    {
        this.entries = entries;
        return this;
    }

    public static class Entry
    {
        @JsonProperty
        private I18nTextValue title;

        @JsonProperty
        private String id;

        @JsonProperty
        private String image;

        public I18nTextValue getTitle()
        {
            return this.title;
        }

        public Entry setTitle(I18nTextValue title)
        {
            this.title = title;
            return this;
        }

        public String getId()
        {
            return this.id;
        }

        public Entry setId(String id)
        {
            this.id = id;
            return this;
        }

        public String getImage()
        {
            return this.image;
        }

        public Entry setImage(String image)
        {
            this.image = image;
            return this;
        }

    }
}
