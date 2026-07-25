import React from "react";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { TreeTable, TreeTableNode } from "./TreeTable";
import { Renderer, NodeContextAccessor } from "../Renderer";
import { Backend } from "../../backend/Backend";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { UIContext, UIContextAccessor, UIContextDataNode } from "../data/DataContextManager";

// TreeTable.tsx now dispatches the load-more button through HandlerFactory -> Backend.sendEvent
// (plan-76 Slice 4), and the registration test below renders through Renderer.tsx which transitively
// imports Button.tsx -> Handler.ts -> Backend.ts -> axios's ESM build either way. An explicit factory
// mock (not the no-factory automock) avoids evaluating that real import graph -- see
// react4j-core-ui-jest-mock-backend-factory-not-automock. Because CRA's default jest config sets
// resetMocks:true (cra-jest-resetmocks-wipes-mock-factory-implementations), sendEvent is re-armed as a
// plain jest.fn() by this SAME factory before every test -- fine here since no test needs a specific
// resolved value, only call assertions.
jest.mock("../../backend/Backend", () => ({
    Backend: {
        uploadFile: jest.fn(),
        sendEvent: jest.fn(),
        getUI: jest.fn(),
        getUISubNode: jest.fn(),
        fetchData: jest.fn()
    },
    BackendUri: {
        URI_UI: "ui",
        URI_UI_HANDLER: "ui/event",
        URI_UI_DATA_SOURCE: "ui/data/query",
        URI_UPLOAD: "ui/upload",
        resolve: jest.fn((uri: string) => uri)
    }
}));

const mockedSendEvent = Backend.sendEvent as jest.MockedFunction<typeof Backend.sendEvent>;

beforeEach(() => {
    jest.clearAllMocks();
});

/**
 * Real TREETABLE JSON payload matching the frozen node-JSON contract emitted by
 * TreeTableRendererImpl (plan-76 Slice 2, backend, DONE): a flattened rows[] list, each row
 * carrying its own depth/expandable/expanded/cells/target, plus a per-group loadMore descriptor.
 */
function realTreeTableNode(overrides?: Partial<TreeTableNode>): TreeTableNode {
    return {
        type: TreeTable.TYPE,
        target: ["tree"],
        columns: [
            { key: "name", title: "Name", filterTarget: [], sortTarget: [], sortPriority: 0, sortPriorityFieldKey: null, sortPriorityReorderTarget: [], filterable: true, sortable: true },
            { key: "value", title: "Value", filterTarget: [], sortTarget: [], sortPriority: 0, sortPriorityFieldKey: null, sortPriorityReorderTarget: [], filterable: true, sortable: true }
        ],
        rows: [
            {
                nodeId: "a",
                depth: 0,
                expandable: false,
                expanded: false,
                cells: { name: "Alpha", value: "10" },
                target: ["tree", "row0"]
            },
            {
                nodeId: "b",
                depth: 0,
                expandable: true,
                expanded: false,
                cells: { name: "Beta", value: "20" },
                target: ["tree", "row1"]
            },
            {
                nodeId: "c",
                depth: 1,
                expandable: true,
                expanded: true,
                cells: { name: "Beta Child", value: "5" },
                target: ["tree", "row1", "row0"]
            }
        ],
        loadMore: {
            available: true,
            nextOffset: 3,
            nextLimit: 500,
            totalCount: 3,
            target: ["tree", "loadmore"]
        },
        multiColumnSortEnabled: false,
        activeSortCount: 0,
        filtersVisible: false,
        activeFilterCount: 0,
        filterToggleTarget: ["tree", "filtertoggle"],
        filterEnabled: true,
        sortEnabled: true,
        flatModeToggleEnabled: false,
        flatMode: false,
        flatToggleTarget: [],
        ...overrides
    };
}

function rowElement(container: HTMLElement, nodeId: string): HTMLElement {
    const row = container.querySelector(`tr[data-node-id="${nodeId}"]`);
    if (!row) {
        throw new Error(`row ${nodeId} not found`);
    }
    return row as HTMLElement;
}

/**
 * Fixture for the Slice-5 childLoadMore placement scenarios: row "c" (depth 1, expanded, with its own
 * childLoadMore) has a descendant "d" (depth 2), followed by a sibling "e" back at depth 0 -- the
 * placement rule requires c's childLoadMore to render after "d" (last row of c's descendant run) and
 * before "e" (the next same-or-shallower row).
 */
function nodeWithExpandedChildAndPlacement(): TreeTableNode {
    return {
        type: TreeTable.TYPE,
        target: ["tree"],
        columns: [
            { key: "name", title: "Name", filterTarget: [], sortTarget: [], sortPriority: 0, sortPriorityFieldKey: null, sortPriorityReorderTarget: [], filterable: true, sortable: true },
            { key: "value", title: "Value", filterTarget: [], sortTarget: [], sortPriority: 0, sortPriorityFieldKey: null, sortPriorityReorderTarget: [], filterable: true, sortable: true }
        ],
        rows: [
            { nodeId: "a", depth: 0, expandable: false, expanded: false, cells: { name: "Alpha", value: "10" }, target: ["tree", "row0"] },
            { nodeId: "b", depth: 0, expandable: true, expanded: false, cells: { name: "Beta", value: "20" }, target: ["tree", "row1"] },
            {
                nodeId: "c", depth: 1, expandable: true, expanded: true, cells: { name: "Beta Child", value: "5" }, target: ["tree", "row1", "row0"],
                childLoadMore: { available: true, nextOffset: 1, nextLimit: 500, totalCount: 5, target: ["tree", "row1", "row0", "loadmore"] }
            },
            { nodeId: "d", depth: 2, expandable: false, expanded: false, cells: { name: "Grandchild", value: "1" }, target: ["tree", "row1", "row0", "row0"] },
            { nodeId: "e", depth: 0, expandable: false, expanded: false, cells: { name: "Echo", value: "99" }, target: ["tree", "row2"] }
        ],
        loadMore: { available: false, nextOffset: 5, nextLimit: 500, totalCount: 5, target: [] },
        multiColumnSortEnabled: false,
        activeSortCount: 0,
        filtersVisible: false,
        activeFilterCount: 0,
        filterToggleTarget: ["tree", "filtertoggle"],
        filterEnabled: true,
        sortEnabled: true,
        flatModeToggleEnabled: false,
        flatMode: false,
        flatToggleTarget: []
    };
}

