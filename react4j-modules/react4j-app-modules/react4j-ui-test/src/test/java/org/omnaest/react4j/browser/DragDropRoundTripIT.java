package org.omnaest.react4j.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.omnaest.react4j.MockApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.BoundingBox;

/**
 * plan-235 S3 (browser verification, AC-BROWSER-1..5/7). Real Playwright against the real
 * {@link org.omnaest.react4j.ComponentShowcaseUI} "Drag and Drop" card (genuine server-side state, see that
 * class's {@code dragDropContainerChildren}).
 * <p>
 * <b>Mechanism (S9.5c / this slice's Step 0).</b> Drives every drag with the RAW MOUSE recipe - move to the
 * source's center, {@code down()}, ~10 intermediate {@code move()} calls stepping toward the destination,
 * {@code up()} - never {@code locator.dragTo()} alone and never a synthetic-event dispatch. Two reasons, both
 * load-bearing and recorded in the plan: {@code dragTo()} returns without throwing regardless of outcome, so its
 * silence is not evidence; and a synthetic-event drive with a fresh {@code DataTransfer} per event still fires a
 * {@code drop} with an EMPTY payload, which looks green and proves nothing. Every test below therefore asserts on
 * the PAYLOAD's effect (the card's new position/parent, read back from real rendered DOM text, and - for
 * AC-BROWSER-1 - a full page reload), never on the mere occurrence of a drop.
 * <p>
 * <b>Fixture isolation.</b> Each test method drags its OWN dedicated pair/trio of cards (see
 * {@code ComponentShowcaseUI.initDragDropDemoState}), because the underlying {@code @Service} state is a
 * singleton that lives for the whole class run (Spring caches the context across {@code @Test} methods) - no
 * two methods below ever touch the same card id.
 * <p>
 * Excluded from default {@code mvn test} via {@code @Tag("browser")} + the module's
 * {@code <excludedGroups>browser</excludedGroups>} POM property (see
 * {@code surefire-excludedgroups-property-not-config-literal} / {@link ToggleButtonRoundTripIT}).
 */
