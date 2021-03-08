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

import java.util.Optional;
import java.util.stream.Stream;

import org.omnaest.react4j.domain.Location;
import org.omnaest.react4j.domain.context.Context;
import org.omnaest.react4j.domain.context.data.DataContext;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.ui.UIContext;

/**
 * @see UIContext
 * @author omnaest
 */
public class UIContextImpl implements UIContext
{
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

    @Override
    public String getId(Location location)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<DataContext> asDataContext()
    {
        return Optional.empty();
    }

    @Override
    public Document get(int index)
    {
        return new DocumentImpl(index, this);
    }

    @Override
    public int size()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Stream<Document> stream()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
