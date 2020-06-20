package org.omnaest.react;

import javax.annotation.PostConstruct;

import org.omnaest.react.domain.Button.Style;
import org.omnaest.react.domain.Heading.Level;
import org.omnaest.react.domain.ReactUI;
import org.omnaest.react.domain.UnsortedList.Icon;
import org.omnaest.react.domain.VerticalContentSwitcher.VerticalContent.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockIMBSUI
{
    @Autowired
    private ReactUIService uiService;

    @PostConstruct
    public void init()
    {
        ReactUI reactUI = this.uiService.getOrCreateDefaultRoot();
        reactUI.withNavigationBar(navigationBar -> navigationBar.addEntry(entry -> entry.withText("Home")
                                                                                        .withLinkedLocator("home"))
                                                                .addEntry(entry -> entry.withText("News blog")
                                                                                        .withLinkedLocator("news_blog"))
                                                                .addEntry(entry -> entry.withText("Research")
                                                                                        .withLinkedLocator("research"))
                                                                .addEntry(entry -> entry.withText("Library")
                                                                                        .withLinkedLocator("library"))
                                                                .addEntry(entry -> entry.withText("Patient Information")
                                                                                        .withLinkedLocator("patient_information"))
                                                                .addEntry(entry -> entry.withText("Forum")
                                                                                        .withLink("https://www.rareconnect.org/en/community/trimethylaminuria"))
                                                                .addEntry(entry -> entry.withText("About")
                                                                                        .withLinkedLocator("about")))
               .addNewComponent(f -> f.newContainerGrid()
                                      .withLinkLocator("home")
                                      .addRowContent(f2 -> f2.newComposite()
                                                             .addNewComponent(f3 ->
                                                             {
                                                                 return f3.newContainerGrid()
                                                                          .addRow(r ->
                                                                          {
                                                                              r.addCell(c -> c.withColumnSpan(2)
                                                                                              .withContent(f4 -> f4.newImage()
                                                                                                                   .withName("IMBS logo")
                                                                                                                   .withImage("logo.png")))
                                                                               .addCell(c -> c.withColumnSpan(8)
                                                                                              .withContent(f4 -> f4.newHeading()
                                                                                                                   .withLevel(Level.H1)
                                                                                                                   .withText("Intestinal Metabolism Bromhidrosis Syndrome Alliance")))
                                                                               .addCell(c -> c.withColumnSpan(2)
                                                                                              .withContent(f4 -> f4.newButton()
                                                                                                                   .withName("Donate")
                                                                                                                   .withStyle(Style.SUCCESS)));
                                                                          });
                                                             }))
                                      .addRowContent(f2 -> f2.newComposite()
                                                             .addNewComponent(f3 ->
                                                             {
                                                                 return f3.newContainerGrid()
                                                                          .addRow(r -> r.addCell(c ->
                                                                          {
                                                                              c.withColumnSpan(8)
                                                                               .withContent(f4 -> f4.newBlockQuote()
                                                                                                    .addText("As intestinal metabolites cross the intestinal wall barrier and liver they are excreted in sweat and breath. The resulting symptoms are called Bromhidrosis and Halitosis.")
                                                                                                    .withFooter("Information for Patients"));
                                                                          }));
                                                             }))
                                      .addRowContent(f2 -> f2.newJumboTron()
                                                             .withTitle("Can you imagine a world where...?")
                                                             .addContentLeft(f3 -> f3.newUnsortedList()
                                                                                     .addText("...you go to a general practitioner and he takes you seriously and sends you to a specialist?")
                                                                                     .addText("...you could work in your job without being treated badly because of your bodies disability?")
                                                                                     .addText("...you ride a bus and people know your disability?")
                                                                                     .addText("...you meet new people and don't have to worry that they think you did not wash yourself?")
                                                                                     .addText("...you don't have to think about what failure of life you are?")
                                                                                     .addText("...you can participate in research studies to find a cure for your disease?"))
                                                             .addContentRight(f3 -> f3.newImage()
                                                                                      .withName("Crowded Bus")
                                                                                      .withImage("crowded_bus.jpg")))
                                      .addRowContent(f2 -> f2.newJumboTron()
                                                             .withTitle("How you can help?")
                                                             .addContentLeft(f3 -> f3.newUnsortedList()
                                                                                     .addText(Icon.ENVELOPE, "with leading email campaigns!")
                                                                                     .addText(Icon.DOLLAR_SIGN, "with leading crowdfunding campaigns!")
                                                                                     .addText(Icon.MICROSCOPE, "with identifying relevant researchers!")
                                                                                     .addText(Icon.HEARTBEAT, "with creating sensor components!")
                                                                                     .addText(Icon.DNA,
                                                                                              "with taking part in genetical and metabolome mapping research!"))
                                                             .addContentLeft(f3 -> f3.newButton()
                                                                                     .withName("Join us!")
                                                                                     .withStyle(Style.SUCCESS))
                                                             .addContentRight(f3 -> f3.newImage()
                                                                                      .withName("You can help!")
                                                                                      .withImage("you_can_help.jpg")))
                                      .addRowContent(f2 -> f2.newImageIndex()
                                                             .addEntry("News Blog", "news_blog", "blog.jpg")
                                                             .addEntry("Research", "research", "research.jpg")))
               .addNewComponent(f1 -> f1.newContainerGrid()
                                        .addRowContent(f2 -> f2.newCard()
                                                               .withLinkLocator("news_blog")
                                                               .withTitle("News Blog"))
                                        .addRowContent(f2 -> f2.newCard()
                                                               .withLinkLocator("patient_information")
                                                               .withTitle("Patient Information")
                                                               .withAdjustment(true)
                                                               .withContent(f3 -> f3.newImage()
                                                                                    .withImage("bromhidrosis_overview.png")
                                                                                    .withName("Bromhidrosis overview")))
                                        .addRowContent(f2 -> f2.newCard()
                                                               .withLinkLocator("research")
                                                               .withTitle("Research")
                                                               .withAdjustment(true))
                                        .addRowContent(f2 -> f2.newCard()
                                                               .withLinkLocator("library")
                                                               .withTitle("Library")
                                                               .withAdjustment(true)
                                                               .withContent(f3 -> f3.newVerticalContentSwitcher()
                                                                                    .addContentEntry(entry -> entry.withTitle("Trimethylaminuria")
                                                                                                                   .withState(State.ACTIVE)
                                                                                                                   .withContent(f4 -> f4.newTable()
                                                                                                                                        .withColumnTitles("Title",
                                                                                                                                                          "Date",
                                                                                                                                                          "Link")
                                                                                                                                        .addRow(row -> row.addCell(cell -> cell.withContent(f5 -> f5.newParagraph()
                                                                                                                                                                                                    .addText("A Review of Trimethylaminuria: (Fish Odor Syndrome)")))
                                                                                                                                                          .addCell(cell -> cell.withContent(f5 -> f5.newParagraph()
                                                                                                                                                                                                    .addText("2013")))
                                                                                                                                                          .addCell(cell -> cell.withContent(f5 -> f5.newAnker()
                                                                                                                                                                                                    .withText("PMC")
                                                                                                                                                                                                    .withLink("https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3848652/?report=reader"))))))
                                                                                    .addContentEntry(entry -> entry.withTitle("Bromhidrosis")
                                                                                                                   .withContent(f4 -> f4.newTable()
                                                                                                                                        .withColumnTitles("Title",
                                                                                                                                                          "Date",
                                                                                                                                                          "Link")
                                                                                                                                        .addRow(row -> row.addCell(cell -> cell.withContent(f5 -> f5.newParagraph()
                                                                                                                                                                                                    .addText("Nothing yet")))
                                                                                                                                                          .addCell(cell -> cell.withContent(f5 -> f5.newParagraph()
                                                                                                                                                                                                    .addText("2013")))
                                                                                                                                                          .addCell(cell -> cell.withContent(f5 -> f5.newAnker()
                                                                                                                                                                                                    .withText("PMC")
                                                                                                                                                                                                    .withLink("https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3848652/?report=reader"))))))))
                                        .addRowContent(f2 -> f2.newCard()
                                                               .withLinkLocator("partners")
                                                               .withTitle("Partners")
                                                               .withAdjustment(true)));
    }
}
