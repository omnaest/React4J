package org.omnaest.react4j;

import javax.annotation.PostConstruct;

import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

//@Service
@Profile("mock")
public class MockUI
{
    @Autowired
    private ReactUIService uiService;

    @PostConstruct
    public void init()
    {
        this.uiService.getOrCreateDefaultRoot(reactUI ->
        {

            reactUI.withNavigationBar(navigationBar -> navigationBar.addEntry(entry -> entry.withText("News")
                                                                                            .withLinkedLocator("news")))
                   .addNewComponent(factory -> factory.newParagraph()
                                                      .addText("I love you!"))
                   .addNewComponent(factory -> factory.newButton()
                                                      .withName("Click to love me back!"))
                   .addNewComponent(f -> f.newAnker()
                                          .withText("Some link")
                                          .withLink("http://www.wikipedia.de"))
                   .addNewComponent(f -> f.newBlockQuote()
                                          .addText("This is a test")
                                          .withFooter("Footer!!!"))
                   .addNewComponent(factory -> factory.newTable()
                                                      .withColumnTitles("Column1", "Column2")
                                                      .addRow(row -> row.addCell(cell -> cell.withContent(factory.newParagraph()
                                                                                                                 .addText("Cell1")))
                                                                        .addCell(cell -> cell.withContent(factory.newParagraph()
                                                                                                                 .addText("Cell2")))))
                   .addNewComponent(factory -> factory.newComposite()
                                                      .addComponent(factory.newAnker()
                                                                           .withText("Some anker"))
                                                      .addComponent(factory.newButton()
                                                                           .withName("Some button")))
                   .addNewComponent(f -> f.newContainerGrid()
                                          .addRow(row -> row.addCell(cell -> cell.withContent(f2 -> f2.newParagraph()
                                                                                                      .addText("Cell1")))
                                                            .addCell(cell -> cell.withContent(f2 -> f2.newParagraph()
                                                                                                      .addText("Cell2")))))
                   .addNewComponent(f -> f.newCard()
                                          .withLinkLocator("news")
                                          .withTitle("News")
                                          .withContent(f2 -> f2.newButton()
                                                               .withName("Click into the card!!")
                                                               .onClick(() ->
                                                               {
                                                                   System.out.println("Clicked in the card!");
                                                               })))
                   .addNewComponent(factory -> factory.newForm()
                                                      .withDataContext((form,
                                                                        defaultDataContext) -> form.addInputField(input -> input.attachToField(defaultDataContext,
                                                                                                                                               "field1")
                                                                                                                                .withLabel("Field1:")
                                                                                                                                .withPlaceholder("Write something!!")
                                                                                                                                .withDescription("Dont tell me anything!!!"))
                                                                                                   .addButton(button -> button.withText("Submit")
                                                                                                                              .attachTo(defaultDataContext)
                                                                                                                              .onClick((data, dataContext) ->
                                                                                                                              {
                                                                                                                                  System.out.println("Form submitted: "
                                                                                                                                          + data);
                                                                                                                                  return data;
                                                                                                                              })
                                                                                                                              .saveOnClick())));
        });
    }
}
