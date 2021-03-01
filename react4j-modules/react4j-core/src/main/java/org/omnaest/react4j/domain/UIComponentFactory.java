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

import java.util.List;

/**
 * Factory for all UI relevant {@link UIComponent}s like grids, forms, paragraphs, cards, ankers, buttons, etc.
 * 
 * @author omnaest
 */
public interface UIComponentFactory
{
    public Paragraph newParagraph();

    public Button newButton();

    public Anker newAnker();

    public AnkerButton newAnkerButton();

    public BlockQuote newBlockQuote();

    public Card newCard();

    public Table newTable();

    public Composite newComposite();

    public ContainerGrid newContainerGrid();

    public NavigationBar newNavigationBar();

    public Form newForm();

    public Image newImage();

    public Heading newHeading();

    public Jumbotron newJumboTron();

    public UnsortedList newUnsortedList();

    public ImageIndex newImageIndex();

    public VerticalContentSwitcher newVerticalContentSwitcher();

    public ScrollbarContainer newScrollbarContainer();

    public Text newText();

    public List<UIComponent<?>> newMarkdownText(String markdown);

    public List<Card> newMarkdownCards(String markdown);

    public Card newMarkdownCard(String markdown);

    public LineBreak newLineBreak();

    public Toaster newToaster();

    public Icon newIcon();

    public PaddingContainer newPaddingContainer();

}
