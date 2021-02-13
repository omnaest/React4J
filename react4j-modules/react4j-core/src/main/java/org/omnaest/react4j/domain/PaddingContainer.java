package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface PaddingContainer extends UIComponentWithContent<PaddingContainer>
{

    public PaddingContainer withHorizontalPadding(boolean horizontalPadding);

    public PaddingContainer withNoHorizontalPadding();

    public PaddingContainer withVerticalPadding(boolean verticalPadding);

    public PaddingContainer withNoVerticalPadding();

}
