package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface ScrollbarContainer extends UIComponentWithContent<ScrollbarContainer>
{
    public ScrollbarContainer withVerticalBox(VerticalBoxMode verticalBoxMode);

    public ScrollbarContainer withHorizontalBox(HorizontalBoxMode horizontalBoxMode);

    public static enum VerticalBoxMode
    {
        FULL_VIEWPORT_HEIGHT, HALF_VIEWPORT_HEIGHT, FULL_VIEWPORT_HEIGHT_WITHOUT_HEADER, FULL_PARENT_HEIGHT, DEFAULT_HEIGHT
    }

    public static enum HorizontalBoxMode
    {
        FULL_VIEWPORT_WIDTH, FULL_PARENT_WIDTH, DEFAULT_WIDTH, FULL_CONTENT_WIDTH
    }
}