/** Identifies a <tbody> row for ordering assertions: a data row by its nodeId, a load-more row by its button testid. */
function bodyRowIdentity(tr: Element): string | null {
    const nodeId = tr.getAttribute("data-node-id");
    if (nodeId) {
        return nodeId;
    }
    return tr.querySelector("button[data-testid]")?.getAttribute("data-testid") ?? null;
}

/**
 * Reads only the title text of a column header, excluding the sort toggle's Bootstrap Icons glyph
 * (a CSS ::before glyph on an <i> element, invisible to textContent) and the filter input --
 * the dedicated title span keeps header.textContent scoped to the title only.
 */
function headerTitle(header: HTMLElement): string | null | undefined {
    return header.querySelector(".tree-table-header-title")?.textContent;
}

/** Reads the Bootstrap Icons class (e.g. "bi-caret-right-fill") off a control's <i> glyph element. */
function iconClassOf(control: HTMLElement): string | undefined {
    const icon = control.querySelector("i.bi");
    const classes = icon ? Array.from(icon.classList) : [];
    return classes.find((c) => c !== "bi");
}

test("renders one column header per columns[] entry with its title", () => {
    render(<TreeTable node={realTreeTableNode()} />);

    const headers = screen.getAllByRole("columnheader").map(headerTitle);
    expect(headers).toEqual(["Name", "Value"]);
});

test("renders exactly one <tr> per rows[] entry, with cell values rendered by column key", () => {
    const { container } = render(<TreeTable node={realTreeTableNode()} />);

    expect(container.querySelectorAll("tbody tr[data-node-id]")).toHaveLength(3);

    const rowA = rowElement(container, "a");
    expect(within(rowA).getByText("Alpha")).toBeInTheDocument();
    expect(within(rowA).getByText("10")).toBeInTheDocument();

    const rowB = rowElement(container, "b");
    expect(within(rowB).getByText("Beta")).toBeInTheDocument();
    expect(within(rowB).getByText("20")).toBeInTheDocument();
});

test("renders a caret only for expandable rows, direction derived from row.expanded", () => {
    const { container } = render(<TreeTable node={realTreeTableNode()} />);

    // Row "a" is not expandable -> no caret at all.
    const rowA = rowElement(container, "a");
    expect(within(rowA).queryByTestId("tree-table-caret")).toBeNull();

    // Row "b" is expandable and collapsed (expanded=false) -> collapsed caret icon + "Expand" label.
    // Asserted via the Bootstrap Icons class (bi-caret-right-fill / bi-caret-down-fill) and aria-label,
    // direction derived purely from row.expanded (Cliff C5, stateless).
    const rowB = rowElement(container, "b");
    const caretB = within(rowB).getByTestId("tree-table-caret");
    expect(caretB).toHaveAttribute("aria-label", "Expand");
    expect(iconClassOf(caretB)).toBe("bi-caret-right-fill");
    expect(iconClassOf(caretB)).not.toBe("bi-caret-down-fill");

    // Row "c" is expandable and expanded (expanded=true) -> expanded caret icon + "Collapse" label.
    const rowC = rowElement(container, "c");
    const caretC = within(rowC).getByTestId("tree-table-caret");
    expect(caretC).toHaveAttribute("aria-label", "Collapse");
    expect(iconClassOf(caretC)).toBe("bi-caret-down-fill");
    expect(iconClassOf(caretC)).not.toBe("bi-caret-right-fill");
});

test("applies depth-driven indentation on the first column, generically off row.depth", () => {
    const { container } = render(<TreeTable node={realTreeTableNode()} />);

    const firstCellOf = (nodeId: string) => rowElement(container, nodeId).querySelector("td") as HTMLElement;

    // depth 0 -> no indent.
    expect(firstCellOf("a").style.paddingLeft).toBe("0em");
    expect(firstCellOf("b").style.paddingLeft).toBe("0em");

    // depth 1 -> indented relative to depth 0 (generic, not hardcoded to a fixed depth).
    expect(firstCellOf("c").style.paddingLeft).toBe("1.5em");
});

test("shows the load-more control when loadMore.available is true", () => {
    render(<TreeTable node={realTreeTableNode()} />);

    expect(screen.getByTestId("tree-table-load-more")).toBeInTheDocument();
});

