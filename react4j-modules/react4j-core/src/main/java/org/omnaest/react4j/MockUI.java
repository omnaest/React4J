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
package org.omnaest.react4j;

import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

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
                   .addNewComponent(f -> f.newGridContainer()
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
            //                   .addNewComponent(factory -> factory.newForm()
            //                                                      .withDataContext((form,
            //                                                                        defaultDataContext) -> form.addInputField(input -> input.attachToField(defaultDataContext,
            //                                                                                                                                               "field1")
            //                                                                                                                                .withLabel("Field1:")
            //                                                                                                                                .withPlaceholder("Write something!!")
            //                                                                                                                                .withDescription("Dont tell me anything!!!"))
            //                                                                                                   .addButton(button -> button.withText("Submit")
            //                                                                                                                              .attachTo(defaultDataContext)
            //                                                                                                                              .onClick((data, dataContext) ->
            //                                                                                                                              {
            //                                                                                                                                  System.out.println("Form submitted: "
            //                                                                                                                                          + data);
            //                                                                                                                                  return data;
            //                                                                                                                              })
            //                                                                                                                              .saveOnClick()))
            //                                                      )
            ;
        });
    }
}
