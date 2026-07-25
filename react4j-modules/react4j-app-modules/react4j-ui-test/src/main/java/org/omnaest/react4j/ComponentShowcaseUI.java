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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.domain.Alert;
import org.omnaest.react4j.domain.Badge;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.Dropdown;
import org.omnaest.react4j.domain.Modal;
import org.omnaest.react4j.domain.Offcanvas;
import org.omnaest.react4j.domain.Placeholder;
import org.omnaest.react4j.domain.Popover;
import org.omnaest.react4j.domain.Spinner;
import org.omnaest.react4j.domain.Stack;
import org.omnaest.react4j.domain.Tabs;
import org.omnaest.react4j.domain.Toaster;
import org.omnaest.react4j.domain.ToggleButton;
import org.omnaest.react4j.domain.Tooltip;
import org.omnaest.react4j.domain.UIComponent.UIContextConsumer;
import org.omnaest.react4j.domain.UIComponentFactory;
import org.omnaest.react4j.domain.context.data.Value;
import org.omnaest.react4j.domain.context.document.Document;
import org.omnaest.react4j.domain.context.document.Document.Field;
import org.omnaest.react4j.domain.support.UIComponentFactoryFunction;
import org.omnaest.react4j.service.ReactUIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Minimal, demo-quality showcase page (plan-28) exercising all 21 react-bootstrap-backed components added in prior batches
 * (plan-22/24/25/26), plus a Form/FileUpload, NavigationBar and IntervalRerenderingContainer section (plan-74). Pure
 * consumption/wiring of the frozen {@link UIComponentFactory} API - no component interface or {@code *Impl} is modified here.
 * Contributes one top-level {@link Composite} of one {@link org.omnaest.react4j.domain.Card} per component to the app's default
 * root.
 * <p>
 * <b>This IS the served page, exclusively, whenever the {@code treeTableFullWindow} Spring profile is NOT active.</b>
 * {@link MockUI} also calls {@code uiService.getOrCreateDefaultRoot(...)} against the SAME default context path, but
 * {@code ReactUIServiceImpl.getOrCreateRoot} uses {@code computeIfAbsent} - only the first-initialized {@code @Service}'s
 * consumer ever runs; the other's is silently never invoked (confirmed by direct verification, plan-74). {@link MockUI}'s
 * content (MasterDetails/ListView/Form) is therefore currently DEAD CODE with respect to the served app - do not assume it
 * renders, and do not add new showcase content there; add it here instead. Modal, Offcanvas and ToggleButton are each wrapped
 * in their own {@link org.omnaest.react4j.domain.RerenderingContainer} driven by a per-component {@link AtomicBoolean},
 * mirroring the plan-12/13/14 pattern proven in {@code DeployerUI.createConsoleProvider}.
 * <p>
 * {@code @Profile("!treeTableFullWindow")} (paired with {@link MockUI} and with {@link TreeTableFullWindowUI}'s
 * {@code @Profile("treeTableFullWindow")}) keeps this class and {@link TreeTableFullWindowUI} mutually exclusive Spring
 * beans, so the standalone full-window TreeTable showcase never races this class's own default-context-path registration
 * (see {@link TreeTableFullWindowUI}'s class javadoc for the full mechanism).
 */
@Service
@Profile("!treeTableFullWindow")
public class ComponentShowcaseUI
{
    private static final String                 NAV_TARGET_A_LOCATOR  = "nav-target-a";
    private static final String                 NAV_TARGET_B_LOCATOR  = "nav-target-b";

    @Autowired
    private ReactUIService                      uiService;

    private final AtomicBoolean                 modalVisible          = new AtomicBoolean(false);
    private final AtomicBoolean                 offcanvasVisible      = new AtomicBoolean(false);
    private final AtomicBoolean                 toggleButtonPressed   = new AtomicBoolean(false);

    /**
     * Stable {@link ByteArrayChannel} instance held across renders (required by {@link org.omnaest.react4j.component.form.upload.UploadChannel}'s usage
     * contract) so a repeat upload against the same rendered {@code uploadId} keeps working.
     */
    private final ByteArrayChannel              fileUploadChannel     = ByteArrayChannel.create();

    /**
     * Small in-memory multi-level tree (plan-76 Slice 8) held as a stable field, mirroring
     * {@link #fileUploadChannel}, so the demo tree's identity (and any provider-internal caching) survives across
     * renders instead of being rebuilt per request.
     */
    private final ShowcaseTreeTableDataProvider treeTableDataProvider = new ShowcaseTreeTableDataProvider();

    @PostConstruct
    public void init()
    {
        this.uiService.getOrCreateDefaultRoot(reactUI ->
        {
            // NavigationBar (plan-74): two anchor entries scrolling to the two "Navigation Target" cards appended below.
            // Note: React4J's NavigationBar is an in-page anchor-link mechanism (withLinkedLocator -> Card.withLinkLocator),
            // not a client-side router/content-swap - see the withLinkLocator ids on the target Cards in buildShowcase().
            reactUI.withNavigationBar(nav -> nav.addEntry(entry -> entry.withText("Navigation Target A")
                                                                        .withLinkedLocator(NAV_TARGET_A_LOCATOR))
                                                .addEntry(entry -> entry.withText("Navigation Target B")
                                                                        .withLinkedLocator(NAV_TARGET_B_LOCATOR)));
            reactUI.addNewComponent(this::buildShowcase);
        });
    }

    private Composite buildShowcase(UIComponentFactory factory)
    {
        return factory.newComposite()
                      .addComponent(factory.newHeading()
                                           .withText("Component Showcase")
                                           .withLevel(2))
                      .addComponent(factory.newCard()
                                           .withTitle("Badge")
                                           .withContent(factory.newBadge()
                                                               .withText("New")
                                                               .withStyle(Badge.Style.SUCCESS)))
                      .addComponent(factory.newCard()
                                           .withTitle("Spinner")
                                           .withContent(factory.newSpinner()
                                                               .withStyle(Spinner.Style.PRIMARY)
                                                               .withType(Spinner.Type.BORDER)))
                      .addComponent(factory.newCard()
                                           .withTitle("Placeholder")
                                           .withContent(factory.newPlaceholder()
                                                               .withStyle(Placeholder.Style.SECONDARY)
                                                               .withSize(Placeholder.Size.LG)
                                                               .withColumns(6)
                                                               .withAnimation(Placeholder.Animation.GLOW)))
                      .addComponent(factory.newCard()
                                           .withTitle("Alert")
                                           .withContent(factory.newAlert()
                                                               .withStyle(Alert.Style.INFO)
                                                               .withDismissible(true)
                                                               .withContent(factory.newParagraph()
                                                                                   .addText("This is an alert."))))
                      .addComponent(factory.newCard()
                                           .withTitle("Stack")
                                           .withContent(factory.newStack()
                                                               .withDirection(Stack.Direction.HORIZONTAL)
                                                               .withGap(2)
                                                               .withContent(factory.newParagraph()
                                                                                   .addText("Stacked content."))))
                      .addComponent(factory.newCard()
                                           .withTitle("Toaster")
                                           .withContent(factory.newToaster()
                                                               .withTitle("Notice")
                                                               .withStyle(Toaster.Style.WARNING)
                                                               .withPlacement(Toaster.Placement.TOP_END)
                                                               .withContent(factory.newParagraph()
                                                                                   .addText("A toast message."))))
                      .addComponent(factory.newCard()
                                           .withTitle("Figure")
                                           .withContent(factory.newFigure()
                                                               .withImage("demo.svg")
                                                               .withName("Demo image")
                                                               .withCaption("A demo figure.")))
                      .addComponent(factory.newCard()
                                           .withTitle("Breadcrumb")
                                           .withContent(factory.newBreadcrumb()
                                                               .addEntry(entry -> entry.withText("Home")
                                                                                       .withLink("/"))
                                                               .addEntry(entry -> entry.withText("Library")
                                                                                       .withLink("/library"))
                                                               .addEntry(entry -> entry.withText("Data")
                                                                                       .withActiveState(true))))
                      .addComponent(factory.newCard()
                                           .withTitle("Pagination")
                                           .withContent(factory.newPagination()
                                                               .addItem(item -> item.withLabel("1")
                                                                                    .withActiveState(true))
                                                               .addItem(item -> item.withLabel("2"))
                                                               .addItem(item -> item.withLabel("3")
                                                                                    .withDisabledState(true))))
                      .addComponent(factory.newCard()
                                           .withTitle("Tabs")
                                           .withContent(factory.newTabs()
                                                               .addTab(tab -> tab.withTitle("Tab 1")
                                                                                 .withState(Tabs.Tab.State.ACTIVE)
                                                                                 .withContent(factory.newParagraph()
                                                                                                     .addText("Content of tab 1.")))
                                                               .addTab(tab -> tab.withTitle("Tab 2")
                                                                                 .withContent(factory.newParagraph()
                                                                                                     .addText("Content of tab 2.")))))
                      .addComponent(factory.newCard()
                                           .withTitle("Accordion")
                                           .withContent(factory.newAccordion()
                                                               .withAlwaysOpen(false)
                                                               .addPanel(panel -> panel.withTitle("Panel 1")
                                                                                       .withExpandedState(true)
                                                                                       .withContent(factory.newParagraph()
                                                                                                           .addText("Panel 1 content.")))
                                                               .addPanel(panel -> panel.withTitle("Panel 2")
                                                                                       .withContent(factory.newParagraph()
                                                                                                           .addText("Panel 2 content.")))))
                      .addComponent(factory.newCard()
                                           .withTitle("Carousel")
                                           .withContent(factory.newCarousel()
                                                               .withInterval(5, TimeUnit.SECONDS)
                                                               .withControls(true)
                                                               .withIndicators(true)
                                                               .withFade(false)
                                                               .addSlide(slide -> slide.withImage("demo.svg")
                                                                                       .withName("Slide 1")
                                                                                       .withCaption("First slide."))
                                                               .addSlide(slide -> slide.withImage("demo.svg")
                                                                                       .withName("Slide 2")
                                                                                       .withCaption("Second slide."))))
                      .addComponent(factory.newCard()
                                           .withTitle("Tooltip")
                                           .withContent(factory.newTooltip()
                                                               .withText("A helpful tooltip.")
                                                               .withPlacement(Tooltip.Placement.TOP)
                                                               .withContent(factory.newButton()
                                                                                   .withName("Hover me"))))
                      .addComponent(factory.newCard()
                                           .withTitle("Popover")
                                           .withContent(factory.newPopover()
                                                               .withTitle("Popover title")
                                                               .withBody(factory.newParagraph()
                                                                                .addText("Popover body content."))
                                                               .withPlacement(Popover.Placement.RIGHT)
                                                               .withTrigger(Popover.Trigger.CLICK)
                                                               .withContent(factory.newButton()
                                                                                   .withName("Click me"))))
                      .addComponent(factory.newCard()
                                           .withTitle("Collapse")
                                           .withContent(factory.newCollapse()
                                                               .withToggleLabel("Toggle details")
                                                               .withInitiallyOpen(false)
                                                               .withContent(factory.newParagraph()
                                                                                   .addText("Collapsible content."))))
                      .addComponent(factory.newCard()
                                           .withTitle("Dropdown")
                                           .withContent(factory.newDropdown()
                                                               .withTitle("Options")
                                                               .withStyle(Dropdown.Style.PRIMARY)
                                                               .withPresentation(Dropdown.Presentation.BUTTON)
                                                               .withDrop(Dropdown.Drop.DOWN)
                                                               .addHeader("Actions")
                                                               .addItem(item -> item.withText("Action 1"))
                                                               .addItem(item -> item.withText("Action 2"))
                                                               .addDivider()
                                                               .addItem(item -> item.withText("Disabled action")
                                                                                    .withDisabledState(true))))
                      .addComponent(factory.newCard()
                                           .withTitle("SplitButton")
                                           .withContent(factory.newSplitButton()
                                                               .withText("Save")
                                                               .withStyle(Dropdown.Style.SUCCESS)
                                                               .addHeader("Save options")
                                                               .addItem(item -> item.withText("Save as..."))
                                                               .addDivider()
                                                               .addItem(item -> item.withText("Save a copy"))))
                      .addComponent(factory.newCard()
                                           .withTitle("Modal")
                                           .withContent(factory.newRerenderingContainer()
                                                               .enableStaticNodeRerendering()
                                                               .withContent(this.createModalProvider())))
                      .addComponent(factory.newCard()
                                           .withTitle("Offcanvas")
                                           .withContent(factory.newRerenderingContainer()
                                                               .enableStaticNodeRerendering()
                                                               .withContent(this.createOffcanvasProvider())))
                      .addComponent(factory.newCard()
                                           .withTitle("ToggleButton")
                                           .withContent(factory.newRerenderingContainer()
                                                               .enableStaticNodeRerendering()
                                                               .withContent(this.createToggleButtonProvider())))
                      // --- plan-74: additions below are APPENDED, existing sections/order above are untouched ---
                      .addComponent(factory.newCard()
                                           .withLinkLocator(NAV_TARGET_A_LOCATOR)
                                           .withTitle("Navigation Target A")
                                           .withContent(factory.newParagraph()
                                                               .addText("Scrolled here via the NavigationBar's first entry.")))
                      .addComponent(factory.newCard()
                                           .withLinkLocator(NAV_TARGET_B_LOCATOR)
                                           .withTitle("Navigation Target B")
                                           .withContent(factory.newParagraph()
                                                               .addText("Scrolled here via the NavigationBar's second entry.")))
                      .addComponent(factory.newCard()
                                           .withTitle("Form (text input + file upload)")
                                           .withContent(factory.newForm()
                                                               .withUIContext(this.createFormProvider())))
                      .addComponent(factory.newCard()
                                           .withTitle("IntervalRerenderingContainer")
                                           .withContent(factory.newIntervalRerenderingContainer()
                                                               .withIntervalDuration(2, TimeUnit.SECONDS)
                                                               .withRefreshedContent(() -> factory.newParagraph()
                                                                                                  .addText("Server time: "
                                                                                                           + LocalDateTime.now()
                                                                                                                          .format(DateTimeFormatter.ofPattern("HH:mm:ss"))))))
                      // --- plan-76 Slice 8: TreeTable demo, in-memory multi-level provider (ShowcaseTreeTableDataProvider) ---
                      .addComponent(factory.newCard()
                                           .withTitle("TreeTable")
                                           .withContent(factory.newTreeTable()
                                                               .withColumns(TreeTableColumn.of("name", "Name"), TreeTableColumn.of("kind", "Kind"))
                                                               .withDataProvider(this.treeTableDataProvider)
                                                               .withWindowSize(3)));
    }

    /**
     * Server-driven show/hide (plan-12/13/14 pattern): a trigger {@link org.omnaest.react4j.domain.Button} flips
     * {@link #modalVisible} and the {@link Modal} itself renders from that same server-side flag, closing it back via
     * {@link Modal#onClose(org.omnaest.react4j.service.internal.handler.domain.EventHandler)}.
     */
    private UIComponentFactoryFunction createModalProvider()
    {
        return factory -> factory.newComposite()
                                 .addComponent(factory.newButton()
                                                      .withName("Open modal")
                                                      .onClick(() -> this.modalVisible.set(true)))
                                 .addComponent(factory.newModal()
                                                      .withTitle("Demo Modal")
                                                      .withVisible(this.modalVisible.get())
                                                      .withSize(Modal.Size.LARGE)
                                                      .withCentered(true)
                                                      .onClose(() -> this.modalVisible.set(false))
                                                      .withContent(factory.newParagraph()
                                                                          .addText("Modal body content.")));
    }

    /**
     * Server-driven show/hide (plan-12/13/14 pattern), analogous to {@link #createModalProvider()} but for {@link Offcanvas}.
     */
    private UIComponentFactoryFunction createOffcanvasProvider()
    {
        return factory -> factory.newComposite()
                                 .addComponent(factory.newButton()
                                                      .withName("Open offcanvas")
                                                      .onClick(() -> this.offcanvasVisible.set(true)))
                                 .addComponent(factory.newOffcanvas()
                                                      .withTitle("Demo Offcanvas")
                                                      .withVisible(this.offcanvasVisible.get())
                                                      .withPlacement(Offcanvas.Placement.END)
                                                      .onClose(() -> this.offcanvasVisible.set(false))
                                                      .withContent(factory.newParagraph()
                                                                          .addText("Offcanvas body content.")));
    }

    /**
     * Server-driven pressed state (plan-12/13/14 pattern): the {@link ToggleButton} renders {@link #toggleButtonPressed} and
     * flips it on every {@link ToggleButton#onChange(org.omnaest.react4j.service.internal.handler.domain.EventHandler)}.
     */
    private UIComponentFactoryFunction createToggleButtonProvider()
    {
        return factory -> factory.newToggleButton()
                                 .withText("Toggle me")
                                 .withStyle(ToggleButton.Style.PRIMARY)
                                 .withPressed(this.toggleButtonPressed.get())
                                 .onChange(() -> this.toggleButtonPressed.set(!this.toggleButtonPressed.get()));
    }

    /**
     * Builds a {@link Form} with one text {@code InputFormElement} plus one {@code FileUploadFormElement} bound to the stable
     * {@link #fileUploadChannel}, and a submit {@code Button} that triggers a normal {@code /ui/event} server round-trip.
     * <p>
     * {@code form.attachTo(document)} follows the proven shape used by {@code FileUploadEndToEndTest} (react4j-core) - required so
     * {@code FormRendererImpl.getEffectiveContext()} does not NPE on a null {@code dataContext} at render time.
     */
    private UIContextConsumer<Form> createFormProvider()
    {
        return (form, uiContext) ->
        {
            Document document = uiContext.getFirstDocument();
            Field nameField = document.getField("showcaseNameField");

            form.attachTo(document);
            form.addInputField(input -> input.attachToField(nameField)
                                             .withLabel("Name:")
                                             .withPlaceholder("Enter your name"));
            form.addFileUpload(fileUpload -> fileUpload.withUploadChannel(this.fileUploadChannel)
                                                       .withAccept(".txt,.png,.jpg"));
            form.addButton(button -> button.withText("Submit")
                                           .onClick((data, context) ->
                                           {
                                               data.getFieldValue(nameField)
                                                   .map(Value::asString)
                                                   .ifPresent(name -> System.out.println("Showcase form submitted: " + name));
                                               return data;
                                           }));
        };
    }
}