test("hides the load-more control when loadMore.available is false, matching the backend's empty-target contract, and never dispatches", () => {
    // Per the backend contract (plan-76 Slice 4): target is [] (empty array) exactly when available is
    // false -- an empty target is treated as "no click affordance", mirroring how row target empties
    // are already treated.
    render(<TreeTable node={realTreeTableNode({
        loadMore: { available: false, nextOffset: 3, nextLimit: 500, totalCount: 3, target: [] }
    })} />);

    expect(screen.queryByTestId("tree-table-load-more")).toBeNull();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("clicking the load-more button dispatches loadMore.target as a server event via HandlerFactory/Backend.sendEvent", () => {
    const node = realTreeTableNode({
        loadMore: { available: true, nextOffset: 3, nextLimit: 500, totalCount: 10, target: ["tree", "loadmore"] }
    });

    render(<TreeTable node={node} />);

    const loadMoreButton = screen.getByTestId("tree-table-load-more");
    expect(loadMoreButton).not.toBeDisabled();

    fireEvent.click(loadMoreButton);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        ["tree", "loadmore"],
        "",
        undefined,
        undefined
    );
});

test("guards an empty loadMore.target even when available is true (defensive): renders the button disabled and never dispatches on click", () => {
    // Defensive guard (react4j-optional-per-item-handler-tsx-guard): the component must never dispatch
    // a null/empty target, regardless of whether `available` claims a click affordance exists.
    const node = realTreeTableNode({
        loadMore: { available: true, nextOffset: 3, nextLimit: 500, totalCount: 10, target: [] }
    });

    render(<TreeTable node={node} />);

    const loadMoreButton = screen.getByTestId("tree-table-load-more");
    expect(loadMoreButton).toBeDisabled();

    fireEvent.click(loadMoreButton);

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("clicking an expandable row's caret dispatches row.target as a server event via HandlerFactory/Backend.sendEvent", () => {
    const { container } = render(<TreeTable node={realTreeTableNode()} />);

    const rowB = rowElement(container, "b");
    const caret = within(rowB).getByTestId("tree-table-caret");
    expect(caret).not.toBeDisabled();

    fireEvent.click(caret);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        ["tree", "row1"],
        "",
        undefined,
        undefined
    );
});

test("a non-expandable row renders no caret and never dispatches", () => {
    const { container } = render(<TreeTable node={realTreeTableNode()} />);

    const rowA = rowElement(container, "a");
    expect(within(rowA).queryByTestId("tree-table-caret")).toBeNull();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("guards an empty row.target on an expandable row (defensive): caret renders disabled and never dispatches on click", () => {
    const node = realTreeTableNode();
    node.rows = node.rows.map((row) => (row.nodeId === "b" ? { ...row, target: [] } : row));

    const { container } = render(<TreeTable node={node} />);

    const rowB = rowElement(container, "b");
    const caret = within(rowB).getByTestId("tree-table-caret");
    expect(caret).toBeDisabled();

    fireEvent.click(caret);

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("renders an expanded row's childLoadMore control immediately after its descendant run, before the next same-or-shallower row", () => {
    const { container } = render(<TreeTable node={nodeWithExpandedChildAndPlacement()} />);

    const bodyRows = Array.from(container.querySelectorAll("tbody > tr"));
    const order = bodyRows.map(bodyRowIdentity);

    expect(order).toEqual(["a", "b", "c", "d", "tree-table-child-load-more-c", "e"]);
});

test("clicking an expanded row's childLoadMore control dispatches its own distinct target via Backend.sendEvent", () => {
    render(<TreeTable node={nodeWithExpandedChildAndPlacement()} />);

    const childLoadMoreButton = screen.getByTestId("tree-table-child-load-more-c");
    expect(childLoadMoreButton).not.toBeDisabled();

    fireEvent.click(childLoadMoreButton);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(
        ["tree", "row1", "row0", "loadmore"],
        "",
        undefined,
        undefined
    );
});

test("does not render a childLoadMore control for a collapsed row", () => {
    render(<TreeTable node={realTreeTableNode()} />);

    expect(screen.queryByTestId("tree-table-child-load-more-b")).toBeNull();
});

test("guards an empty childLoadMore.target (defensive): renders the child-load-more button disabled and never dispatches", () => {
    const node = nodeWithExpandedChildAndPlacement();
    node.rows = node.rows.map((row) =>
        row.nodeId === "c" && row.childLoadMore ? { ...row, childLoadMore: { ...row.childLoadMore, target: [] } } : row
    );

    render(<TreeTable node={node} />);

    const childLoadMoreButton = screen.getByTestId("tree-table-child-load-more-c");
    expect(childLoadMoreButton).toBeDisabled();

    fireEvent.click(childLoadMoreButton);

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("Renderer.tsx dispatches a TREETABLE node to the TreeTable component (registration)", () => {
    const node = realTreeTableNode();

    render(<>{Renderer.render(node)}</>);

    // Proves the dispatch actually reaches TreeTable's real render output, not a fallback/no-op.
    expect(screen.getByText("Alpha")).toBeInTheDocument();
    expect(screen.getAllByRole("columnheader").map(headerTitle)).toEqual(["Name", "Value"]);
});

// ---------------------------------------------------------------------------------------------
// Slice 6: per-column filter input (mechanism (b), client-authored) + sort toggle
// (mechanism (a), server-computed).
// ---------------------------------------------------------------------------------------------

/**
 * Real UIContextAccessor backed by a plain in-memory map -- NOT a mock -- so
 * DataContextManager.updateFieldByContext's write is genuinely observable through
 * getUIContextById(...).data afterwards (matches the accessor construction Input.test.tsx already
 * uses for the same reason: the field-write mechanism must be exercised for real, not stubbed away).
 */
function createUIContextAccessor(): UIContextAccessor {
    const contexts: { [contextId: string]: UIContext } = {
        "": { contextId: "", data: {}, internalData: {}, updateCounter: 0 }
    };
    return {
        getUIContextById: (contextId: string) => contexts[contextId],
        updateUIContext: (uiContext: UIContext) => {
            contexts[uiContext.contextId] = uiContext;
        },
        initializeUIContext: (uiContext?: UIContextDataNode) => {
            if (uiContext) {
                contexts[uiContext.contextId] = { ...uiContext, updateCounter: 0 } as UIContext;
            }
        }
    };
}

function nodeWithFilterAndSortColumns(): TreeTableNode {
    return {
        type: TreeTable.TYPE,
        target: ["tree"],
        columns: [
            {
                key: "name",
                title: "Name",
                filterValue: "Al",
                filterFieldKey: "treetable.tree.filter.name",
                filterTarget: ["tree", "filter0"],
                sortDirection: "ASCENDING",
                sortTarget: ["tree", "sort0"],
                sortPriority: 0,
                sortPriorityFieldKey: null,
                sortPriorityReorderTarget: [],
                filterable: true,
                sortable: true
            },
            {
                key: "value",
                title: "Value",
                filterValue: null,
                filterFieldKey: "treetable.tree.filter.value",
                filterTarget: ["tree", "filter1"],
                sortDirection: null,
                sortTarget: ["tree", "sort1"],
                sortPriority: 0,
                sortPriorityFieldKey: null,
                sortPriorityReorderTarget: [],
                filterable: true,
                sortable: true
            }
        ],
        rows: [
            { nodeId: "a", depth: 0, expandable: false, expanded: false, cells: { name: "Alpha", value: "10" }, target: ["tree", "row0"] }
        ],
        loadMore: { available: false, nextOffset: 1, nextLimit: 500, totalCount: 1, target: [] },
        multiColumnSortEnabled: false,
        activeSortCount: 0,
        // filtersVisible defaults to true in THIS fixture (unlike realTreeTableNode()) because every
        // pre-existing test below exercises the filter <input>'s write/dispatch mechanics, which only
        // mounts when the row is visible -- Slice 7's own visibility-gating tests override this back to
        // false explicitly where they need the collapsed case.
        filtersVisible: true,
        activeFilterCount: 1,
        filterToggleTarget: ["tree", "filtertoggle"],
        filterEnabled: true,
        sortEnabled: true,
        flatModeToggleEnabled: false,
        flatMode: false,
        flatToggleTarget: []
    };
}

test("renders a filter input seeded from column.filterValue and a sort toggle reflecting column.sortDirection, per column", () => {
    render(<TreeTable node={nodeWithFilterAndSortColumns()} />);

    const nameFilter = screen.getByTestId("tree-table-filter-name") as HTMLInputElement;
    expect(nameFilter.value).toBe("Al");
    const valueFilter = screen.getByTestId("tree-table-filter-value") as HTMLInputElement;
    expect(valueFilter.value).toBe("");

    // Asserted via the Bootstrap Icons class, keyed off column.sortDirection.
    const nameSortIcon = screen.getByTestId("tree-table-sort-name");
    expect(iconClassOf(nameSortIcon)).toBe("bi-caret-up-fill"); // ASCENDING
    expect(nameSortIcon).toHaveAttribute("aria-label", "Sort by Name (ASCENDING)");
    const valueSortIcon = screen.getByTestId("tree-table-sort-value");
    expect(iconClassOf(valueSortIcon)).toBe("bi-arrow-down-up"); // neutral/no sort
    expect(iconClassOf(valueSortIcon)).not.toBe("bi-caret-up-fill");
    expect(iconClassOf(valueSortIcon)).not.toBe("bi-caret-down-fill");
    expect(valueSortIcon).toHaveAttribute("aria-label", "Sort by Value");
});

test("renders the descending sort glyph when column.sortDirection is DESCENDING", () => {
    const node = nodeWithFilterAndSortColumns();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, sortDirection: "DESCENDING" } : column));

    render(<TreeTable node={node} />);

    const nameSortIcon = screen.getByTestId("tree-table-sort-name");
    expect(nameSortIcon).toHaveAttribute("aria-label", "Sort by Name (DESCENDING)");
    expect(iconClassOf(nameSortIcon)).toBe("bi-caret-down-fill"); // DESCENDING
    expect(iconClassOf(nameSortIcon)).not.toBe("bi-caret-up-fill");
    expect(iconClassOf(nameSortIcon)).not.toBe("bi-arrow-down-up");
});

test("does not change the existing per-column-header title text (the dedicated title span stays title-only; the sort glyph/filter input render alongside it, not inside it)", () => {
    render(<TreeTable node={nodeWithFilterAndSortColumns()} />);

    const headers = screen.getAllByRole("columnheader").map(headerTitle);
    expect(headers).toEqual(["Name", "Value"]);
});

test("applying a filter on blur writes filterFieldKey into the submitted Data THEN dispatches filterTarget", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    const callOrder: string[] = [];
    jest.spyOn(uiContextAccessor, "updateUIContext");
    mockedSendEvent.mockImplementation(() => {
        callOrder.push("dispatch");
        return Promise.resolve() as any;
    });

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={nodeWithFilterAndSortColumns()} />
        </RenderingSupportContext.Provider>
    );

    const nameFilter = screen.getByTestId("tree-table-filter-name");
    fireEvent.change(nameFilter, { target: { value: "Alice" } });
    // React 17+ implements onBlur via the delegated, BUBBLING native "focusout" event (blur itself
    // does not bubble) -- fireEvent.blur alone dispatches only a raw non-bubbling "blur" DOM event,
    // which React's root-delegated listener never observes, so the onBlur handler silently never
    // fires. fireEvent.focusOut is the RTL-documented way to reliably trigger a React onBlur in jsdom.
    fireEvent.focusOut(nameFilter);

    // The write: the raw typed text landed under filterFieldKey in the submitted Data.
    expect(uiContextAccessor.getUIContextById("").data["treetable.tree.filter.name"]).toBe("Alice");

    // The dispatch: filterTarget fired as a server event through Backend.sendEvent.
    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "filter0"], "", uiContextAccessor, nodeContextAccessor);

    // Order: the write must land in uiContextAccessor BEFORE the dispatch fires (write-then-dispatch).
    const updateUIContextCallOrderIndex = (uiContextAccessor.updateUIContext as jest.Mock).mock.invocationCallOrder[0];
    const sendEventCallOrderIndex = mockedSendEvent.mock.invocationCallOrder[0];
    expect(updateUIContextCallOrderIndex).toBeLessThan(sendEventCallOrderIndex);
});

test("applying a filter via Enter also writes filterFieldKey and dispatches filterTarget, and prevents default", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={nodeWithFilterAndSortColumns()} />
        </RenderingSupportContext.Provider>
    );

    const valueFilter = screen.getByTestId("tree-table-filter-value");
    fireEvent.change(valueFilter, { target: { value: "20" } });

    const keyDownEvent = new (window as any).KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
    const preventDefaultSpy = jest.spyOn(keyDownEvent, "preventDefault");
    fireEvent(valueFilter, keyDownEvent);

    expect(preventDefaultSpy).toHaveBeenCalledTimes(1);
    expect(uiContextAccessor.getUIContextById("").data["treetable.tree.filter.value"]).toBe("20");
    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "filter1"], "", uiContextAccessor, nodeContextAccessor);
});

