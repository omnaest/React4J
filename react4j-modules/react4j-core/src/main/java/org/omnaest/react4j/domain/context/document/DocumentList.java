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
package org.omnaest.react4j.domain.context.document;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.omnaest.utils.stream.Streamable;

public interface DocumentList extends Streamable<Document>
{
    /**
     * Returns the {@link Document} at the given index position. Index = 0,1,2, ...
     * 
     * @param index
     * @return
     */
    public Document get(int index);

    /**
     * Returns the first {@link Document}
     * 
     * @return
     */
    public default Document getFirstDocument()
    {
        return this.get(0);
    }

    /**
     * Returns the number of elements
     * 
     * @return
     */
    public int size();

    @Override
    default Stream<Document> stream()
    {
        return IntStream.range(0, this.size())
                        .mapToObj(this::get);
    }

}
