package org.omnaest.react4j.domain;

public interface UIComponentFactory
{
    public Paragraph newParagraph();

    public Button newButton();

    public Anker newAnker();

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
}