test("a column with an empty/guarded filterTarget never writes the field nor dispatches on blur", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    const node = nodeWithFilterAndSortColumns();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, filterTarget: [] } : column));

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={node} />
        </RenderingSupportContext.Provider>
    );

    const nameFilter = screen.getByTestId("tree-table-filter-name");
    fireEvent.change(nameFilter, { target: { value: "Zed" } });
    fireEvent.focusOut(nameFilter);

    expect(uiContextAccessor.getUIContextById("").data["treetable.tree.filter.name"]).toBeUndefined();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("a column with a null filterFieldKey never writes nor dispatches even with a routable filterTarget (defensive)", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    const node = nodeWithFilterAndSortColumns();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, filterFieldKey: null } : column));

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={node} />
        </RenderingSupportContext.Provider>
    );

    fireEvent.change(screen.getByTestId("tree-table-filter-name"), { target: { value: "Zed" } });
    fireEvent.focusOut(screen.getByTestId("tree-table-filter-name"));

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("clicking a column's sort toggle dispatches sortTarget as a server event, with no client-side direction computation", () => {
    render(<TreeTable node={nodeWithFilterAndSortColumns()} />);

    fireEvent.click(screen.getByTestId("tree-table-sort-name"));

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "sort0"], "", undefined, undefined);
});

