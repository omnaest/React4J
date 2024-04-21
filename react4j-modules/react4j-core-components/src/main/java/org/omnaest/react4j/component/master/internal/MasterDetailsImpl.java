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
package org.omnaest.react4j.component.master.internal;

import java.util.Optional;

import org.omnaest.react4j.component.master.MasterDetails;
import org.omnaest.react4j.domain.GridContainer;
import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.rendering.RenderableUIComponent;
import org.omnaest.react4j.domain.rendering.UIComponentRenderer;
import org.omnaest.react4j.domain.support.ComponentAndUIContextConsumer;
import org.omnaest.react4j.domain.support.UIComponentProvider;
import org.omnaest.react4j.service.internal.component.AbstractUIComponentWithSubComponents;
import org.omnaest.react4j.service.internal.component.ComponentContext;

import lombok.Builder;
import lombok.Data;

public class MasterDetailsImpl extends AbstractUIComponentWithSubComponents<MasterDetails> implements MasterDetails
{
    private MasterDetailsData.MasterDetailsDataBuilder data;

    public MasterDetailsImpl(ComponentContext context)
    {
        this(context, MasterDetailsData.builder());
    }

    public MasterDetailsImpl(ComponentContext context, MasterDetailsData.MasterDetailsDataBuilder data)
    {
        super(context);
        this.data = data;
    }

    @SuppressWarnings("rawtypes")
    private static class AbstractContainerImpl<RT> implements Container<RT>, UIComponentProvider<UIComponent>
    {
        private UIComponent<?> content;
        private int            columnSpan = 6;

        public int getColumnSpan()
        {
            return this.columnSpan;
        }

        @Override
        public UIComponent get()
        {
            return this.content;
        }

        @SuppressWarnings("unchecked")
        @Override
        public RT withContent(UIComponent<?> content)
        {
            this.content = content;
            return (RT) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public RT withColumnSpan(int columnSpan)
        {
            this.columnSpan = columnSpan;
            return (RT) this;
        }

    }

    private static class DetailsContainerImpl extends AbstractContainerImpl<DetailsContainer> implements DetailsContainer
    {
    }

    private static class MasterContainerImpl extends AbstractContainerImpl<MasterContainer> implements MasterContainer
    {
    }

    @Data
    @Builder(toBuilder = true)
    public static class MasterDetailsData
    {
        private ComponentAndUIContextConsumer<MasterContainer>  masterComponentFunction;
        private ComponentAndUIContextConsumer<DetailsContainer> detailsComponentFunction;
    }

    @SuppressWarnings("unchecked")
    @Override
    public UIComponentRenderer asRenderer()
    {
        return ((RenderableUIComponent<GridContainer>) this.createGridContainer()).asRenderer();
    }

    private GridContainer createGridContainer()
    {
        return this.context.getUiComponentFactory()
                           .newGridContainer()
                           .withUIContext((container, uiContext) ->
                           {
                               MasterDetailsData effectiveData = this.data.build();

                               MasterContainerImpl masterContainerImpl = new MasterContainerImpl();
                               Optional.of(effectiveData)
                                       .map(MasterDetailsData::getMasterComponentFunction)
                                       .ifPresent(componentFunction -> componentFunction.accept(masterContainerImpl, uiContext));

                               DetailsContainerImpl detailsContainerImpl = new DetailsContainerImpl();
                               Optional.of(effectiveData)
                                       .map(MasterDetailsData::getDetailsComponentFunction)
                                       .ifPresent(componentFunction -> componentFunction.accept(detailsContainerImpl, uiContext));

                               container.addRow(row -> row.addCell(cell -> cell.withColumnSpan(masterContainerImpl.getColumnSpan())
                                                                               .withContent(() -> masterContainerImpl.get()))
                                                          .addCell(cell -> cell.withColumnSpan(detailsContainerImpl.getColumnSpan())
                                                                               .withContent(() -> detailsContainerImpl.get())));
                           });
    }

    @Override
    public UIComponentProvider<MasterDetails> asTemplateProvider()
    {
        return () -> new MasterDetailsImpl(this.context, this.data.build()
                                                                  .toBuilder());
    }

    @Override
    public MasterDetails withMasterContainer(ComponentAndUIContextConsumer<MasterContainer> masterComponentFunction)
    {
        this.data.masterComponentFunction(masterComponentFunction);
        return this;
    }

    @Override
    public MasterDetails withDetailsContainer(ComponentAndUIContextConsumer<DetailsContainer> detailsComponentFunction)
    {
        this.data.detailsComponentFunction(detailsComponentFunction);
        return this;
    }
}
