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
package org.omnaest.react4j.domain;

import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface ScrollbarContainer extends UIComponentWithContent<ScrollbarContainer>
{
    public ScrollbarContainer withVerticalBox(VerticalBoxMode verticalBoxMode);

    public ScrollbarContainer withHorizontalBox(HorizontalBoxMode horizontalBoxMode);

    /**
     * Opt-in: enables auto-scrolling the container's viewport to the bottom whenever its content updates. Default is
     * disabled (off), so existing usages are unaffected unless this method is called.
     *
     * @return this
     */
    public ScrollbarContainer scrollToBottomOnUpdate();

    public static enum VerticalBoxMode
    {
        FULL_VIEWPORT_HEIGHT,
        HALF_VIEWPORT_HEIGHT,
        FULL_VIEWPORT_HEIGHT_WITHOUT_HEADER,
        FULL_PARENT_HEIGHT,

        /**
         * Makes this region a vertical flow: it takes the full height of its parent and lays its children out top to
         * bottom, so that one of them can claim whatever height the others leave over.
         * <p>
         * This is the PARENT half of the fill-remaining pattern; the child that should take the leftover space uses
         * {@link #FILL_REMAINING_HEIGHT}. Together they express the ordinary application shell - a header of its
         * natural height, a body that takes exactly what is left and scrolls inside itself, and a footer of its
         * natural height - with no fixed pixel values anywhere, so it stays correct when the header's font, padding or
         * content changes.
         * <p>
         * Note this is NOT a Bootstrap feature. Bootstrap's own layouts let the whole document scroll and pin chrome
         * with {@code position: sticky}; it ships no {@code min-height} utility, which is the one property this pattern
         * needs. Hence expressing it here, once, rather than in every application's stylesheet.
         */
        VERTICAL_FLOW,

        /**
         * Makes this region take whatever vertical space its siblings leave over, and scroll inside itself rather than
         * growing the page.
         * <p>
         * The CHILD half of the fill-remaining pattern: the enclosing region must be a {@link #VERTICAL_FLOW}, or
         * there is no leftover space to claim and this behaves as an ordinary block.
         * <p>
         * Prefer this over {@link #FULL_VIEWPORT_HEIGHT_WITHOUT_HEADER}, which hardcodes the header at 80px and is
         * silently wrong for any other header.
         */
        FILL_REMAINING_HEIGHT,

        DEFAULT_HEIGHT
    }

    public static enum HorizontalBoxMode
    {
        FULL_VIEWPORT_WIDTH, FULL_PARENT_WIDTH, DEFAULT_WIDTH, FULL_CONTENT_WIDTH
    }
}