test("clicking a currently-unsorted column's sort toggle dispatches its own sortTarget", () => {
    render(<TreeTable node={nodeWithFilterAndSortColumns()} />);

    fireEvent.click(screen.getByTestId("tree-table-sort-value"));

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "sort1"], "", undefined, undefined);
});

test("a column with an empty/guarded sortTarget renders its sort toggle disabled and never dispatches on click", () => {
    const node = nodeWithFilterAndSortColumns();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, sortTarget: [] } : column));

    render(<TreeTable node={node} />);

    const sortToggle = screen.getByTestId("tree-table-sort-name");
    expect(sortToggle).toBeDisabled();

    fireEvent.click(sortToggle);

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

// ---------------------------------------------------------------------------------------------
// plan-81: in-header priority <select> for multi-column sort (mechanism (b), mirrors the filter
// <input>'s write-then-dispatch). Rendered only when multiColumnSortEnabled && column.sortPriority > 0
// && node.activeSortCount >= 2.
// ---------------------------------------------------------------------------------------------

/** Both columns active (priority 1 and 2) in multi-column-sort mode -- the priority <select> should render for both. */
function nodeWithMultiColumnSort(overrides?: Partial<TreeTableNode>): TreeTableNode {
    return {
        type: TreeTable.TYPE,
        target: ["tree"],
        columns: [
            {
                key: "name",
                title: "Name",
                filterTarget: [],
                sortTarget: ["tree", "sort0"],
                sortDirection: "ASCENDING",
                sortPriority: 1,
                sortPriorityFieldKey: "treetable.tree.sortpriority.name",
                sortPriorityReorderTarget: ["tree", "sortpriority0"],
                filterable: true,
                sortable: true
            },
            {
                key: "value",
                title: "Value",
                filterTarget: [],
                sortTarget: ["tree", "sort1"],
                sortDirection: "DESCENDING",
                sortPriority: 2,
                sortPriorityFieldKey: "treetable.tree.sortpriority.value",
                sortPriorityReorderTarget: ["tree", "sortpriority1"],
                filterable: true,
                sortable: true
            }
        ],
        rows: [
            { nodeId: "a", depth: 0, expandable: false, expanded: false, cells: { name: "Alpha", value: "10" }, target: ["tree", "row0"] }
        ],
        loadMore: { available: false, nextOffset: 1, nextLimit: 500, totalCount: 1, target: [] },
        multiColumnSortEnabled: true,
        activeSortCount: 2,
        filtersVisible: false,
        activeFilterCount: 0,
        filterToggleTarget: ["tree", "filtertoggle"],
        filterEnabled: true,
        sortEnabled: true,
        flatModeToggleEnabled: false,
        flatMode: false,
        flatToggleTarget: [],
        ...overrides
    };
}

test("renders a priority select per active column, valued at column.sortPriority, offering options 1..activeSortCount, when multiColumnSortEnabled and activeSortCount>=2", () => {
    render(<TreeTable node={nodeWithMultiColumnSort()} />);

    const nameSelect = screen.getByTestId("tree-table-sort-priority-name") as HTMLSelectElement;
    expect(nameSelect.value).toBe("1");
    expect(Array.from(nameSelect.options).map((o) => o.value)).toEqual(["1", "2"]);

    const valueSelect = screen.getByTestId("tree-table-sort-priority-value") as HTMLSelectElement;
    expect(valueSelect.value).toBe("2");
    expect(Array.from(valueSelect.options).map((o) => o.value)).toEqual(["1", "2"]);
});

