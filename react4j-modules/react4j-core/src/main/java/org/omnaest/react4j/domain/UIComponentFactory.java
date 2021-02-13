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