@Tag("browser")
@SpringBootTest(classes = MockApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class DragDropRoundTripIT
{
    @LocalServerPort
    private int        port;

    private Playwright playwright;
    private Browser    browser;
    private Page       page;

    @BeforeEach
    public void openBrowser()
    {
        this.playwright = Playwright.create();
        this.browser = this.playwright.chromium()
                                      .launch(new BrowserType.LaunchOptions().setHeadless(true));
        this.page = this.browser.newPage();
        this.page.navigate("http://localhost:" + this.port + "/");
    }

    @AfterEach
    public void closeBrowser()
    {
        if (this.browser != null)
        {
            this.browser.close();
        }
        if (this.playwright != null)
        {
            this.playwright.close();
        }
    }

    /**
     * AC-BROWSER-1 (persists via RELOAD, not the immediate response) + AC-BROWSER-5 (both lists, siblings under
     * ONE RerenderingContainer, visibly update in the SAME response). {@code card-x1} (List A) is dragged onto
     * {@code card-x2} (List B), relation BEFORE (drop on x2's top third) - a genuine cross-list move.
     */
    @Test
    public void crossListDragPersistsAndBothListsUpdateInSameResponse()
    {
        Locator listA = this.dragDropCard("card-x1");
        listA.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(this.dragDropList("A").locator("[data-drag-id='card-x1']")
                       .count() == 1,
                   "precondition: card-x1 starts in List A");
        assertEquals(0,
                     this.dragDropList("B")
                         .locator("[data-drag-id='card-x1']")
                         .count(),
                     "precondition: card-x1 does not start in List B");

        this.rawMouseDrag("card-x1", "card-x2", VerticalZone.TOP);
        this.waitForSettled();

        // AC-BROWSER-5: BOTH lists reflect the move in the SAME response, no reload yet.
        assertEquals(0,
                     this.dragDropList("A")
                         .locator("[data-drag-id='card-x1']")
                         .count(),
                     "List A must no longer contain card-x1 immediately after the settled drop");
        assertEquals(1,
                     this.dragDropList("B")
                         .locator("[data-drag-id='card-x1']")
                         .count(),
                     "List B must contain card-x1 immediately after the settled drop, in the SAME response");

        // AC-BROWSER-1: reload (never a targeted subnode fetch) and re-check from a fresh GET /ui.
        this.reload();
        Locator reloadedCard = this.dragDropList("B")
                                   .locator("[data-drag-id='card-x1']");
        reloadedCard.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertEquals(1, reloadedCard.count(), "card-x1 must still be in List B after a full page reload");
        assertEquals(0,
                     this.dragDropList("A")
                         .locator("[data-drag-id='card-x1']")
                         .count(),
                     "card-x1 must still be absent from List A after a full page reload");
    }

    /**
     * AC-BROWSER-2: {@code card-y2} dropped onto {@code card-y1}'s top third (BEFORE) reorders List A's
     * [y1, y2] to [y2, y1]. Verified by reading the rendered ORDER of the two labels' text, then by reload.
     */
    @Test
    public void reorderWithinListPersists()
    {
        assertEquals("card-y1,card-y2", this.dragDropOrderOf("A", "card-y1", "card-y2"), "precondition: y1 before y2");

        this.rawMouseDrag("card-y2", "card-y1", VerticalZone.TOP);
        this.waitForSettled();
        assertEquals("card-y2,card-y1", this.dragDropOrderOf("A", "card-y1", "card-y2"), "y2 must now render BEFORE y1");

        this.reload();
        assertEquals("card-y2,card-y1", this.dragDropOrderOf("A", "card-y1", "card-y2"),
                     "reorder must survive a full page reload");
    }

    /**
     * AC-BROWSER-3: {@code card-z1} dropped onto {@code card-zt1}'s MIDDLE third (INTO) reparents z1 to become a
     * child rendered nested inside zt1 - z1 must disappear from List A's TOP-LEVEL card set and appear as a
     * descendant of zt1 instead.
     */
    @Test
    public void dropOntoItemReparentsIt()
    {
        this.dragDropCard("card-z1")
            .waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertTrue(this.isTopLevel("A", "card-z1"), "precondition: card-z1 starts as a TOP-LEVEL card in List A");

        this.rawMouseDrag("card-z1", "card-zt1", VerticalZone.MIDDLE);
        this.waitForSettled();

        assertFalse(this.isTopLevel("A", "card-z1"), "card-z1 must no longer be a TOP-LEVEL card in List A after being dropped INTO zt1");
        Locator nestedZ1 = this.dragDropCardByDragId("card-zt1")
                               .locator("[data-drag-id='card-z1']");
        assertEquals(1, nestedZ1.count(), "card-z1 must now render nested inside card-zt1");

        this.reload();
        assertEquals(1,
                     this.dragDropCardByDragId("card-zt1")
                         .locator("[data-drag-id='card-z1']")
                         .count(),
                     "the reparent must survive a full page reload");
    }

    /**
     * AC-BROWSER-4, the sharpest criterion: TWO consecutive drags with NO intervening reload must BOTH apply.
     * Handler registration runs on the GET /ui walk and each request's own re-registration of its
     * RerenderingContainer subtree (see {@code react4j-event-handler-registration-runs-only-at-get-ui} and
     * plan-235 Cliff 5/S9's registration mechanism) - a single-drag test cannot see a second drag silently
     * resolving no handler. [w1, w2, w3] -> drag w2 onto w1 (BEFORE) -> [w2, w1, w3] -> immediately (no reload)
     * drag w3 onto w2 (BEFORE) -> [w3, w2, w1].
     */
    @Test
    public void twoConsecutiveDragsBothApplyWithNoReload()
    {
        assertEquals("card-w1,card-w2,card-w3", this.dragDropOrderOf("A", "card-w1", "card-w2", "card-w3"),
                     "precondition: w1, w2, w3 in that order");

        this.rawMouseDrag("card-w2", "card-w1", VerticalZone.TOP);
        this.waitForSettled();
        assertEquals("card-w2,card-w1,card-w3", this.dragDropOrderOf("A", "card-w1", "card-w2", "card-w3"),
                     "first drag must apply: w2 before w1");

        // Second drag, SAME page, no reload in between - the criterion under test.
        this.rawMouseDrag("card-w3", "card-w2", VerticalZone.TOP);
        this.waitForSettled();
        assertEquals("card-w3,card-w2,card-w1", this.dragDropOrderOf("A", "card-w1", "card-w2", "card-w3"),
                     "second drag must ALSO apply, with no reload between the two - a silently-unresolved handler "
                                                                                                            + "on the second drag would leave the order unchanged from after the first drag");
    }

    /**
     * AC-BROWSER-7: the keyboard path works without a pointer. Focus the Draggable {@code card-k1} (List A) and
     * press Enter to "pick up" ({@code aria-grabbed} flips true); focus the DropTarget wrapping {@code card-k2}
     * (List B) and press Enter to "drop" - {@code DropTarget.tsx}'s keyboard handler always uses relation INTO,
     * so k1 must end up reparented under k2.
     * <p>
     * <b>plan-235 S1 corrective round 2 (Defect A), GREEN.</b> Round 1 left this RED with a documented finding:
     * pressing Enter to pick up {@code card-k1} was itself firing a spurious self-drop and clearing
     * {@code KeyboardDragState} before the real drop was ever attempted - confirmed at the unit level in
     * {@code Draggable.test.tsx}/{@code DropTarget.test.tsx} (the fixed source's own test suite shows this
     * RED-before-GREEN: {@code KeyboardDragState.getGrabbedDragId()} was observed {@code null} immediately after
     * pick-up against the unfixed source). Root cause: {@code Draggable.tsx}'s {@code handleKeyDown} never called
     * {@code stopPropagation()}, so the pick-up keydown bubbled into card-k1's OWN enclosing {@code DropTarget},
     * whose {@code handleKeyDown} read the just-set grabbed id synchronously and self-dropped. Fixed by having
     * {@code Draggable.tsx} stop propagation on a handled key, and {@code DropTarget.tsx} stop propagation only
     * once it genuinely consumes an event (drop or keyboard drop) - see both files' javadoc for the full
     * mechanism. This test now asserts the POSITIVE outcome plus the negative control that motivated the fix:
     * zero {@code /ui/event} requests during pick-up itself.
     */
    @Test
    public void keyboardPickUpAndDropWorksWithoutPointer()
    {
        Locator draggableK1 = this.dragDropCardByDragId("card-k1");
        draggableK1.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertEquals("false", draggableK1.getAttribute("aria-grabbed"), "precondition: k1 not yet grabbed");

        List<String> eventUrls = new CopyOnWriteArrayList<>();
        this.page.onRequest(req ->
        {
            if (req.url()
                   .contains("/ui/event"))
            {
                eventUrls.add(req.url());
            }
        });

        draggableK1.focus();
        draggableK1.press("Enter");
        assertEquals("true", draggableK1.getAttribute("aria-grabbed"), "Enter on a focused Draggable must pick it up");

        // AC-R1/AC-R3: picking up must NOT itself fire a round trip (the confirmed Defect A self-drop).
        this.page.waitForTimeout(300);
        assertTrue(eventUrls.isEmpty(), "pick-up alone must fire ZERO /ui/event requests - observed: " + eventUrls);

        // The DropTarget wrapping card-k2 is card-k2's own ANCESTOR div (react4j-drop-target); focus THAT element,
        // not the inner draggable, since DropTarget.tsx's keyboard handler is registered on its own root div.
        Locator dropTargetK2 = this.dragDropTarget("card-k2");
        assertEquals(1, dropTargetK2.count(), "the DropTarget wrapping card-k2 must resolve to exactly one element");
        assertEquals("0", dropTargetK2.getAttribute("tabindex"), "it must be focusable (hasTarget true)");

        dropTargetK2.focus();
        assertTrue((Boolean) this.page.evaluate("el => el === document.activeElement", dropTargetK2.elementHandle()),
                   "dropTargetK2 must actually become document.activeElement after focus()");
        assertEquals("true", draggableK1.getAttribute("aria-grabbed"),
                     "k1 must STILL be grabbed after focusing the drop target - no stray blur/focus reset");

        dropTargetK2.press("Enter");
        this.waitForSettled();

        assertFalse(eventUrls.isEmpty(), "the drop itself must fire a /ui/event request");
        Locator nestedK1 = this.dragDropCardByDragId("card-k2")
                               .locator("[data-drag-id='card-k1']");
        assertEquals(1, nestedK1.count(), "card-k1 must now render nested inside card-k2 (relation is always INTO for the keyboard path)");

        this.reload();
        assertEquals(1,
                     this.dragDropCardByDragId("card-k2")
                         .locator("[data-drag-id='card-k1']")
                         .count(),
                     "the keyboard-driven reparent must survive a full page reload");
    }

    /**
     * plan-235 S1 corrective round 2 (Defect B), AC-R4(i). List C's own outer {@link Locator}
     * {@code card-n1-target} is BOTH an item-level {@code DropTarget} AND nested inside List C's OWN
     * list-level {@code DropTarget} ({@link org.omnaest.react4j.ComponentShowcaseUI#renderNestedDropTargetList}).
     * A drop landing ON the item must route to the item only - {@code DropTarget.tsx} stopping propagation once
     * it genuinely consumes the drop must prevent the list-level handler from ALSO firing for the same physical
     * gesture, so exactly ONE {@code /ui/event} request fires.
     */
    @Test
    public void dropOntoNestedListItemRoutesToItemOnly()
    {
        this.dragDropCardByDragId("card-n1-target")
            .waitFor(new Locator.WaitForOptions().setTimeout(10000));
        assertEquals(0,
                     this.dragDropCardByDragId("card-n1-target")
                         .locator("[data-drag-id='card-n1-src']")
                         .count(),
                     "precondition: card-n1-src is not yet nested inside card-n1-target");

        List<String> eventUrls = new CopyOnWriteArrayList<>();
        this.page.onRequest(req ->
        {
            if (req.url()
                   .contains("/ui/event"))
            {
                eventUrls.add(req.url());
            }
        });

        this.rawMouseDrag("card-n1-src", "card-n1-target", VerticalZone.MIDDLE);
        this.waitForSettled();

        assertEquals(1,
                     eventUrls.size(),
                     "exactly ONE /ui/event request must fire for a drop onto an item nested inside a live "
                                       + "list-level DropTarget - observed: "
                                       + eventUrls);
        assertEquals(1,
                     this.dragDropCardByDragId("card-n1-target")
                         .locator("[data-drag-id='card-n1-src']")
                         .count(),
                     "card-n1-src must now render nested inside card-n1-target (the item-level handler fired)");
        assertFalse(this.isTopLevel("C", "card-n1-src"),
                    "card-n1-src must NOT be a TOP-LEVEL child of List C - a list-level double-fire would have added it there");

        this.reload();
        assertEquals(1,
                     this.dragDropCardByDragId("card-n1-target")
                         .locator("[data-drag-id='card-n1-src']")
                         .count(),
                     "must survive a full page reload");
    }

    /**
     * plan-235 S1 corrective round 2 (Defect B), AC-R4(ii). List D starts EMPTY - no item-level {@code DropTarget}
     * exists inside it at all, so a drop anywhere within its bounds can ONLY land on the list-level
     * {@code DropTarget} itself. This is the capability an item-only target structurally cannot offer: a Kanban
     * column emptied of its last card must still be able to receive one.
     */
    @Test
    public void dropOntoEmptyNestedListRoutesToTheListItself()
    {
        Locator listDItems = this.dragDropList("D")
                                 .locator("[data-testid='drop-target']");
        assertEquals(0, listDItems.count(), "precondition: List D starts with no item-level DropTargets at all");

        List<String> eventUrls = new CopyOnWriteArrayList<>();
        this.page.onRequest(req ->
        {
            if (req.url()
                   .contains("/ui/event"))
            {
                eventUrls.add(req.url());
            }
        });

        this.rawMouseDragOntoListArea("card-n2-src", "D");
        this.waitForSettled();

        assertEquals(1,
                     eventUrls.size(),
                     "exactly ONE /ui/event request must fire for a drop onto a live list-level DropTarget's own empty space - observed: "
                                       + eventUrls);
        assertEquals(1,
                     this.dragDropList("D")
                         .locator("[data-drag-id='card-n2-src']")
                         .count(),
                     "card-n2-src must now be a child of List D itself - the list-level handler received the drop");

        this.reload();
        assertEquals(1,
                     this.dragDropList("D")
                         .locator("[data-drag-id='card-n2-src']")
                         .count(),
                     "must survive a full page reload");
    }

    // ------------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------------

    private enum VerticalZone
    {
        TOP, MIDDLE, BOTTOM
    }

    /**
     * A full page reload (never a targeted subnode fetch, per this slice's diagnostic guidance) - then waits for
     * the "Drag and Drop" card's own heading to be back in the DOM before returning, since {@code Page.navigate}
     * only waits for the HTML document's 'load' event, not for the React app's own async {@code GET /ui} fetch and
     * subsequent render to complete. Reading rendered state immediately after {@code navigate()} with no such wait
     * raced the render and read an empty/stale DOM in an earlier version of this file.
     */
    private void reload()
    {
        this.page.navigate("http://localhost:" + this.port + "/");
        this.page.locator(".card", new Page.LocatorOptions().setHasText("Drag and Drop"))
                 .waitFor(new Locator.WaitForOptions().setTimeout(10000));
    }

    /**
     * The card's OWN Draggable div (carries {@code data-drag-id}), located ANYWHERE on the page - AC-BROWSER-3's
     * nested case needs this to find a card regardless of current nesting depth.
     */
    private Locator dragDropCard(String dragId)
    {
        return this.page.locator("[data-drag-id='" + dragId + "']");
    }

    private Locator dragDropCardByDragId(String dragId)
    {
        return this.dragDropCard(dragId);
    }

    /**
     * The card's own DropTarget ancestor div - the element a real drop lands on. Deliberately the Draggable's
     * OWN IMMEDIATE DOM PARENT ({@code xpath=..}), not a {@code has()}-descendant filter over
     * {@code [data-testid='drop-target']}: plan-235 S1 corrective round 2 (AC-R4) nests a list-level DropTarget
     * AROUND item-level ones, so a {@code has()} filter matches BOTH the item's own DropTarget AND every live
     * DropTarget ancestor whose subtree happens to contain this dragId (a Playwright "strict mode violation",
     * observed with card-n1-target resolving to 2 elements before this fix) - the immediate-parent read is
     * unambiguous regardless of nesting depth, since {@code DropTargetImpl} always renders its {@code content}
     * (here, the Draggable) as its own direct-and-only child with no wrapper in between.
     */
    private Locator dragDropTarget(String dragId)
    {
        return this.dragDropCard(dragId)
                   .locator("xpath=..");
    }

    /**
     * Whether {@code dragId} is currently a TOP-LEVEL (direct) child of the named list, i.e. NOT nested under
     * another reparented card. Deliberately reads each top-level {@code drop-target}'s OWN direct child's
     * {@code data-drag-id} (same technique as {@link #dragDropOrderOf}) rather than
     * {@code Locator.filter(setHas(...))}: a {@code has()}-based descendant check matches ANY ancestor whose
     * subtree contains a {@code data-drag-id} match anywhere within it - which is wrong here specifically BECAUSE
     * reparenting works, since after {@code card-z1} is dropped INTO {@code card-zt1}, {@code card-zt1}'s own
     * subtree DOES contain a {@code [data-drag-id='card-z1']} descendant, so a {@code has()} filter for z1 keeps
     * matching zt1's top-level element and reports z1 as still "top-level" (observed: count 1 where 0 was
     * expected, even though {@link #dragDropOrderOf} correctly omitted z1 from the same scan).
     */
    private boolean isTopLevel(String listLabel, String dragId)
    {
        Locator topLevelCards = this.dragDropList(listLabel)
                                    .locator(":scope > [data-testid='drop-target']");
        int count = topLevelCards.count();
        for (int i = 0; i < count; i++)
        {
            String id = topLevelCards.nth(i)
                                     .locator(":scope > [data-drag-id]")
                                     .getAttribute("data-drag-id");
            if (dragId.equals(id))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * The "List A"/"List B"/... card's content area, scoped by its own heading text - used to count TOP-LEVEL
     * cards (direct children) versus cards nested under a reparented card elsewhere on the page.
     */
    private Locator dragDropList(String listLabel)
    {
        String heading = "List " + listLabel;
        return this.page.locator(".card", new Page.LocatorOptions().setHasText("Drag and Drop"))
                        .locator("h4", new Locator.LocatorOptions().setHasText(heading))
                        .locator("xpath=..");
    }

    /**
     * plan-235 S1 corrective round 2 (AC-R4). The OUTER, list-level {@code DropTarget} div wrapping the named
     * list - the element a drop onto the list's own empty space (no item under the pointer) actually lands on.
     * Distinct from {@link #dragDropList}, which resolves to the inner {@code Stack} div one level down; this is
     * that Stack's own IMMEDIATE DOM PARENT ({@code xpath=..}), mirroring {@link #dragDropTarget}'s fix -
     * {@code renderNestedDropTargetList} wraps the Stack directly in the list-level DropTarget with no
     * intervening element, and a {@code has()}-descendant filter chained through the Stack locator's own
     * {@code xpath=..} step was observed resolving to ZERO matches (a 30s Playwright timeout) rather than
     * composing as expected.
     */
    private Locator dragDropListDropTarget(String listLabel)
    {
        return this.dragDropList(listLabel)
                   .locator("xpath=..");
    }

    /**
     * Reads the rendered top-to-bottom order of the given drag ids as they appear inside the named list,
     * restricted to TOP-LEVEL cards (direct children of the list), as a comma-joined string.
     */
    private String dragDropOrderOf(String listLabel, String... dragIds)
    {
        Locator topLevelCards = this.dragDropList(listLabel)
                                    .locator(":scope > [data-testid='drop-target']");
        int count = topLevelCards.count();
        StringBuilder order = new StringBuilder();
        for (int i = 0; i < count; i++)
        {
            String id = topLevelCards.nth(i)
                                     .locator(":scope > [data-drag-id]")
                                     .getAttribute("data-drag-id");
            if (Arrays.asList(dragIds)
                      .contains(id))
            {
                if (order.length() > 0)
                {
                    order.append(",");
                }
                order.append(id);
            }
        }
        return order.toString();
    }

    /**
     * Raw-mouse drag (plan-235 S9.5c GO recipe, this slice's Step 0 confirmation under Java Playwright 1.44):
     * move to the source card's center, press down, ~10 intermediate moves toward the destination zone, release.
     * A single down/move/up does NOT start a real HTML5 drag - the intermediate moves are required.
     */
    private void rawMouseDrag(String sourceDragId, String targetDragId, VerticalZone zone)
    {
        double zoneFraction;
        if (zone == VerticalZone.TOP)
        {
            zoneFraction = 0.1;
        }
        else if (zone == VerticalZone.BOTTOM)
        {
            zoneFraction = 0.9;
        }
        else
        {
            zoneFraction = 0.5;
        }
        this.rawMouseDragTo(this.dragDropCard(sourceDragId), sourceDragId, this.dragDropTarget(targetDragId), "target drop zone for '" + targetDragId + "'",
                            zoneFraction);
    }

    /**
     * plan-235 S1 corrective round 2 (AC-R4-ii). Drags onto the MIDDLE of a list's own outer, list-level
     * {@code DropTarget} area (see {@link #dragDropListDropTarget}) rather than onto any item - used to reach a
     * list's empty space, which has no item-level {@code DropTarget} to aim at.
     */
    private void rawMouseDragOntoListArea(String sourceDragId, String listLabel)
    {
        this.rawMouseDragTo(this.dragDropCard(sourceDragId), sourceDragId, this.dragDropListDropTarget(listLabel), "list drop zone for List '" + listLabel
                                                                                                                   + "'",
                            0.5);
    }

    private void rawMouseDragTo(Locator source, String sourceDescription, Locator target, String targetDescription, double verticalFraction)
    {
        source.scrollIntoViewIfNeeded();
        BoundingBox sourceBox = source.boundingBox();
        assertTrue(sourceBox != null, "source card '" + sourceDescription + "' must be rendered with a real bounding box");
        double sx = sourceBox.x + sourceBox.width / 2;
        double sy = sourceBox.y + sourceBox.height / 2;

        target.scrollIntoViewIfNeeded();
        BoundingBox targetBox = target.boundingBox();
        assertTrue(targetBox != null, targetDescription + " must be rendered with a real bounding box");
        double tx = targetBox.x + targetBox.width / 2;
        double ty = targetBox.y + targetBox.height * verticalFraction;

        this.page.mouse()
                 .move(sx, sy);
        this.page.mouse()
                 .down();
        int steps = 10;
        for (int i = 1; i <= steps; i++)
        {
            double t = (double) i / steps;
            this.page.mouse()
                     .move(sx + (tx - sx) * t, sy + (ty - sy) * t);
        }
        this.page.mouse()
                 .up();
    }

    private void waitForSettled()
    {
        this.page.waitForFunction("document.querySelector('.App')?.getAttribute('data-inflight-count') === '0'");
        assertEquals("0", this.page.locator(".App")
                                   .getAttribute("data-inflight-count"));
    }
}