test("does not render the priority select when multiColumnSortEnabled is false (single-column mode) even though the column is active", () => {
    const node = nodeWithMultiColumnSort({ multiColumnSortEnabled: false });

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-sort-priority-name")).toBeNull();
    expect(screen.queryByTestId("tree-table-sort-priority-value")).toBeNull();
    // The plain caret still renders -- only the extra re-rank control is withheld.
    expect(screen.getByTestId("tree-table-sort-name")).toBeInTheDocument();
});

test("does not render the priority select when activeSortCount is 1 (only one active sort, ordering not meaningful)", () => {
    const node = nodeWithMultiColumnSort({ activeSortCount: 1 });
    node.columns = node.columns.map((column) => (column.key === "value" ? { ...column, sortPriority: 0, sortDirection: null } : column));

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-sort-priority-name")).toBeNull();
    expect(screen.queryByTestId("tree-table-sort-priority-value")).toBeNull();
    expect(screen.getByTestId("tree-table-sort-name")).toBeInTheDocument();
});

test("does not render the priority select for an inactive column (sortPriority 0) even when multiColumnSortEnabled and activeSortCount>=2", () => {
    const node = nodeWithMultiColumnSort();
    node.columns = node.columns.map((column) => (column.key === "value" ? { ...column, sortPriority: 0, sortDirection: null } : column));

    render(<TreeTable node={node} />);

    // "name" stays active -> its select still renders; "value" is now inactive -> no select for it.
    expect(screen.getByTestId("tree-table-sort-priority-name")).toBeInTheDocument();
    expect(screen.queryByTestId("tree-table-sort-priority-value")).toBeNull();
});

test("changing a column's priority select writes sortPriorityFieldKey into the submitted Data THEN dispatches sortPriorityReorderTarget", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    jest.spyOn(uiContextAccessor, "updateUIContext");

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={nodeWithMultiColumnSort()} />
        </RenderingSupportContext.Provider>
    );

    const nameSelect = screen.getByTestId("tree-table-sort-priority-name");
    fireEvent.change(nameSelect, { target: { value: "2" } });

    // The write: the chosen priority landed under sortPriorityFieldKey in the submitted Data.
    expect(uiContextAccessor.getUIContextById("").data["treetable.tree.sortpriority.name"]).toBe("2");

    // The dispatch: sortPriorityReorderTarget fired as a server event through Backend.sendEvent.
    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "sortpriority0"], "", uiContextAccessor, nodeContextAccessor);

    // Order: the write must land in uiContextAccessor BEFORE the dispatch fires (write-then-dispatch).
    const updateUIContextCallOrderIndex = (uiContextAccessor.updateUIContext as jest.Mock).mock.invocationCallOrder[0];
    const sendEventCallOrderIndex = mockedSendEvent.mock.invocationCallOrder[0];
    expect(updateUIContextCallOrderIndex).toBeLessThan(sendEventCallOrderIndex);
});

test("a column with a null sortPriorityFieldKey never writes nor dispatches even with a routable sortPriorityReorderTarget (defensive guard)", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    const node = nodeWithMultiColumnSort();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, sortPriorityFieldKey: null } : column));

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={node} />
        </RenderingSupportContext.Provider>
    );

    fireEvent.change(screen.getByTestId("tree-table-sort-priority-name"), { target: { value: "2" } });

    expect(uiContextAccessor.getUIContextById("").data["treetable.tree.sortpriority.name"]).toBeUndefined();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("a column with an empty/guarded sortPriorityReorderTarget never writes nor dispatches on change (defensive guard)", () => {
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;
    const node = nodeWithMultiColumnSort();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, sortPriorityReorderTarget: [] } : column));

    render(
        <RenderingSupportContext.Provider value={{ uiContextAccessor, nodeContextAccessor }}>
            <TreeTable node={node} />
        </RenderingSupportContext.Provider>
    );

    fireEvent.change(screen.getByTestId("tree-table-sort-priority-name"), { target: { value: "2" } });

    expect(uiContextAccessor.getUIContextById("").data["treetable.tree.sortpriority.name"]).toBeUndefined();
    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("regression: the priority select coexists with an unaffected caret/funnel/flat-toggle/filter-row/load-more", () => {
    const node = nodeWithMultiColumnSort({
        filtersVisible: true,
        activeFilterCount: 0,
        flatModeToggleEnabled: true,
        flatMode: false,
        flatToggleTarget: ["tree", "flattoggle"],
        loadMore: { available: true, nextOffset: 1, nextLimit: 500, totalCount: 5, target: ["tree", "loadmore"] }
    });
    node.columns = node.columns.map((column) => ({ ...column, filterable: true }));

    render(<TreeTable node={node} />);

    expect(screen.getByTestId("tree-table-sort-priority-name")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-sort-priority-value")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-sort-name")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-sort-value")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-filter-toggle")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-filter-row")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-flat-toggle")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-load-more")).toBeInTheDocument();
});

// ---------------------------------------------------------------------------------------------
// plan-77 / Slice 7: collapsible filter row -- funnel toggle (filled-vs-outline is the sole active-
// filter indicator, no count badge), conditional filter row.
// ---------------------------------------------------------------------------------------------

test("renders the funnel toggle button with the plain bi-funnel glyph and no badge when activeFilterCount is 0", () => {
    render(<TreeTable node={realTreeTableNode()} />);

    const toggle = screen.getByTestId("tree-table-filter-toggle");
    expect(toggle).toBeInTheDocument();
    expect(iconClassOf(toggle)).toBe("bi-funnel");
    expect(iconClassOf(toggle)).not.toBe("bi-funnel-fill");
    expect(screen.queryByTestId("tree-table-filter-toggle-badge")).toBeNull();
    expect(toggle).toHaveAttribute("aria-label", "Show filters");
});

