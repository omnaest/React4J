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

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.form.Form.CheckboxFormElement.CheckboxType;
import org.omnaest.react4j.component.form.Form.FormElement.ColumnSpan;
import org.omnaest.react4j.component.form.Form.ValidationMessageType;
import org.omnaest.react4j.domain.UIComponent.UIContextAndDataConsumer;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class MockUI
{
    @Autowired
    private ReactUIService uiService;

    @PostConstruct
    public void init()
    {
        this.uiService.getOrCreateDefaultRoot(reactUI ->
        {

            reactUI.addNewComponent(factory -> factory.newCard()
                                                      .withContent(factory.newForm()
                                                                          .withUIContext(this.newForm())));
            //            .withNavigationBar(navigationBar -> navigationBar.addEntry(entry -> entry.withText("News")
            //                                                                                            .withLinkedLocator("news")))
            //                   .addNewComponent(factory -> factory.newParagraph()
            //                                                      .addText("I love you!"))
            //                   .addNewComponent(factory -> factory.newButton()
            //                                                      .withName("Click to love me back!"))
            //                   .addNewComponent(f -> f.newAnker()
            //                                          .withText("Some link")
            //                                          .withLink("http://www.wikipedia.de"))
            //                   .addNewComponent(f -> f.newBlockQuote()
            //                                          .addText("This is a test")
            //                                          .withFooter("Footer!!!"))
            //                   .addNewComponent(factory -> factory.newTable()
            //                                                      .withColumnTitles("Column1", "Column2")
            //                                                      .addRow(row -> row.addCell(cell -> cell.withContent(factory.newParagraph()
            //                                                                                                                 .addText("Cell1")))
            //                                                                        .addCell(cell -> cell.withContent(factory.newParagraph()
            //                                                                                                                 .addText("Cell2")))))
            //                   .addNewComponent(factory -> factory.newComposite()
            //                                                      .addComponent(factory.newAnker()
            //                                                                           .withText("Some anker"))
            //                                                      .addComponent(factory.newButton()
            //                                                                           .withName("Some button")))
            //                   .addNewComponent(f -> f.newGridContainer()
            //                                          .addRow(row -> row.addCell(cell -> cell.withContent(f2 -> f2.newParagraph()
            //                                                                                                      .addText("Cell1")))
            //                                                            .addCell(cell -> cell.withContent(f2 -> f2.newParagraph()
            //                                                                                                      .addText("Cell2")))))
            //                   .addNewComponent(f -> f.newCard()
            //                                          .withLinkLocator("news")
            //                                          .withTitle("News")
            //                                          .withContent(f2 -> f2.newButton()
            //                                                               .withName("Click into the card!!")
            //                                                               .onClick(() ->
            //                                                               {
            //                                                                   System.out.println("Clicked in the card!");
            //                                                               })))
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

    private UIContextAndDataConsumer<Form> newForm()
    {
        return (form, context, initialData) ->
        {
            Document document = context.getFirstDocument();
            Field nameField = document.getField("nameField");
            Field descriptionField = document.getField("descriptionField");
            Field rangefield = document.getField("rangeField");
            Field dropDownfield = document.getField("dropDownField");
            Field switchField = document.getField("switchField");

            initialData.setFieldValue(descriptionField, "Hello there!!!");
            initialData.setFieldValue(rangefield, 1);

            form.addInputField(input -> input.attachToField(nameField)
                                             .withColumnSpan(4)
                                             .withLabel("Name:"))
                .addInputField(input -> input.attachToField(descriptionField)
                                             .withColumnSpan(8)
                                             .withLabel("Description:"))
                .addInputField(input -> input.withLabel("Category:"))
                .addCheckbox(checkbox -> checkbox.attachToField(switchField)
                                                 .withType(CheckboxType.SWITCH)
                                                 .withLabel("Toggle:")
                                                 .withDescription("Description")
                                                 .withInitialValue(true)
                                                 .withDisabled(false))
                .addRange(range -> range.attachToField(rangefield)
                                        .withColumnSpan(ColumnSpan.TWELVE_COLUMNS)
                                        .withInitialValue(1)
                                        .withLabel("Range:")
                                        .withMax(4)
                                        .withStep(1)
                                        .withDisabled(false))
                .addDropdown(dropDown -> dropDown.attachToField(dropDownfield)
                                                 .withLabel("Select:")
                                                 .withColumnSpan(12 - 3)
                                                 //                                                 .withMultiselectSupport()
                                                 .withOptions(options -> options.addOption("1", "label 1")
                                                                                .addOption("2", "label 2")
                                                                                .addDisabledOption("3", "label 3")
                                                                                .addOptions(IntStream.range(4, 100)
                                                                                                     .boxed()
                                                                                                     .collect(Collectors.toMap(i -> "" + i, i -> "label " + i,
                                                                                                                               (a, b) -> a,
                                                                                                                               LinkedHashMap::new)))))
                .addButton(button -> button.withText("Save")
                                           .withColumnSpan(3)
                                           .onClick((data, messaging, context1) ->
                                           {
                                               data.getFieldValue(nameField)
                                                   .map(Value::asString)
                                                   .filter(StringUtils::isNotBlank)
                                                   .ifPresentOrElse(value ->
                                                   {
                                                       messaging.addValidationMessage(nameField, ValidationMessageType.VALID, "Yes! Yes!");
                                                       System.out.println(value);
                                                   }, () ->
                                                   {
                                                       messaging.addValidationMessage(nameField, "Omg! no! Enter at least something!");
                                                       data.setFieldValue(nameField, "Something!!");
                                                   });
                                               messaging.addValidationMessage(descriptionField, ValidationMessageType.VALID, "Yes!");

                                               messaging.addValidationMessage(switchField, ValidationMessageType.VALID, "Yes1!!")
                                                        .addValidationMessage(switchField, ValidationMessageType.VALID, "Yes2!!");

                                               data.getFieldValue(dropDownfield)
                                                   .map(Value::asStringList)
                                                   .ifPresentOrElse(System.out::println, () ->
                                                   {
                                                       messaging.addValidationMessage(dropDownfield, "Omg! no! Select at least something!");
                                                       data.setFieldValue(dropDownfield, "10");
                                                   });

                                               return data;
                                           }))
                .addInputField(input -> input.withLabel("Another category:")
                                             .withColumnSpan(6));
        };
    }
}
