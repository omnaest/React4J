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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.omnaest.react4j.component.form.Form;
import org.omnaest.react4j.component.form.upload.ByteArrayChannel;
import org.omnaest.react4j.component.treetable.provider.TreeTableColumn;
import org.omnaest.react4j.domain.Alert;
import org.omnaest.react4j.domain.Badge;
import org.omnaest.react4j.domain.Composite;
import org.omnaest.react4j.domain.DropTarget;
import org.omnaest.react4j.domain.DropTarget.DropEvent;
import org.omnaest.react4j.domain.DropTarget.DropRelation;
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
import org.omnaest.react4j.domain.UIComponent;
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
    private static final String                 NAV_TARGET_A_LOCATOR      = "nav-target-a";
    private static final String                 NAV_TARGET_B_LOCATOR      = "nav-target-b";

    @Autowired
    private ReactUIService                      uiService;

    private final AtomicBoolean                 modalVisible              = new AtomicBoolean(false);
    private final AtomicBoolean                 offcanvasVisible          = new AtomicBoolean(false);
    private final AtomicBoolean                 toggleButtonPressed       = new AtomicBoolean(false);

    /**
     * Stable {@link ByteArrayChannel} instance held across renders (required by {@link org.omnaest.react4j.component.form.upload.UploadChannel}'s usage
     * contract) so a repeat upload against the same rendered {@code uploadId} keeps working.
     */
    private final ByteArrayChannel              fileUploadChannel         = ByteArrayChannel.create();

    /**
     * Small in-memory multi-level tree (plan-76 Slice 8) held as a stable field, mirroring
     * {@link #fileUploadChannel}, so the demo tree's identity (and any provider-internal caching) survives across
     * renders instead of being rebuilt per request.
     */
    private final ShowcaseTreeTableDataProvider treeTableDataProvider     = new ShowcaseTreeTableDataProvider();

    /**
     * plan-235 S3: genuine server-side drag-and-drop demo state (AC-BROWSER-1..5/7). {@code dragDropCardLabel}
     * holds each card's stable display text; {@code dragDropContainerChildren} holds, per container id, the
     * ORDERED list of card ids it directly owns - a container id is either a list root ({@code "A"}/{@code "B"})
     * or another card's own id (a card that has been dropped ONTO another - {@link DropRelation#INTO} - becomes a
     * child of that card, one level of nesting, rendered indented under it). Mutated only from
     * {@link #handleDragDropEvent(String, DropEvent)}, which every demo {@link DropTarget} card shares.
     */
    private final Map<String, String>           dragDropCardLabel         = new ConcurrentHashMap<>();
    private final Map<String, List<String>>     dragDropContainerChildren = new ConcurrentHashMap<>();

    /**
     * plan-235 S3 AC-BROWSER-6: bumped by the {@link org.omnaest.react4j.domain.Button} added inside the
     * IntervalRerenderingContainer card's refreshed content, alongside the pre-existing "Server time" paragraph -
     * proves the interval-wrapped subtree still handles a click while it keeps ticking on its own timer.
     */
    private final AtomicInteger                 intervalClickCount        = new AtomicInteger(0);

    /**
     * plan-262 S1: bumped by the {@code Breadcrumb} demo's "Library" entry {@code onClick} - demonstrates the
     * new per-entry server-side click handler alongside the existing link-only entries.
     */
    private final AtomicInteger                 breadcrumbClickCount      = new AtomicInteger(0);

    @PostConstruct
    public void init()
    {
        this.initDragDropDemoState();
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
                                                                                       .withLink("/library")
                                                                                       .onClick(() -> this.breadcrumbClickCount.incrementAndGet()))
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
                                                               .withRefreshedContent(() -> this.createIntervalContent(factory))))
                      // --- plan-235 S3: drag-and-drop demo, genuine server-side state (AC-BROWSER-1..5/7) ---
                      .addComponent(factory.newCard()
                                           .withTitle("Drag and Drop")
                                           .withContent(factory.newRerenderingContainer()
                                                               .enableStaticNodeRerendering()
                                                               .withContent(this.createDragDropProvider())))
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

    /**
     * plan-235 S3 AC-BROWSER-6: the interval-wrapped subtree's refreshed content, extended beyond the original
     * bare "Server time" {@link org.omnaest.react4j.domain.Paragraph} (plan-74) with ONE clickable
     * {@link org.omnaest.react4j.domain.Button} - the previous showcase wiring had no clickable child inside the
     * IntervalRerenderingContainer card at all, so there was nothing there to exercise the click half of this
     * criterion. The button's own visible text carries {@link #intervalClickCount} so a browser test can observe
     * the click's effect without a second, colliding paragraph inside {@code .card-inner-body} (the sibling
     * {@code IntervalRerenderingContainerLiveUpdateIT} locates its "Server time" text via
     * {@code .card-inner-body p}, which must keep matching EXACTLY one element).
     */
    private Composite createIntervalContent(UIComponentFactory factory)
    {
        return factory.newComposite()
                      .addComponent(factory.newParagraph()
                                           .addText("Server time: "
                                                    + LocalDateTime.now()
                                                                   .format(DateTimeFormatter.ofPattern("HH:mm:ss"))))
                      .addComponent(factory.newButton()
                                           .withName("Bump Interval Counter (" + this.intervalClickCount.get() + ")")
                                           .onClick(() -> this.intervalClickCount.incrementAndGet()));
    }

    /**
     * plan-235 S3: seeds the drag-and-drop demo's server-side state (called once from {@code init()}, before the
     * page is ever rendered). Each AC-BROWSER scenario gets its OWN dedicated pair/trio of cards so the seven
     * acceptance tests - which all run against the SAME long-lived {@code @Service} instance across the whole IT
     * class, since Spring caches the context across {@code @Test} methods by default - never mutate state another
     * test method depends on, regardless of JUnit's (unspecified) method execution order.
     * <ul>
     * <li>{@code card-x1} (list A) / {@code card-x2} (list B) - AC-BROWSER-1/5 (cross-list drag).</li>
     * <li>{@code card-y1} / {@code card-y2} (both list A) - AC-BROWSER-2 (reorder).</li>
     * <li>{@code card-z1} / {@code card-zt1} (both list A) - AC-BROWSER-3 (reparent, z1 dropped ONTO zt1).</li>
     * <li>{@code card-w1}/{@code card-w2}/{@code card-w3} (list A) - AC-BROWSER-4 (two consecutive drags).</li>
     * <li>{@code card-k1} (list A) / {@code card-k2} (list B) - AC-BROWSER-7 (keyboard pick-up/drop).</li>
     * <li>{@code card-n1-src} (list A) / {@code card-n1-target} (list C) - plan-235 S1 corrective round 2
     * AC-R4(i): a drop onto an item nested inside a LIVE list-level {@link DropTarget} must route to the
     * item only.</li>
     * <li>{@code card-n2-src} (list A) / list D (starts empty) - AC-R4(ii): a drop onto a live list-level
     * {@link DropTarget}'s own empty space (no item to aim at) must route to the list itself.</li>
     * </ul>
     */
    private void initDragDropDemoState()
    {
        this.dragDropCardLabel.put("card-x1", "Card X1");
        this.dragDropCardLabel.put("card-x2", "Card X2");
        this.dragDropCardLabel.put("card-y1", "Card Y1");
        this.dragDropCardLabel.put("card-y2", "Card Y2");
        this.dragDropCardLabel.put("card-z1", "Card Z1");
        this.dragDropCardLabel.put("card-zt1", "Card ZT1");
        this.dragDropCardLabel.put("card-w1", "Card W1");
        this.dragDropCardLabel.put("card-w2", "Card W2");
        this.dragDropCardLabel.put("card-w3", "Card W3");
        this.dragDropCardLabel.put("card-k1", "Card K1");
        this.dragDropCardLabel.put("card-k2", "Card K2");
        this.dragDropCardLabel.put("card-n1-src", "Card N1 Src");
        this.dragDropCardLabel.put("card-n1-target", "Card N1 Target");
        this.dragDropCardLabel.put("card-n2-src", "Card N2 Src");

        this.dragDropContainerChildren.put("A",
                                           new CopyOnWriteArrayList<>(List.of("card-x1", "card-y1", "card-y2", "card-z1", "card-zt1", "card-w1", "card-w2",
                                                                              "card-w3", "card-k1", "card-n1-src", "card-n2-src")));
        this.dragDropContainerChildren.put("B", new CopyOnWriteArrayList<>(List.of("card-x2", "card-k2")));
        this.dragDropContainerChildren.put("C", new CopyOnWriteArrayList<>(List.of("card-n1-target")));
        this.dragDropContainerChildren.put("D", new CopyOnWriteArrayList<>());
    }

    /**
     * plan-235 S3 AC-BROWSER-5: BOTH lists as siblings under this ONE returned component tree, which is the
     * single {@code UIComponentFactoryFunction} wired into ONE {@code RerenderingContainer} in
     * {@link #buildShowcase(UIComponentFactory)} - so a single {@code /ui/event} response can update both lists'
     * rendered content at once.
     * <p>
     * The two lists sit side by side in a {@link Stack#withDirection(Stack.Direction) HORIZONTAL} {@link Stack} -
     * deliberately NOT a bare {@link Composite}: the React client's {@code Composite.tsx} renders a
     * {@code React.Fragment} (no wrapping DOM element at all - verified at source, plan-235 S3), so a browser test
     * has nothing to scope a "this list's cards" locator to. {@link Stack} renders a real react-bootstrap
     * {@code <div>} (its {@code BSStack}), which is what {@link #renderDragDropList} relies on to give each list a
     * genuine, locatable DOM container.
     * <p>
     * Lists C and D (plan-235 S1 corrective round 2, AC-R4) are the SAME shape but each is ALSO itself a live
     * {@link DropTarget} - see {@link #renderNestedDropTargetList}. Kept as separate lists from A/B rather than
     * retrofitting A/B, so every pre-existing AC-BROWSER-1..5/7 scenario's DOM shape (and locators) stay
     * byte-for-byte unchanged.
     */
    private UIComponentFactoryFunction createDragDropProvider()
    {
        return factory -> factory.newStack()
                                 .withDirection(Stack.Direction.HORIZONTAL)
                                 .withGap(4)
                                 .withContent(factory.newComposite()
                                                     .addComponent(this.renderDragDropList(factory, "A", "List A"))
                                                     .addComponent(this.renderDragDropList(factory, "B", "List B"))
                                                     .addComponent(this.renderNestedDropTargetList(factory, "C", "List C"))
                                                     .addComponent(this.renderNestedDropTargetList(factory, "D", "List D")));
    }

    /**
     * plan-235 S1 corrective round 2 (Defect B, AC-R4). Identical shape to {@link #renderDragDropList} except
     * the whole list is ALSO wrapped in its OWN live {@link DropTarget}, whose {@code onDrop} routes to
     * {@link #handleDragDropEvent} with {@code targetCardId = listId} - now that {@code DropTarget.tsx} stops
     * propagation once it genuinely consumes a drop (Defect A/B fix), this is safe to nest around the
     * item-level {@link DropTarget}s without a single physical drop double-firing both handlers:
     * <ul>
     * <li>A drop that lands ON an item element fires ONLY that item's {@code onDrop} (the item-level handler
     * consumes the event and stops propagation before it ever reaches this outer one).</li>
     * <li>A drop that lands on the list's OWN empty space (no item element under the pointer) never reaches any
     * item-level handler at all, so it is this outer handler that fires - {@link DropRelation#INTO} on a
     * {@code targetCardId} that is a list id (not a card id) adds the dragged card as a new child of that list
     * container directly, via the SAME {@link #handleDragDropEvent} clause item-level INTO drops already use.
     * This is the capability AC-BROWSER-3-style item-only targets structurally cannot offer: a list emptied of
     * its last card has no item left to aim a drop at.</li>
     * </ul>
     */
    private UIComponent<?> renderNestedDropTargetList(UIComponentFactory factory, String listId, String heading)
    {
        Composite content = factory.newComposite()
                                   .addComponent(factory.newHeading()
                                                        .withText(heading)
                                                        .withLevel(4));
        for (String cardId : this.dragDropContainerChildren.getOrDefault(listId, List.of()))
        {
            content.addComponent(this.renderDragDropCard(factory, cardId));
        }
        return factory.newDropTarget()
                      .withContent(factory.newStack()
                                          .withDirection(Stack.Direction.VERTICAL)
                                          .withGap(2)
                                          .withContent(content))
                      .onDrop(dropEvent -> this.handleDragDropEvent(listId, dropEvent));
    }

    /**
     * Real wrapping {@code <div>} per list (see {@link #createDragDropProvider()}'s javadoc for why a bare
     * {@link Composite} cannot serve this role) - its heading and cards are DIRECT children of this
     * {@link Stack}'s own rendered div, since the {@link Composite} passed as its content is itself a
     * pass-through {@code React.Fragment} on the client.
     */
    private UIComponent<?> renderDragDropList(UIComponentFactory factory, String listId, String heading)
    {
        Composite content = factory.newComposite()
                                   .addComponent(factory.newHeading()
                                                        .withText(heading)
                                                        .withLevel(4));
        for (String cardId : this.dragDropContainerChildren.getOrDefault(listId, List.of()))
        {
            content.addComponent(this.renderDragDropCard(factory, cardId));
        }
        return factory.newStack()
                      .withDirection(Stack.Direction.VERTICAL)
                      .withGap(2)
                      .withContent(content);
    }

    /**
     * Every demo card is a {@link DropTarget} wrapping a {@link org.omnaest.react4j.domain.Draggable} (Cliff 4:
     * composable with any content) - the OUTER {@link DropTarget} is what receives a drop (its own bounds drive
     * {@code relationForPointer}), the INNER {@link org.omnaest.react4j.domain.Draggable} is what a drag starts
     * from, carrying the card's own id as {@code dragId}.
     * <p>
     * <b>Historical note (plan-235 S1, corrective round 2 of 2 fixed this):</b> lists A/B are deliberately NOT
     * also wrapped in an outer, list-level {@link DropTarget} here. That used to be load-bearing - before this
     * round, {@code DropTarget.tsx} never called {@code stopPropagation()} on {@code drop}/{@code dragover}, so
     * nesting a second LIVE {@link DropTarget} as an ancestor of these item-level ones made a single physical
     * drop bubble into BOTH handlers. That propagation defect is now fixed (see {@code DropTarget.tsx}'s
     * {@code handleDrop}/{@code handleKeyDown}, which stop propagation only once they genuinely consume the
     * event) and proven safe by lists C/D ({@link #renderNestedDropTargetList}, AC-R4). Lists A/B are kept
     * unwrapped regardless, so every pre-existing AC-BROWSER-1..5/7 scenario's DOM shape stays unchanged rather
     * than being retrofitted for no behavioural gain - every one of those scenarios targets a specific,
     * already-present card in a non-empty list, so an item-level target was always sufficient for them.
     */
    private UIComponent<?> renderDragDropCard(UIComponentFactory factory, String cardId)
    {
        Composite content = factory.newComposite()
                                   .addComponent(factory.newParagraph()
                                                        .addText(this.dragDropCardLabel.get(cardId)));
        List<String> childIds = this.dragDropContainerChildren.get(cardId);
        if (childIds != null && !childIds.isEmpty())
        {
            Composite children = factory.newComposite();
            for (String childId : childIds)
            {
                children.addComponent(this.renderDragDropCard(factory, childId));
            }
            content.addComponent(children);
        }
        return factory.newDropTarget()
                      .withContent(factory.newDraggable()
                                          .withDragId(cardId)
                                          .withContent(content))
                      .onDrop(dropEvent -> this.handleDragDropEvent(cardId, dropEvent));
    }

    /**
     * Shared move logic for every demo card's {@code onDrop} (plan-235 S3). {@link DropRelation#BEFORE}/
     * {@link DropRelation#AFTER} reposition the dragged card as a sibling of the target, in the target's OWN
     * container; {@link DropRelation#INTO} reparents the dragged card to become a child of the target
     * (AC-BROWSER-3). Synchronized because a move is a remove-then-insert across two container lists that must be
     * observed atomically by the next render.
     */
    private synchronized void handleDragDropEvent(String targetCardId, DropEvent event)
    {
        String dragId = event.getDragId();
        if (dragId == null || dragId.equals(targetCardId) || !this.dragDropCardLabel.containsKey(dragId))
        {
            return;
        }
        this.removeFromCurrentDragDropContainer(dragId);
        if (event.getRelation() == DropRelation.INTO)
        {
            this.dragDropContainerChildren.computeIfAbsent(targetCardId, key -> new CopyOnWriteArrayList<>())
                                          .add(dragId);
        }
        else
        {
            String targetContainerId = this.findDragDropContainerOf(targetCardId);
            if (targetContainerId != null)
            {
                List<String> siblings = this.dragDropContainerChildren.computeIfAbsent(targetContainerId, key -> new CopyOnWriteArrayList<>());
                int targetIndex = siblings.indexOf(targetCardId);
                int insertAt = event.getRelation() == DropRelation.BEFORE ? targetIndex : targetIndex + 1;
                siblings.add(Math.max(0, insertAt), dragId);
            }
        }
    }

    private void removeFromCurrentDragDropContainer(String cardId)
    {
        this.dragDropContainerChildren.values()
                                      .forEach(children -> children.remove(cardId));
    }

    private String findDragDropContainerOf(String cardId)
    {
        return this.dragDropContainerChildren.entrySet()
                                             .stream()
                                             .filter(entry -> entry.getValue()
                                                                   .contains(cardId))
                                             .map(Map.Entry::getKey)
                                             .findFirst()
                                             .orElse(null);
    }
}