test("shows the bi-funnel-fill glyph and no badge on the funnel toggle when activeFilterCount is greater than 0 (filled funnel is the sole visual indicator)", () => {
    const node = realTreeTableNode({ activeFilterCount: 2 });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-filter-toggle");
    expect(iconClassOf(toggle)).toBe("bi-funnel-fill");
    expect(iconClassOf(toggle)).not.toBe("bi-funnel");
    expect(screen.queryByTestId("tree-table-filter-toggle-badge")).toBeNull();
    expect(toggle).toHaveAttribute("aria-label", "Show filters (2 active)");
});

test("the funnel toggle's aria-label reflects filtersVisible=true as 'Hide filters'", () => {
    const node = realTreeTableNode({ filtersVisible: true });

    render(<TreeTable node={node} />);

    expect(screen.getByTestId("tree-table-filter-toggle")).toHaveAttribute("aria-label", "Hide filters");
});

test("clicking the funnel toggle dispatches filterToggleTarget as a server event via HandlerFactory/Backend.sendEvent", () => {
    const node = realTreeTableNode({ filterToggleTarget: ["tree", "filtertoggle"] });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-filter-toggle");
    expect(toggle).not.toBeDisabled();

    fireEvent.click(toggle);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "filtertoggle"], "", undefined, undefined);
});

test("guards an empty/guarded filterToggleTarget (defensive): funnel toggle renders disabled and never dispatches on click", () => {
    const node = realTreeTableNode({ filterToggleTarget: [] });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-filter-toggle");
    expect(toggle).toBeDisabled();

    fireEvent.click(toggle);

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("the filter input row is absent from the DOM when node.filtersVisible is false, even though filter data keeps being emitted", () => {
    const node = nodeWithFilterAndSortColumns();
    node.filtersVisible = false;

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-filter-row")).toBeNull();
    expect(screen.queryByTestId("tree-table-filter-name")).toBeNull();
    expect(screen.queryByTestId("tree-table-filter-value")).toBeNull();
    // The rest of the header (title/sort) is unaffected by the row being hidden.
    expect(screen.getAllByRole("columnheader").map(headerTitle)).toEqual(["Name", "Value"]);
});

test("the filter input row is present, one <input> per column seeded from column.filterValue, when node.filtersVisible is true", () => {
    const node = nodeWithFilterAndSortColumns();
    node.filtersVisible = true;

    render(<TreeTable node={node} />);

    expect(screen.getByTestId("tree-table-filter-row")).toBeInTheDocument();
    const nameFilter = screen.getByTestId("tree-table-filter-name") as HTMLInputElement;
    expect(nameFilter.value).toBe("Al");
    const valueFilter = screen.getByTestId("tree-table-filter-value") as HTMLInputElement;
    expect(valueFilter.value).toBe("");
});

test("filter row visibility follows node.filtersVisible purely, with no client-side memory across re-renders (component stays stateless)", () => {
    // Proves the component holds NO client useState for visibility: re-rendering the SAME mounted
    // instance with a flipped filtersVisible prop must flip the DOM immediately in both directions,
    // seeded values preserved from column.filterValue throughout (never cleared by a visibility flip).
    const hiddenNode = nodeWithFilterAndSortColumns();
    hiddenNode.filtersVisible = false;
    const { rerender } = render(<TreeTable node={hiddenNode} />);
    expect(screen.queryByTestId("tree-table-filter-row")).toBeNull();

    const visibleNode = nodeWithFilterAndSortColumns();
    visibleNode.filtersVisible = true;
    rerender(<TreeTable node={visibleNode} />);
    expect(screen.getByTestId("tree-table-filter-row")).toBeInTheDocument();
    expect((screen.getByTestId("tree-table-filter-name") as HTMLInputElement).value).toBe("Al");

    const hiddenAgainNode = nodeWithFilterAndSortColumns();
    hiddenAgainNode.filtersVisible = false;
    rerender(<TreeTable node={hiddenAgainNode} />);
    expect(screen.queryByTestId("tree-table-filter-row")).toBeNull();
});

// ---------------------------------------------------------------------------------------------
// plan-78: whole-feature filterEnabled/sortEnabled + per-column filterable/sortable gating.
// Gate on the booleans as the PRIMARY signal, not merely on the (also-emptied) Target fields.
// ---------------------------------------------------------------------------------------------

test("filterEnabled=false renders no funnel toggle at all, and no filter row even when filtersVisible is true", () => {
    const node = nodeWithFilterAndSortColumns();
    node.filterEnabled = false;
    node.filtersVisible = true;

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-filter-toggle")).toBeNull();
    expect(screen.queryByTestId("tree-table-filter-row")).toBeNull();
    expect(screen.queryByTestId("tree-table-filter-name")).toBeNull();
    expect(screen.queryByTestId("tree-table-filter-value")).toBeNull();
    // The rest of the header (title/sort) is unaffected.
    expect(screen.getAllByRole("columnheader").map(headerTitle)).toEqual(["Name", "Value"]);
});

test("sortEnabled=false renders no sort toggle in any column header, even though each column is individually sortable", () => {
    const node = nodeWithFilterAndSortColumns();
    node.sortEnabled = false;

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-sort-name")).toBeNull();
    expect(screen.queryByTestId("tree-table-sort-value")).toBeNull();
    // Title/filter row are unaffected.
    expect(screen.getAllByRole("columnheader").map(headerTitle)).toEqual(["Name", "Value"]);
});

test("a column with filterable=false renders an empty filter cell (no <input>) while a filterable=true sibling still renders its input", () => {
    const node = nodeWithFilterAndSortColumns();
    node.filtersVisible = true;
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, filterable: false } : column));

    const { container } = render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-filter-name")).toBeNull();
    const valueFilter = screen.getByTestId("tree-table-filter-value") as HTMLInputElement;
    expect(valueFilter.value).toBe("");

    // Column alignment stays 1:1 with the header/body: the non-filterable column still contributes an
    // empty <td> to the filter row, not a removed cell.
    const filterRow = screen.getByTestId("tree-table-filter-row");
    const filterCells = Array.from(filterRow.querySelectorAll("td"));
    expect(filterCells).toHaveLength(2);
    expect(filterCells[0].querySelector("input")).toBeNull();
    expect(filterCells[1].querySelector("input")).not.toBeNull();
});

