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
package org.omnaest.react4j.service.internal.service.internal.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.Locations;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.ui.UIContext;
import org.omnaest.utils.EncoderUtils.TextEncoderAndDecoderFactory;
import org.omnaest.utils.ListUtils;
import org.omnaest.utils.StringUtils;
import org.omnaest.utils.StringUtils.StringEncoderAndDecoder;

/**
 * @see UIContext
 * @author omnaest
 */
public class UIContextImpl implements UIContext
{
    private Function<Location, String> idFunction        = location -> this.encodeLocation(location);
    private StringEncoderAndDecoder    encoderAndDecoder = StringUtils.encoder()
                                                                      .with(TextEncoderAndDecoderFactory::forAlphaNumericText);
    private Locations                  locations;

    private List<Document> documents = new ArrayList<>();

    public UIContextImpl(Locations locations)
    {
        super();
        this.locations = locations;
    }

    @Override
    public String getId(Location location)
    {
        return this.idFunction.apply(location);
    }

    @Override
    public UIContext withId(String id)
    {
        this.idFunction = location -> id;
        return this;
    }

    private String encodeLocation(Location location)
    {

        return this.encoderAndDecoder.encodeList(this.locations.findParentOf(location)
                                                               .get(),
                                                 ".");
    }

    @Override
    public Optional<DataContext> asDataContext()
    {
        return Optional.empty();
    }

    @Override
    public Document get(int index)
    {
        return ListUtils.ensureSize(this.documents, index + 1, elementIndex -> new DocumentImpl(elementIndex, this))
                        .get(index);
    }

    @Override
    public int size()
    {
        return this.documents.size();
    }

    @Override
    public Stream<Document> stream()
    {
        return this.documents.stream();
    }

    private static class DocumentImpl implements Document
    {
        private int       index;
        private UIContext uiContext;

        public DocumentImpl(int index, UIContext uiContext)
        {
            this.index = index;
            this.uiContext = uiContext;
        }

        @Override
        public Field getField(String fieldName)
        {
            return new Field()
            {
                @Override
                public String getFieldName()
                {
                    return fieldName;
                }

                @Override
                public Document getDocument()
                {
                    return DocumentImpl.this;
                }

                @Override
                public Context getContext()
                {
                    return this.getDocument()
                               .getContext();
                }
            };
        }

        @Override
        public Context getContext()
        {
            return this.uiContext;
        }
    }

}
