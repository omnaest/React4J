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

import org.omnaest.react4j.component.anker.Anker;
import org.omnaest.react4j.component.ankerbutton.AnkerButton;
import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.table.Table;

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

    public GridContainer newGridContainer();

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

    @Deprecated
    public List<UIComponent<?>> newMarkdownText(String markdown);

    /**
     * Returns a {@link List} of {@link UIComponent}s based on the markdown file in the content folder. <br>
     * The suffix of the file has to be '.md' and the identifier represents the file name itself.<br>
     * <br>
     * E.g. to address the 'content/mardown_example.md' file, the identifier has to be 'markdown_example'.
     * 
     * @param identifier
     * @return
     */
    @Deprecated
    public List<UIComponent<?>> newMarkdownTextFromContent(String identifier);

    @Deprecated
    public List<Card> newMarkdownCards(String markdown);

    /**
     * Returns a {@link List} of {@link Card}s based on the markdown file in the content folder. <br>
     * The suffix of the file has to be '.md' and the identifier represents the file name itself.<br>
     * <br>
     * E.g. to address the 'content/mardown_example.md' file, the identifier has to be 'markdown_example'.
     * 
     * @see ContentService
     * @param identifier
     * @return
     */
    @Deprecated
    public List<Card> newMarkdownCardsFromContent(String identifier);

    @Deprecated
    public Card newMarkdownCard(String markdown);

    /**
     * Returns a {@link Card} based on the markdown file in the content folder. <br>
     * The suffix of the file has to be '.md' and the identifier represents the file name itself.<br>
     * <br>
     * E.g. to address the 'content/mardown_example.md' file, the identifier has to be 'markdown_example'.
     * 
     * @param identifier
     * @return
     */
    @Deprecated
    public Card newMarkdownCardFromContent(String identifier);

    public MarkdownComponentChoice newMarkdown();

    public static interface MarkdownComponentChoice
    {
        public MarkdownComponentFactory<List<UIComponent<?>>> texts();

        public MarkdownComponentFactory<List<Card>> cards();

        public MarkdownComponentFactory<Card> card();
    }

    public static interface MarkdownComponentFactory<U>
    {
        public U from(String markdown);

        public U fromContentFile(String identifier);
    }

    public LineBreak newLineBreak();

    public Toaster newToaster();

    public Icon newIcon();

    public PaddingContainer newPaddingContainer();

    public TextAlignmentContainer newTextAlignmentContainer();

    public RerenderingContainer newRerenderingContainer();

    public IntervalRerenderingContainer newIntervalRerenderingContainer();

    public ProgressBar newProgressBar();

    public RatioContainer newRatioContainer();

    public IFrame newIFrame();

    public NativeHtml newNativeHtml();

    public SVGContainer newSVGContainer();

    public SizedContainer newSizedContainer();
}
