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

import java.util.function.Consumer;

import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.support.UIComponentWithContent;

public interface Card extends UIComponentWithContent<Card>
{
    public Card withTitle(String title);

    public Card withTitle(Field field);

    public Card withLinkLocator(String locator);

    public Card withAdjustment(boolean value);

    public Card withImage(Consumer<Image> imageConsumer);

    public Card withSubTitle(String subTitle);

    /**
     * Content for the card's header - a Bootstrap {@code card-header}, rendered above the body and visually separated
     * from it.
     * <p>
     * Distinct from {@link #withTitle(String)}, which is a heading INSIDE the body: a header can hold components, so
     * it is where controls that act on the whole card belong (a toolbar, a filter chip row, a close button). Without
     * it an app has to place such controls in the body and separate them by hand.
     *
     * @param component
     *            the header content, or {@code null} for no header
     * @return this
     */
    public Card withHeader(UIComponent<?> component);

    /**
     * Content for the card's footer - a Bootstrap {@code card-footer}, rendered below the body and visually separated
     * from it.
     * <p>
     * Useful for the actions or summary belonging to a card: a submit row, a result count, pagination. Note that the
     * footer sits after the body in normal flow; it does not pin to the bottom of a fixed-height card on its own.
     *
     * @param component
     *            the footer content, or {@code null} for no footer
     * @return this
     */
    public Card withFooter(UIComponent<?> component);

    /**
     * Makes the card take the full height of its parent instead of sizing to its content.
     * <p>
     * The point is not the height itself but what follows from it: a Bootstrap card is already a vertical flex
     * container whose body grows, so a full-height card puts its {@link #withFooter(UIComponent)} at the BOTTOM of the
     * available space rather than immediately under the content. That is the difference between a chat panel whose
     * message box sits at the foot of the panel and one whose message box floats halfway up it.
     * <p>
     * Only meaningful when the parent has a height to fill - pair it with
     * {@link org.omnaest.react4j.domain.ScrollbarContainer.VerticalBoxMode#FILL_REMAINING_HEIGHT} or
     * {@code FULL_PARENT_HEIGHT}, or there is nothing to be 100% of.
     *
     * @param fullHeight
     *            {@code true} to fill the parent's height
     * @return this
     */
    public Card withFullHeight(boolean fullHeight);

    /**
     * An accessible name for this card, which also promotes it to a landmark region a screen reader user can jump
     * between. Emits {@code role="region"} with {@code aria-label}.
     * <p>
     * Worth setting on a card that is a major area of the page - a panel, a sidebar, a transcript - because landmarks
     * are how a screen reader user navigates past content rather than through it. Not worth setting on a card used as
     * a list item or a tile: promoting dozens of them to landmarks makes the landmark list useless.
     *
     * @param ariaLabel
     *            the accessible name, or {@code null} to leave the card an ordinary container
     * @return this
     */
    public Card withAriaLabel(String ariaLabel);


}
