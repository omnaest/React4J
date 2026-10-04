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
import org.omnaest.react4j.domain.Icon;
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
import org.omnaest.react4j.domain.UIComponent.UIContextAndDataConsumer;
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
    public static final String                  NAV_TARGET_A_LOCATOR                         = "nav-target-a";
    public static final String                  NAV_TARGET_B_LOCATOR                         = "nav-target-b";

    /**
     * plan-265 S0: three addressable {@link org.omnaest.react4j.domain.DiagramViewer} showcase fixtures -
     * before this slice {@code newDiagramViewer} appeared nowhere in {@code react4j-ui-test} (verified:
     * zero occurrences across its whole {@code src} tree, main and test), so there was nothing to point a
     * browser at. Each card's TITLE is the handle a test addresses it by - a deliberate contract, not an
     * incidental selector: this class and {@code browser.DiagramViewerZoomOverflowIT} are both compiled in
     * this SAME Maven module (main and test sources share one compile scope), so the IT references these
     * constants directly rather than a hand-duplicated string literal that could silently drift out of
     * sync. Two clearly different aspect ratios (roughly 120x900 and 900x120) are used deliberately - a
     * square fixture could not distinguish a scroll axis that was wired from one that was never touched.
     */
    public static final String                  DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE        = "DiagramViewer Tall Narrow (interactive)";
    public static final String                  DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE          = "DiagramViewer Wide Flat (interactive)";
    public static final String                  DIAGRAM_VIEWER_NON_INTERACTIVE_CARD_TITLE    = "DiagramViewer Thumbnail (non-interactive)";

    /**
     * plan-266 Cliff N4 / AC-N11 - the card's own fixture pair (tall-narrow/wide-flat) exercises only ONE
     * branch of the cover mechanism: each violates exactly one of the two CSS minima, because each is
     * smaller than the host on one axis and larger on the other. The falsified N4-A candidate was exact on
     * that branch and wrong by 3x on the other: "larger than the host on BOTH axes", which is also the
     * common case for real Mermaid output. These four fixtures span all four intrinsic-size relationships a
     * diagram can have against its host (smaller than host on both axes / larger on both / larger in width
     * only / larger in height only), so AC-N11 in {@code DiagramViewerZoomOverflowIT} can run AC-N1/N3/N5/
     * N10 across all four rather than assuming the existing pair generalises.
     */
    public static final String                  DIAGRAM_VIEWER_SMALLER_BOTH_AXES_CARD_TITLE  = "DiagramViewer Smaller Than Host On Both Axes (interactive)";
    public static final String                  DIAGRAM_VIEWER_LARGER_BOTH_AXES_CARD_TITLE   = "DiagramViewer Larger Than Host On Both Axes (interactive)";
    public static final String                  DIAGRAM_VIEWER_LARGER_WIDTH_ONLY_CARD_TITLE  = "DiagramViewer Larger Than Host In Width Only (interactive)";
    public static final String                  DIAGRAM_VIEWER_LARGER_HEIGHT_ONLY_CARD_TITLE = "DiagramViewer Larger Than Host In Height Only (interactive)";

    /**
     * plan-266 AC-N7 - "where the diagram's aspect equals the host's, Whole diagram and Fit produce identical
     * geometry". An explicit, non-percentage {@code withWidth}/{@code withHeight} pins the DIAGRAM'S OWN box
     * to a fixed, page-layout-independent size, so the host's content-box aspect is a constant this fixture
     * can be authored to match, rather than a number that depends on Bootstrap column width. See
     * {@code DiagramViewerZoomOverflowIT} for the measured host box this viewBox was calibrated against.
     */
    public static final String                  DIAGRAM_VIEWER_SQUARE_ASPECT_CARD_TITLE      = "DiagramViewer Aspect Equals Host (interactive)";

    /**
     * plan-266 Cliff N4, AC-N1/N2/N3/N4/N5/N9/N10 - a DEDICATED, explicit-height pair, deliberately separate
     * from {@link #DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE}/{@link #DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE}.
     * MEASURED finding (not assumed): cover's "min-height: calc(100% * scale)" can only resolve against a
     * DEFINITE ancestor height. The two S0/S1 fixtures above are deliberately auto-height (no
     * {@code withHeight} call, `.diagram-viewer`'s own height is "auto" up to its 60vh ceiling) - which is
     * exactly right for what THEY test (S1's band-subtraction-on-an-auto-box finding), but wide-flat's
     * natural width-bound height (about 209px at this viewport) never reaches that 60vh ceiling, so nothing
     * ever makes its host height definite, and cover's height minimum silently fails to resolve - it
     * degenerates to "fit width, let height follow", indistinguishable from contain for that one fixture.
     * (tall-narrow does NOT show this: its natural width-bound height vastly exceeds 60vh, so the ceiling
     * itself supplies the missing definiteness.) The REAL target consumer (KanbanBoardServer's fullscreen
     * overlay) always calls {@code withHeight("100%")}, so it is never in the affected regime - these two
     * fixtures mirror that by giving the diagram viewer an explicit, definite height, so the cover battery
     * exercises the mechanism under the conditions it actually ships in.
     */
    public static final String                  DIAGRAM_VIEWER_COVER_WIDE_FLAT_CARD_TITLE    = "DiagramViewer Cover Wide Flat, Fixed Height (interactive)";
    public static final String                  DIAGRAM_VIEWER_COVER_TALL_NARROW_CARD_TITLE  = "DiagramViewer Cover Tall Narrow, Fixed Height (interactive)";

    /**
     * plan-277 F3: showcase cards for three Bootstrap-styled components that no browser check could address before (a plain table, a NAV-presented
     * dropdown and the inline form validation feedback). The card TITLE is the handle a test addresses a card by, so the titles and the labels below
     * were chosen not to be, and not to contain, any text an existing browser test locates by (hasText and getByRole names match case-insensitive
     * substrings): none of them contains "Badge", "Copy", "Submit", "Home", "Open modal", "SplitButton", "Options", "Drag and Drop", "Uploaded:" or "Navigation Target", and no locator of this card matches a card that existed before.
     */
    public static final String                  TABLE_CARD_TITLE                             = "Bootstrap Table";
    public static final String                  NAV_DROPDOWN_CARD_TITLE                      = "Navbar Style Menu";
    public static final String                  NAV_DROPDOWN_TOGGLE_LABEL                    = "Browse";
    /**
     * plan-277 section 8 (kanban bcfd7e44): the NavigationBar's own dropdown (its toggle and the two items linking to the "Navigation Target" cards), and
     * the card that renders every {@link Icon.StandardIcon}. None of the texts contains "Navigation Target", "Options", "Home" or "Copy".
     */
    public static final String                  NAV_DROPDOWN_ENTRY_TOGGLE_TEXT               = "Quick links";
    public static final String                  NAV_DROPDOWN_ENTRY_A_TEXT                    = "Go to A";
    public static final String                  NAV_DROPDOWN_ENTRY_B_TEXT                    = "Go to B";
    public static final String                  ICON_CARD_TITLE                              = "Icon Glyphs";
    public static final String                  VALIDATION_CARD_TITLE                        = "Validation Feedback";
    public static final String                  VALIDATION_INVALID_FIELD_LABEL               = "Login id";
    public static final String                  VALIDATION_VALID_FIELD_LABEL                 = "Contact address";
    public static final String                  VALIDATION_BUTTON_NAME                       = "Validate fields";
    public static final String                  VALIDATION_INVALID_MESSAGE                   = "Login id is already taken";
    public static final String                  VALIDATION_VALID_MESSAGE                     = "Contact address looks fine";
    /**
     * Hand-written literal SVGs (plan-265 S0 brief - preferred over pulling in a diagram renderer for a
     * measuring-instrument fixture). Each carries both a parseable {@code viewBox} AND its own
     * {@code width}/{@code height} attributes - what real server-rendered diagram SVG looks like, and what
     * {@code DiagramViewer.tsx}'s {@code hasViewBox} state gates the zoom/pan controls on.
     * {@code marker-rect} is the SVG-interior element a scroll-geometry test can measure the position of.
     */
    private static final String                 DIAGRAM_VIEWER_TALL_NARROW_SVG               = "<svg viewBox=\"0 0 120 900\" width=\"120\" height=\"900\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"120\" height=\"900\" fill=\"#eef2ff\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"10\" y=\"10\" width=\"30\" height=\"30\" fill=\"#0066cc\" />"
                                                                                               + "<line x1=\"0\" y1=\"450\" x2=\"120\" y2=\"450\" stroke=\"#333333\" stroke-width=\"2\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"80\" y=\"860\" width=\"30\" height=\"30\" fill=\"#cc0066\" />"
                                                                                               + "<text x=\"10\" y=\"890\" font-size=\"20\">Tall</text>" + "</svg>";

    private static final String                 DIAGRAM_VIEWER_WIDE_FLAT_SVG                 = "<svg viewBox=\"0 0 900 120\" width=\"900\" height=\"120\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"900\" height=\"120\" fill=\"#eefaef\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"10\" y=\"10\" width=\"30\" height=\"30\" fill=\"#0066cc\" />"
                                                                                               + "<line x1=\"450\" y1=\"0\" x2=\"450\" y2=\"120\" stroke=\"#333333\" stroke-width=\"2\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"860\" y=\"80\" width=\"30\" height=\"30\" fill=\"#cc0066\" />"
                                                                                               + "<text x=\"10\" y=\"110\" font-size=\"20\">Wide</text>" + "</svg>";

    private static final String                 DIAGRAM_VIEWER_NON_INTERACTIVE_SVG           = "<svg viewBox=\"0 0 200 200\" width=\"200\" height=\"200\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"200\" height=\"200\" fill=\"#fefeee\" />"
                                                                                               + "<rect x=\"20\" y=\"20\" width=\"40\" height=\"40\" fill=\"#666666\" />" + "</svg>";

    /**
     * plan-266 AC-N11 fixtures. Each carries {@code #marker-rect} at the origin corner (reusing the existing
     * IT convention) AND {@code #end-marker} at the FAR corner - the element AC-N4 ("no content lost") scrolls
     * to and asserts is reachable inside the host's visible rect. Dimensions are deliberately far from the
     * host's own aspect ratio in each direction, per the workspace finding that a coincidentally-proportioned
     * fixture can make a wrong answer numerically equal the right one (plan-266 section 2.10c).
     */
    private static final String                 DIAGRAM_VIEWER_SMALLER_BOTH_AXES_SVG         = "<svg viewBox=\"0 0 300 200\" width=\"300\" height=\"200\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"300\" height=\"200\" fill=\"#eef2ff\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"5\" y=\"5\" width=\"20\" height=\"20\" fill=\"#0066cc\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"275\" y=\"175\" width=\"20\" height=\"20\" fill=\"#cc0066\" />"
                                                                                               + "</svg>";

    private static final String                 DIAGRAM_VIEWER_LARGER_BOTH_AXES_SVG          = "<svg viewBox=\"0 0 3000 2500\" width=\"3000\" height=\"2500\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"3000\" height=\"2500\" fill=\"#eef2ff\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"10\" y=\"10\" width=\"60\" height=\"60\" fill=\"#0066cc\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"2930\" y=\"2430\" width=\"60\" height=\"60\" fill=\"#cc0066\" />"
                                                                                               + "</svg>";

    private static final String                 DIAGRAM_VIEWER_LARGER_WIDTH_ONLY_SVG         = "<svg viewBox=\"0 0 3000 400\" width=\"3000\" height=\"400\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"3000\" height=\"400\" fill=\"#eefaef\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"10\" y=\"10\" width=\"40\" height=\"40\" fill=\"#0066cc\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"2950\" y=\"350\" width=\"40\" height=\"40\" fill=\"#cc0066\" />"
                                                                                               + "</svg>";

    private static final String                 DIAGRAM_VIEWER_LARGER_HEIGHT_ONLY_SVG        = "<svg viewBox=\"0 0 400 3000\" width=\"400\" height=\"3000\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"400\" height=\"3000\" fill=\"#fefeee\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"10\" y=\"10\" width=\"40\" height=\"40\" fill=\"#0066cc\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"350\" y=\"2950\" width=\"40\" height=\"40\" fill=\"#cc0066\" />"
                                                                                               + "</svg>";

    /**
     * plan-266 AC-N7 fixture. {@code viewBox} width/height are CALIBRATED to the host box MEASURED for
     * {@code withWidth("800px").withHeight("300px")} at viewport 1600x1000 - clientWidth=800.0,
     * clientHeight=260.0 (300px minus the 40px control band), aspect 3.0769..., which is exactly 800/260 -
     * i.e. an exact match by construction rather than a coincidence, since the width and height are both
     * author-fixed (no percentage, no card-column dependency) and the band's 40px is itself a measured
     * constant (plan-266 S1).
     */
    private static final String                 DIAGRAM_VIEWER_SQUARE_ASPECT_SVG             = "<svg viewBox=\"0 0 800 260\" width=\"800\" height=\"260\" "
                                                                                               + "xmlns=\"http://www.w3.org/2000/svg\">"
                                                                                               + "<rect x=\"0\" y=\"0\" width=\"800\" height=\"260\" fill=\"#f5f5f5\" />"
                                                                                               + "<rect id=\"marker-rect\" x=\"10\" y=\"10\" width=\"40\" height=\"40\" fill=\"#0066cc\" />"
                                                                                               + "<rect id=\"end-marker\" x=\"750\" y=\"210\" width=\"40\" height=\"40\" fill=\"#cc0066\" />"
                                                                                               + "</svg>";

    @Autowired
    private ReactUIService                      uiService;

    private final AtomicBoolean                 modalVisible                                 = new AtomicBoolean(false);
    private final AtomicBoolean                 offcanvasVisible                             = new AtomicBoolean(false);
    private final AtomicBoolean                 toggleButtonPressed                          = new AtomicBoolean(false);

    /**
     * Stable {@link ByteArrayChannel} instance held across renders (required by {@link org.omnaest.react4j.component.form.upload.UploadChannel}'s usage
     * contract) so a repeat upload against the same rendered {@code uploadId} keeps working.
     */
    private final ByteArrayChannel              fileUploadChannel                            = ByteArrayChannel.create();

    /**
     * Small in-memory multi-level tree (plan-76 Slice 8) held as a stable field, mirroring
     * {@link #fileUploadChannel}, so the demo tree's identity (and any provider-internal caching) survives across
     * renders instead of being rebuilt per request.
     */
    private final ShowcaseTreeTableDataProvider treeTableDataProvider                        = new ShowcaseTreeTableDataProvider();

    /**
     * plan-235 S3: genuine server-side drag-and-drop demo state (AC-BROWSER-1..5/7). {@code dragDropCardLabel}
     * holds each card's stable display text; {@code dragDropContainerChildren} holds, per container id, the
     * ORDERED list of card ids it directly owns - a container id is either a list root ({@code "A"}/{@code "B"})
     * or another card's own id (a card that has been dropped ONTO another - {@link DropRelation#INTO} - becomes a
     * child of that card, one level of nesting, rendered indented under it). Mutated only from
     * {@link #handleDragDropEvent(String, DropEvent)}, which every demo {@link DropTarget} card shares.
     */
    private final Map<String, String>           dragDropCardLabel                            = new ConcurrentHashMap<>();
    private final Map<String, List<String>>     dragDropContainerChildren                    = new ConcurrentHashMap<>();

    /**
     * plan-235 S3 AC-BROWSER-6: bumped by the {@link org.omnaest.react4j.domain.Button} added inside the
     * IntervalRerenderingContainer card's refreshed content, alongside the pre-existing "Server time" paragraph -
     * proves the interval-wrapped subtree still handles a click while it keeps ticking on its own timer.
     */
    private final AtomicInteger                 intervalClickCount                           = new AtomicInteger(0);

    /**
     * plan-262 S1: bumped by the {@code Breadcrumb} demo's "Library" entry {@code onClick} - demonstrates the
     * new per-entry server-side click handler alongside the existing link-only entries.
     */
    private final AtomicInteger                 breadcrumbClickCount                         = new AtomicInteger(0);

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
                                                                        .withLinkedLocator(NAV_TARGET_B_LOCATOR))
                                                .addDropdown(dropdown -> dropdown.withText(NAV_DROPDOWN_ENTRY_TOGGLE_TEXT)
                                                                                 .addEntry(entry -> entry.withText(NAV_DROPDOWN_ENTRY_A_TEXT)
                                                                                                         .withLinkedLocator(NAV_TARGET_A_LOCATOR))
                                                                                 .addEntry(entry -> entry.withText(NAV_DROPDOWN_ENTRY_B_TEXT)
                                                                                                         .withLinkedLocator(NAV_TARGET_B_LOCATOR))));
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
                                                               .withWindowSize(3)))
                      // --- plan-265 S0: DiagramViewer showcase fixtures (measuring instrument, no production change) ---
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_TALL_NARROW_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_TALL_NARROW_SVG)
                                                               .withInteractive(true)))
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_WIDE_FLAT_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_WIDE_FLAT_SVG)
                                                               .withInteractive(true)))
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_NON_INTERACTIVE_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_NON_INTERACTIVE_SVG)
                                                               .withInteractive(false)))
                      // --- plan-266 Cliff N4 / AC-N11: the four intrinsic-size-relationship fixtures. Each
                      // gets an explicit height for the same measured reason as the cover battery pair above
                      // (see DIAGRAM_VIEWER_COVER_WIDE_FLAT_CARD_TITLE's javadoc) - without it, a fixture
                      // whose natural width-bound height stays under the 60vh ceiling (the width-only-larger
                      // shape) cannot make its host height definite, and cover's min-height silently fails to
                      // resolve. ---
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_SMALLER_BOTH_AXES_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_SMALLER_BOTH_AXES_SVG)
                                                               .withHeight("400px")
                                                               .withInteractive(true)))
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_LARGER_BOTH_AXES_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_LARGER_BOTH_AXES_SVG)
                                                               .withHeight("400px")
                                                               .withInteractive(true)))
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_LARGER_WIDTH_ONLY_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_LARGER_WIDTH_ONLY_SVG)
                                                               .withHeight("400px")
                                                               .withInteractive(true)))
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_LARGER_HEIGHT_ONLY_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_LARGER_HEIGHT_ONLY_SVG)
                                                               .withHeight("400px")
                                                               .withInteractive(true)))
                      // --- plan-266 AC-N7: fixed, page-layout-independent box so the viewBox can be
                      // calibrated to match the host's own content-box aspect exactly ---
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_SQUARE_ASPECT_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_SQUARE_ASPECT_SVG)
                                                               .withWidth("800px")
                                                               .withHeight("300px")
                                                               .withInteractive(true)))
                      // --- plan-266 AC-N1/N2/N3/N4/N5/N9/N10: definite-height cover battery fixtures ---
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_COVER_WIDE_FLAT_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_WIDE_FLAT_SVG)
                                                               .withHeight("400px")
                                                               .withInteractive(true)))
                      .addComponent(factory.newCard()
                                           .withTitle(DIAGRAM_VIEWER_COVER_TALL_NARROW_CARD_TITLE)
                                           .withContent(factory.newDiagramViewer()
                                                               .withSvg(DIAGRAM_VIEWER_TALL_NARROW_SVG)
                                                               .withHeight("400px")
                                                               .withInteractive(true)))
                      // --- plan-277 F3: table, NAV dropdown and form validation feedback (appended last, no existing card moves) ---
                      .addComponent(factory.newCard()
                                           .withTitle(TABLE_CARD_TITLE)
                                           .withContent(factory.newTable()
                                                               .withColumnTitles("Item", "Count", "State")
                                                               .addRowTextContent(List.of("Alpha", "12", "ready"))
                                                               .addRowTextContent(List.of("Beta", "7", "pending"))
                                                               .addRowTextContent(List.of("Gamma", "31", "done"))))
                      .addComponent(factory.newCard()
                                           .withTitle(NAV_DROPDOWN_CARD_TITLE)
                                           .withContent(factory.newDropdown()
                                                               .withTitle(NAV_DROPDOWN_TOGGLE_LABEL)
                                                               .withPresentation(Dropdown.Presentation.NAV)
                                                               .addItem(item -> item.withText("Reports"))
                                                               .addItem(item -> item.withText("Settings"))))
                      .addComponent(this.buildIconCard(factory))
                      .addComponent(factory.newCard()
                                           .withTitle(VALIDATION_CARD_TITLE)
                                           .withContent(factory.newStack()
                                                               .withContent(factory.newForm()
                                                                                   .withUIContext(this.createValidationFormProvider()))));
    }
    /**
     * plan-277 section 8: a card showing every {@link Icon.StandardIcon}, so that a browser test can assert that each one renders a real glyph
     */
    private UIComponent<?> buildIconCard(UIComponentFactory factory)
    {
        Composite icons = factory.newComposite();
        for (Icon.StandardIcon standardIcon : Icon.StandardIcon.values())
        {
            icons.addComponent(factory.newIcon()
                                      .from(standardIcon));
        }
        return factory.newCard()
                      .withTitle(ICON_CARD_TITLE)
                      .withContent(icons);
    }

    /**
     * plan-277 F3: a {@link Form} of two text fields and one button whose server side handler adds an INVALID message to the first field and a VALID
     * one to the second, so that a browser test can click it and read the real validation feedback rendering (is-invalid / is-valid plus the
     * feedback texts) through the genuine {@code /ui/event} round trip. The document is attached the way {@link #createFormProvider()} does it.
     */
    private UIContextAndDataConsumer<Form> createValidationFormProvider()
    {
        return (form, uiContext, initialData) ->
        {
            Document document = uiContext.getFirstDocument();
            Field invalidField = document.getField("showcaseLoginIdField");
            Field validField = document.getField("showcaseContactField");

            form.addInputField(input -> input.attachToField(invalidField)
                                             .withLabel(VALIDATION_INVALID_FIELD_LABEL));
            form.addInputField(input -> input.attachToField(validField)
                                             .withLabel(VALIDATION_VALID_FIELD_LABEL));
            form.addButton(button -> button.withText(VALIDATION_BUTTON_NAME)
                                           .onClick((data, messaging, context) ->
                                           {
                                               messaging.addValidationMessage(invalidField, Form.ValidationMessageType.INVALID, VALIDATION_INVALID_MESSAGE);
                                               messaging.addValidationMessage(validField, Form.ValidationMessageType.VALID, VALIDATION_VALID_MESSAGE);
                                               return data;
                                           }));
        };
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