test("a column with sortable=false renders no sort toggle for it, while a sortable=true sibling still renders its toggle", () => {
    const node = nodeWithFilterAndSortColumns();
    node.columns = node.columns.map((column) => (column.key === "name" ? { ...column, sortable: false } : column));

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-sort-name")).toBeNull();
    expect(screen.getByTestId("tree-table-sort-value")).toBeInTheDocument();
    // The non-sortable column's title still renders.
    expect(screen.getAllByRole("columnheader").map(headerTitle)).toEqual(["Name", "Value"]);
});

test("regression: with filterEnabled/sortEnabled and every column's filterable/sortable all true, funnel + filter inputs + sort toggles render as before", () => {
    const node = nodeWithFilterAndSortColumns();
    node.filtersVisible = true;

    render(<TreeTable node={node} />);

    expect(screen.getByTestId("tree-table-filter-toggle")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-filter-row")).toBeInTheDocument();
    expect((screen.getByTestId("tree-table-filter-name") as HTMLInputElement).value).toBe("Al");
    expect((screen.getByTestId("tree-table-filter-value") as HTMLInputElement).value).toBe("");
    expect(screen.getByTestId("tree-table-sort-name")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-sort-value")).toBeInTheDocument();
});

// ---------------------------------------------------------------------------------------------
// plan-80: flat/tree toggle -- rendered only when flatModeToggleEnabled, icon/aria-label reflect
// flatMode, dispatches flatToggleTarget through the same wrap-raw-Target-into-ServerHandler +
// HandlerFactory.onClick + empty-target-guard pattern as the funnel/sort/caret/load-more controls.
// ---------------------------------------------------------------------------------------------

test("renders no flat/tree toggle at all when flatModeToggleEnabled is false", () => {
    const node = realTreeTableNode({ flatModeToggleEnabled: false, flatMode: false, flatToggleTarget: [] });

    render(<TreeTable node={node} />);

    expect(screen.queryByTestId("tree-table-flat-toggle")).toBeNull();
});

test("renders the flat/tree toggle with the bi-list-ul glyph and 'Switch to flat list' aria-label when flatModeToggleEnabled is true and flatMode is false (tree mode)", () => {
    const node = realTreeTableNode({ flatModeToggleEnabled: true, flatMode: false, flatToggleTarget: ["tree", "flattoggle"] });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-flat-toggle");
    expect(toggle).toBeInTheDocument();
    expect(iconClassOf(toggle)).toBe("bi-list-ul");
    expect(iconClassOf(toggle)).not.toBe("bi-diagram-3");
    expect(toggle).toHaveAttribute("aria-label", "Switch to flat list");
});

test("renders the flat/tree toggle with the bi-diagram-3 glyph and 'Switch to tree view' aria-label when flatMode is true (flat mode)", () => {
    const node = realTreeTableNode({ flatModeToggleEnabled: true, flatMode: true, flatToggleTarget: ["tree", "flattoggle"] });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-flat-toggle");
    expect(iconClassOf(toggle)).toBe("bi-diagram-3");
    expect(iconClassOf(toggle)).not.toBe("bi-list-ul");
    expect(toggle).toHaveAttribute("aria-label", "Switch to tree view");
});

test("clicking the flat/tree toggle dispatches flatToggleTarget as a server event via HandlerFactory/Backend.sendEvent", () => {
    const node = realTreeTableNode({ flatModeToggleEnabled: true, flatMode: false, flatToggleTarget: ["tree", "flattoggle"] });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-flat-toggle");
    expect(toggle).not.toBeDisabled();

    fireEvent.click(toggle);

    expect(mockedSendEvent).toHaveBeenCalledTimes(1);
    expect(mockedSendEvent).toHaveBeenCalledWith(["tree", "flattoggle"], "", undefined, undefined);
});

test("guards an empty/guarded flatToggleTarget (defensive): flat/tree toggle renders disabled and never dispatches on click", () => {
    const node = realTreeTableNode({ flatModeToggleEnabled: true, flatMode: false, flatToggleTarget: [] });

    render(<TreeTable node={node} />);

    const toggle = screen.getByTestId("tree-table-flat-toggle");
    expect(toggle).toBeDisabled();

    fireEvent.click(toggle);

    expect(mockedSendEvent).not.toHaveBeenCalled();
});

test("regression: flatModeToggleEnabled has no effect on the existing caret/sort/funnel/filter/load-more controls", () => {
    const node = nodeWithFilterAndSortColumns();
    node.flatModeToggleEnabled = true;
    node.flatMode = false;
    node.flatToggleTarget = ["tree", "flattoggle"];
    node.filtersVisible = true;

    const { container } = render(<TreeTable node={node} />);

    expect(screen.getByTestId("tree-table-flat-toggle")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-filter-toggle")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-filter-row")).toBeInTheDocument();
    expect((screen.getByTestId("tree-table-filter-name") as HTMLInputElement).value).toBe("Al");
    expect(screen.getByTestId("tree-table-sort-name")).toBeInTheDocument();
    expect(screen.getByTestId("tree-table-sort-value")).toBeInTheDocument();
    expect(container.querySelectorAll("tbody tr[data-node-id]")).toHaveLength(1);
});
