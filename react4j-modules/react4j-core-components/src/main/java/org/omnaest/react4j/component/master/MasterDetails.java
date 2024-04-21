package org.omnaest.react4j.component.master;

import org.omnaest.react4j.domain.UIComponent;
import org.omnaest.react4j.domain.support.ComponentAndUIContextConsumer;

public interface MasterDetails extends UIComponent<MasterDetails>
{
    public MasterDetails withMasterContainer(ComponentAndUIContextConsumer<MasterContainer> masterAndUiContext);

    public MasterDetails withDetailsContainer(ComponentAndUIContextConsumer<DetailsContainer> detailsAndUiContext);

    public static interface Container<RT>
    {
        public RT withContent(UIComponent<?> content);

        public RT withColumnSpan(int columnSpan);
    }

    public static interface MasterContainer extends Container<MasterContainer>
    {
    }

    public static interface DetailsContainer extends Container<DetailsContainer>
    {
    }
}