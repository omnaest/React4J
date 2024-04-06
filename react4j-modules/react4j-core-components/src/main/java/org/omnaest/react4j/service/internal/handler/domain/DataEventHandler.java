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
package org.omnaest.react4j.service.internal.handler.domain;

import org.omnaest.react4j.domain.context.data.Data;

import lombok.Builder;

@FunctionalInterface
public interface DataEventHandler
{
    public MappedData invoke(Data data, Data internalData);

    @lombok.Data
    @Builder
    public static class MappedData
    {
        private final Data data;
        private final Data internalData;

        public MappedData mergeWith(MappedData mappedData)
        {
            return MappedData.builder()
                             .data(this.data.mergeWith(mappedData.getData()))
                             .internalData(this.internalData.mergeWith(mappedData.getInternalData()))
                             .build();
        }
    }
}
