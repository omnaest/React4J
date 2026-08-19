import React from "react";
import "./TreeTable.css";
import { Node, RenderingSupport, Target } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { HandlerFactory, ServerHandler } from "../handler/Handler";
import { UIContextAccessor } from "../data/DataContextManager";

/**
 * Frozen node-JSON contract emitted by TreeTableRendererImpl (plan-76 Slice 2, backend, DONE):
 * a flattened list of rows (each own depth/expandable/expanded/cells), NOT a recursively-nested
 * node object -- so it maps directly to a real <table><tbody> and every row keeps its own flat
 * server Target (Cliff C4).
 */
/** This column's current sort direction, or `null` when not currently sorted. */
export type TreeTableSortDirection = "ASCENDING" | "DESCENDING" | null;

export interface TreeTableColumn {
    key: string;
    title: string;
    /**
     * Slice 6 (mechanism (b), client-authored filter value): the column's currently-active filter
     * text, or `null`/absent when no filter is active. Seeds the filter `<input>` on render; the
     * component never computes this itself.
     */
    filterValue?: string | null;
    /**
     * Slice 6: the submitted-Data field key the frontend must write the raw typed filter text into
     * BEFORE dispatching {@link filterTarget}. `null` when the column has no routable grid Location
     * yet (defensive -- filtering is then a no-op).
     */
    filterFieldKey?: string | null;
    /** Slice 6: routable Target for this column's filter control. Empty ([]) when not clickable. */
    filterTarget: Target;
    /**
     * Slice 6 (mechanism (a), server-computed sort): the column's current sort direction. The
     * component only ever DISPLAYS this value -- clicking the toggle just dispatches
     * {@link sortTarget}; the server cycles ASCENDING -> DESCENDING -> none.
     */
    sortDirection?: TreeTableSortDirection;
    /** Slice 6: routable Target for this column's sort toggle. Empty ([]) when not clickable. */
    sortTarget: Target;
    /**
     * Multi-column-sort feature (plan-81, backend track DONE): this column's 1-based position in the
     * active sort list, or `0` when the column is not currently sorted. Only meaningful for rendering
     * the priority re-rank `<select>` when {@link TreeTableNode.multiColumnSortEnabled} is `true` AND
     * `TreeTableNode.activeSortCount >= 2`.
     */
    sortPriority: number;
    /**
     * Multi-column-sort feature (plan-81, mechanism (b), mirrors {@link filterFieldKey}): the
     * submitted-Data field key the frontend must write a chosen new 1-based priority into BEFORE
     * dispatching {@link sortPriorityReorderTarget}. `null` unless
     * `TreeTableNode.multiColumnSortEnabled` is `true` AND this column is currently active
     * (`sortPriority > 0`).
     */
    sortPriorityFieldKey: string | null;
    /**
     * Multi-column-sort feature (plan-81): routable Target for this column's priority re-rank
     * `<select>`. Empty ([]) whenever {@link sortPriorityFieldKey} is `null` (multi-column sort
     * disabled, or this column is not currently active).
     */
    sortPriorityReorderTarget: Target;
    /**
     * Whole-feature enable flag for THIS column's filter control (backend track, plan-78). When
     * `false` the component renders an empty `<td>` in the filter row (keeps 1:1 column alignment)
     * instead of a real `<input>` -- gated on this boolean as the primary signal, not merely on
     * `filterTarget` being empty (the backend already empties the target too, but the boolean is
     * authoritative).
     */
    filterable: boolean;
    /**
     * Whole-feature enable flag for THIS column's sort toggle (backend track, plan-78). When `false`
     * the header renders only the title, no sort toggle at all -- gated on this boolean as the
     * primary signal, not merely on `sortTarget` being empty.
     */
    sortable: boolean;
}

export interface TreeTableRow {
    nodeId: string;
    /** Indentation level. Always render generically off this value -- never assume a fixed depth. */
    depth: number;
    /** Whether to draw an expand/collapse caret for this row. */
    expandable: boolean;
    /** Server-owned expansion state (Cliff C5) -- caret direction derives from this, no local React state. */
    expanded: boolean;
    /** columnKey -> stringified cell value. */
    cells: { [columnKey: string]: string };
    /**
     * For an expandable row this is ALSO the caret's click Target (plan-76 Slice 5) -- the backend
     * deliberately reuses this field rather than adding a separate expandTarget. Empty ([]) when the
     * row is not clickable.
     */
    target: Target;
    /**
     * Present (non-null) iff expanded===true; null/absent for collapsed or non-expandable rows. Same
     * shape as the root loadMore, with its own distinct target (plan-76 Slice 5).
     */
    childLoadMore?: TreeTableLoadMore | null;
}

export interface TreeTableLoadMore {
    available: boolean;
    nextOffset: number;
    nextLimit: number;
    totalCount: number | null;
    target: Target;
}

export interface TreeTableNode extends Node {
    columns: TreeTableColumn[];
    rows: TreeTableRow[];
    loadMore: TreeTableLoadMore;
    /**
     * Multi-column-sort feature (plan-81, backend track DONE): whole-feature enable flag (build-time
     * component configuration, constant across renders). When `true`, each actively-sorted column
     * (`sortPriority > 0`) shows its 1-based priority, and -- once `activeSortCount >= 2` -- a
     * re-rank `<select>` alongside its sort caret.
     */
    multiColumnSortEnabled: boolean;
    /**
     * Multi-column-sort feature (plan-81): the number of columns currently present in the active sort
     * list -- `0` when nothing is sorted. Drives the valid `1..activeSortCount` option range for each
     * column's priority re-rank `<select>`, and gates the select's very rendering (no re-rank control
     * shown for a single-column sort, since there is nothing to re-rank against).
     */
    activeSortCount: number;
    /**
     * Slice 7 (collapsible filter row): whether to render the per-column filter input row.
     * Server-owned (survives re-renders, lives in submitted Data) -- the component never holds its
     * own visibility state. Defaults to `false` (collapsed) on the backend.
     */
    filtersVisible: boolean;

    /** Whether the header stays put while the body scrolls (see TreeTable.withStickyHeader). */
    stickyHeader?: boolean;
    /** Accessible name for the grid as a whole (see TreeTable.withAriaLabel). */
    ariaLabel?: string;
    /**
     * Slice 7: count of columns with a non-empty active filter value -- drives the funnel toggle's
     * filled-vs-outline "filters are active" state (and the accessible aria-label suffix) even while
     * the input row itself is hidden. No longer surfaced as a visual badge (see renderFilterToggle) --
     * the filled funnel glyph alone is the visual indicator.
     */
    activeFilterCount: number;
    /** Slice 7: routable Target for the funnel show/hide toggle. Always routable (never empty). */
    filterToggleTarget: Target;
    /**
     * Whole-filter-feature enable flag (backend track, plan-78). When `false`: no funnel toggle at
     * all, and the filter input row never renders even if `filtersVisible` is true. Gated on this
     * boolean as the primary signal -- the backend already empties `filterToggleTarget`/per-column
     * `filterTarget` in the disabled case, but that is a secondary defensive guard, not the gate.
     */
    filterEnabled: boolean;
    /**
     * Whole-sort-feature enable flag (backend track, plan-78). When `false`, no column renders a sort
     * toggle regardless of its own `sortable` flag. Gated on this boolean as the primary signal.
     */
    sortEnabled: boolean;
    /**
     * Whole-feature enable flag for the flat/tree toggle control (backend track, plan-80). When `false`
     * no toggle control is rendered at all -- `flatMode` still reflects the table's current mode, but
     * there is no user-facing way to change it. Gated on this boolean as the primary signal, mirroring
     * `filterEnabled`/`sortEnabled`.
     */
    flatModeToggleEnabled: boolean;
    /**
     * Whether this render is in flat mode (plan-80). Server-owned (Cliff C5 pattern, mirrors
     * `filtersVisible`) -- purely displayed here; the component never computes it. When `true` every row
     * already carries `depth: 0`/`expandable: false`/`expanded: false`/`childLoadMore: null`, so the
     * existing row rendering already renders flat with no special-casing needed.
     */
    flatMode: boolean;
    /** Routable Target for the flat/tree toggle control. Empty ([]) when `flatModeToggleEnabled` is false. */
    flatToggleTarget: Target;
}

export interface Props {
    node: TreeTableNode;
}

/** Indentation applied per depth level on the first (label) column. */
const DEPTH_INDENT_EM = 1.5;

/**
 * Real TREETABLE renderer (plan-76 Slice 3; load-more dispatch added Slice 4; caret expand/collapse
 * dispatch + per-expanded-row childLoadMore added Slice 5; per-column filter input + sort toggle added
 * Slice 6; collapsible filter row + funnel toggle + active-filter badge added plan-77/Slice 7). Stays
 * stateless w.r.t. server-owned state -- no client window/expansion/sort-direction/filter-visibility
 * state; the server owns the query, the expanded set, the sort order, and whether the filter row is
 * shown (`node.filtersVisible`, survives re-renders since it lives in submitted Data). The filter
 * `<input>` keeps a transient UNCONTROLLED typing buffer only (seeded from `column.filterValue`, the
 * actual source of truth on render) -- this component never computes a filter/sort result itself.
 */
export class TreeTable extends React.Component<Props, {}> {
    public static TYPE: string = "TREETABLE";
    public static contextType = RenderingSupportContext;

    /**
     * Bootstrap Icons glyph per active sort direction (the bootstrap-icons font is bundled
     * framework-wide, see App.tsx import in App.tsx, and its `.bi-*::before` glyph rules are already
     * used elsewhere in this codebase -- see Icon2.tsx's `<i className={"bi bi-" + icon}>` pattern).
     * Superseding the earlier plain-Unicode glyphs (▲/▼/↕): on Windows those Unicode triangle
     * characters render with emoji presentation (chunky colored glyphs) rather than crisp text glyphs,
     * which is the ugliness this swap fixes. A neutral/no-sort column shows a muted up-down icon.
     */
    private static readonly SORT_ICON: { [direction: string]: string } = {
        ASCENDING: "bi-caret-up-fill",
        DESCENDING: "bi-caret-down-fill"
    };
    private static readonly SORT_ICON_NEUTRAL = "bi-arrow-down-up";

    /** Bootstrap Icons caret glyphs -- see SORT_ICON comment above for why these replace the old Unicode triangles. */
    private static readonly CARET_ICON_EXPANDED = "bi-caret-down-fill";
    private static readonly CARET_ICON_COLLAPSED = "bi-caret-right-fill";

    /**
     * Funnel toggle glyphs (Slice 7): plain (outline) when no column has an active filter, filled when
     * `activeFilterCount > 0` -- the filled funnel is the SOLE visual affordance that tells the user
     * results are filtered even while the input row itself is collapsed/hidden (no count badge --
     * removed per the simplified-affordance follow-up; the active count remains in the aria-label for
     * accessibility).
     */
    private static readonly FUNNEL_ICON = "bi-funnel";
    private static readonly FUNNEL_ICON_ACTIVE = "bi-funnel-fill";

    /**
     * Flat/tree toggle glyphs (plan-80): `bi-list-ul` in tree mode (the click flattens the tree into a
     * list) and `bi-diagram-3` in flat mode (the click switches back to the hierarchy) -- the icon
     * always depicts the ACTION the click performs, mirroring the funnel toggle's icon-as-affordance
     * convention rather than depicting current state.
     */
    private static readonly FLAT_TOGGLE_ICON_TREE_MODE = "bi-list-ul";
    private static readonly FLAT_TOGGLE_ICON_FLAT_MODE = "bi-diagram-3";

    /**
     * Writes `value` into `fieldKey` on the shared `""` ui-context's submitted Data, using the SAME
     * `uiContextAccessor.getUIContextById(...)` / `.updateUIContext(...)` primitives
     * `DataContextManager.updateFieldByContext` uses internally (the established Form.tsx/FileUpload.tsx
     * write-a-field-before-dispatch mechanism) -- but NOT that wrapper function itself, because it
     * guards on `if (contextId)`, which is falsy for an EMPTY-STRING contextId and would silently
     * no-op the write. TreeTable's caret/load-more/expand controls already dispatch with
     * `contextId: ""` (frozen Slices 4/5 behavior, out of this slice's scope to change), so this
     * inlines the identical accessor-level write (skip-if-unchanged, bump updateCounter, notify) past
     * that one incompatible guard rather than introducing a new mechanism or a new contextId
     * convention. Generalized (plan-81) from the original filter-only `writeFilterField` -- the SAME
     * write-then-dispatch primitive also backs the priority re-rank `<select>` in
     * {@link commitSortPriority}, so this stays the single shared accessor helper rather than two
     * near-duplicate ones.
     */
    private writeDataField(fieldKey: string, value: string, uiContextAccessor: UIContextAccessor | undefined): void {
        const uiContext = uiContextAccessor?.getUIContextById("");
        if (uiContext && uiContext.data[fieldKey] !== value) {
            uiContext.data[fieldKey] = value;
            uiContext.updateCounter++;
            uiContextAccessor?.updateUIContext(uiContext);
        }
    }

    /**
     * Writes the raw typed filter text into `column.filterFieldKey` and THEN dispatches
     * `column.filterTarget` (Slice 6, Cliff C1a mechanism (b) -- unlike load-more/expand/sort, the
     * filter VALUE originates client-side, so it must reach the submitted Data before the server
     * round trip fires). Guards defensively (react4j-optional-per-item-handler-tsx-guard): never write
     * nor dispatch when the column has no routable filterFieldKey/filterTarget -- the write and the
     * notify are meaningless without a server handler to receive them.
     */
    private commitFilter(column: TreeTableColumn, value: string, renderingSupport: RenderingSupport | undefined): void {
        const hasTarget = Array.isArray(column.filterTarget) && column.filterTarget.length > 0;
        if (!column.filterFieldKey || !hasTarget) {
            return;
        }
        this.writeDataField(column.filterFieldKey, value, renderingSupport?.uiContextAccessor);
        const handler: ServerHandler = { type: "SERVER", target: column.filterTarget, contextId: "" };
        HandlerFactory.handleEvent(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor);
    }

    /**
     * Writes the chosen 1-based priority into `column.sortPriorityFieldKey` and THEN dispatches
     * `column.sortPriorityReorderTarget` (plan-81, Cliff C1a mechanism (b) -- mirrors
     * {@link commitFilter} exactly: the re-rank VALUE originates client-side from the `<select>`, so it
     * must reach the submitted Data before the server round trip fires). Guards defensively
     * (react4j-optional-per-item-handler-tsx-guard): never write nor dispatch when the column has no
     * routable `sortPriorityFieldKey`/`sortPriorityReorderTarget` -- the write and the notify are
     * meaningless without a server handler to receive them.
     */
    private commitSortPriority(column: TreeTableColumn, value: string, renderingSupport: RenderingSupport | undefined): void {
        const hasTarget = Array.isArray(column.sortPriorityReorderTarget) && column.sortPriorityReorderTarget.length > 0;
        if (!column.sortPriorityFieldKey || !hasTarget) {
            return;
        }
        this.writeDataField(column.sortPriorityFieldKey, value, renderingSupport?.uiContextAccessor);
        const handler: ServerHandler = { type: "SERVER", target: column.sortPriorityReorderTarget, contextId: "" };
        HandlerFactory.handleEvent(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor);
    }

    /**
     * Filter `<input>` (Slice 6, mechanism (b)), seeded from `column.filterValue` at mount via
     * `defaultValue` -- kept UNCONTROLLED so typing never round-trips per keystroke and never fights a
     * React re-render mid-edit; the source of truth on render is still `column.filterValue`, this is
     * only a transient typing buffer (per the brief, an uncontrolled input is the allowed exception to
     * "component stays stateless"). Applies on Enter or blur -- the least-surprising trigger (mirrors
     * Input.tsx's existing submitOnEnter convention) and avoids a server round-trip per keystroke that
     * a debounced onChange would still incur. Since plan-77/Slice 7 this renders inside the collapsible
     * filter row (see render()'s conditional second `<thead>` `<tr>`), gated on `node.filtersVisible`
     * -- collapsing the row never clears or resets this value; `column.filterValue` keeps being emitted
     * regardless, so the input is simply not mounted while hidden.
     */
    private renderFilterInput(column: TreeTableColumn, renderingSupport: RenderingSupport | undefined): JSX.Element {
        const commit = (event: React.SyntheticEvent<HTMLInputElement>) => {
            this.commitFilter(column, (event.target as HTMLInputElement).value, renderingSupport);
        };
        return (
            <input
                type="text"
                className="form-control form-control-sm tree-table-filter-input"
                data-testid={`tree-table-filter-${column.key}`}
                aria-label={`Filter ${column.title}`}
                defaultValue={column.filterValue ?? ""}
                onBlur={commit}
                onKeyDown={(event) => {
                    if (event.key === "Enter") {
                        event.preventDefault();
                        commit(event);
                    }
                }}
            />
        );
    }

    /**
     * Sort toggle (Slice 6, mechanism (a), server-computed): purely displays `column.sortDirection`
     * and dispatches `column.sortTarget` on click -- the server cycles ASCENDING -> DESCENDING -> none,
     * this component never computes the next state. Same ServerHandler-wrap + guard pattern as the
     * caret/load-more controls (react4j-optional-per-item-handler-tsx-guard).
     */
    private renderSortToggle(column: TreeTableColumn, renderingSupport: RenderingSupport | undefined): JSX.Element {
        const iconClass = column.sortDirection ? TreeTable.SORT_ICON[column.sortDirection] : TreeTable.SORT_ICON_NEUTRAL;
        const hasTarget = Array.isArray(column.sortTarget) && column.sortTarget.length > 0;
        const handler: ServerHandler | undefined = hasTarget ? {
            type: "SERVER",
            target: column.sortTarget,
            contextId: ""
        } : undefined;

        return (
            <button
                type="button"
                className="tree-table-sort-toggle btn btn-link p-0"
                data-testid={`tree-table-sort-${column.key}`}
                aria-label={`Sort by ${column.title}${column.sortDirection ? ` (${column.sortDirection})` : ""}`}
                disabled={!hasTarget}
                style={{
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                    width: "1.25rem",
                    height: "1.25rem",
                    fontSize: column.sortDirection ? "0.75rem" : "0.9rem",
                    lineHeight: 1,
                    opacity: column.sortDirection ? 1 : 0.5
                }}
                onClick={handler ? HandlerFactory.onClick(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
            >
                <i className={`bi ${iconClass}`} aria-hidden="true"></i>
            </button>
        );
    }

    /**
     * Whether the priority re-rank `<select>` should render for `column` (plan-81): multi-column sort
     * must be enabled, the column must currently be active (`sortPriority > 0`), and there must be at
     * least two active sorts for a re-rank to be meaningful (`activeSortCount >= 2`) -- otherwise this
     * renders nothing extra, just the plain caret from {@link renderSortToggle}.
     */
    private shouldRenderSortPrioritySelect(node: TreeTableNode, column: TreeTableColumn): boolean {
        return node.multiColumnSortEnabled === true && column.sortPriority > 0 && node.activeSortCount >= 2;
    }

    /**
     * Priority re-rank `<select>` (plan-81, mechanism (b), mirrors the filter `<input>`'s
     * write-then-dispatch): shown next to {@link renderSortToggle}'s caret, only when
     * {@link shouldRenderSortPrioritySelect} holds. Purely displays `column.sortPriority` as the
     * current selection and offers every position `1..node.activeSortCount` -- the server owns the
     * actual reordering; this component never computes the new sort list itself. On change, writes the
     * chosen 1-based priority into `column.sortPriorityFieldKey` THEN dispatches
     * `column.sortPriorityReorderTarget` via {@link commitSortPriority} (guarded there against a
     * null/empty field key or target).
     */
    private renderSortPrioritySelect(node: TreeTableNode, column: TreeTableColumn, renderingSupport: RenderingSupport | undefined): JSX.Element {
        const options: number[] = [];
        for (let priority = 1; priority <= node.activeSortCount; priority++) {
            options.push(priority);
        }

        return (
            <select
                className="form-select form-select-sm tree-table-sort-priority-select"
                data-testid={`tree-table-sort-priority-${column.key}`}
                aria-label={`Sort priority for ${column.title}`}
                value={column.sortPriority}
                style={{
                    display: "inline-block",
                    width: "auto",
                    marginLeft: "0.25rem",
                    padding: "0 0.25rem",
                    fontSize: "0.75rem",
                    lineHeight: 1.2
                }}
                onChange={(event: React.ChangeEvent<HTMLSelectElement>) => {
                    this.commitSortPriority(column, event.target.value, renderingSupport);
                }}
            >
                {options.map((priority) => (
                    <option key={priority} value={priority}>
                        {priority}
                    </option>
                ))}
            </select>
        );
    }

    /**
     * Funnel show/hide toggle (Slice 7), placed top-left -- prepended to the first column's header
     * cell (the current `<thead>` has a single header `<tr>` with one `<th>` per column and no
     * separate caret/indent column, so prepending here is the cleanest fit rather than introducing an
     * extra header column that would misalign with the body's per-row `<td>` count). Dispatches
     * `node.filterToggleTarget` through the same wrap-raw-Target-into-ServerHandler +
     * HandlerFactory.onClick path every other per-node click in this component uses
     * (react4j-bare-target-node-field-wrap-serverhandler-through-handlerfactory), guarded
     * (react4j-optional-per-item-handler-tsx-guard) against a null/empty target. Purely displays
     * `node.filtersVisible`/`node.activeFilterCount` -- the server owns both the visibility flag and
     * the active-filter count; this component never computes either.
     */
    private renderFilterToggle(node: TreeTableNode, renderingSupport: RenderingSupport | undefined): JSX.Element {
        const activeFilterCount = node.activeFilterCount ?? 0;
        const hasActiveFilters = activeFilterCount > 0;
        const filtersVisible = node.filtersVisible === true;
        const hasTarget = Array.isArray(node.filterToggleTarget) && node.filterToggleTarget.length > 0;
        const handler: ServerHandler | undefined = hasTarget ? {
            type: "SERVER",
            target: node.filterToggleTarget,
            contextId: ""
        } : undefined;

        const baseLabel = filtersVisible ? "Hide filters" : "Show filters";
        const ariaLabel = hasActiveFilters ? `${baseLabel} (${activeFilterCount} active)` : baseLabel;
        const iconClass = hasActiveFilters ? TreeTable.FUNNEL_ICON_ACTIVE : TreeTable.FUNNEL_ICON;

        return (
            <button
                type="button"
                className="tree-table-filter-toggle btn btn-link p-0"
                data-testid="tree-table-filter-toggle"
                aria-label={ariaLabel}
                disabled={!hasTarget}
                style={{
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                    width: "1.25rem",
                    height: "1.25rem",
                    fontSize: "0.9rem",
                    lineHeight: 1,
                    marginRight: "0.35rem"
                }}
                onClick={handler ? HandlerFactory.onClick(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
            >
                <i className={`bi ${iconClass}`} aria-hidden="true"></i>
            </button>
        );
    }

    /**
     * Flat/tree toggle (plan-80), placed in the first column's header cell alongside the funnel toggle
     * (react4j-treetable-table-wide-control-in-existing-header-cell-not-new-column -- a table-wide
     * control must be embedded inside an existing column's `<th>`, never a new header-only column, to
     * keep the header's `<th>` count aligned 1:1 with the body's `<td>` count). Only rendered when
     * `node.flatModeToggleEnabled` is true. Dispatches `node.flatToggleTarget` through the same
     * wrap-raw-Target-into-ServerHandler + HandlerFactory.onClick path every other per-node click in
     * this component uses (react4j-bare-target-node-field-wrap-serverhandler-through-handlerfactory),
     * guarded (react4j-optional-per-item-handler-tsx-guard) against a null/empty target. Purely displays
     * `node.flatMode` -- the server owns the mode, this component never computes it.
     */
    private renderFlatModeToggle(node: TreeTableNode, renderingSupport: RenderingSupport | undefined): JSX.Element {
        const flatMode = node.flatMode === true;
        const hasTarget = Array.isArray(node.flatToggleTarget) && node.flatToggleTarget.length > 0;
        const handler: ServerHandler | undefined = hasTarget ? {
            type: "SERVER",
            target: node.flatToggleTarget,
            contextId: ""
        } : undefined;

        const ariaLabel = flatMode ? "Switch to tree view" : "Switch to flat list";
        const iconClass = flatMode ? TreeTable.FLAT_TOGGLE_ICON_FLAT_MODE : TreeTable.FLAT_TOGGLE_ICON_TREE_MODE;

        return (
            <button
                type="button"
                className="tree-table-flat-toggle btn btn-link p-0"
                data-testid="tree-table-flat-toggle"
                aria-label={ariaLabel}
                disabled={!hasTarget}
                style={{
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                    width: "1.25rem",
                    height: "1.25rem",
                    fontSize: "0.9rem",
                    lineHeight: 1,
                    marginRight: "0.35rem"
                }}
                onClick={handler ? HandlerFactory.onClick(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
            >
                <i className={`bi ${iconClass}`} aria-hidden="true"></i>
            </button>
        );
    }

    /**
     * Renders the expand/collapse caret. For an expandable row, row.target IS the caret's click Target
     * (plan-76 Slice 5, no separate expandTarget field -- the backend reuses `target`); it is wrapped
     * into a ServerHandler here (raw Target, not a full Handler) before going through the same
     * HandlerFactory.onClick path every other per-node click uses -- the exact pattern already applied
     * to the root/child load-more controls in Slice 4. Per react4j-optional-per-item-handler-tsx-guard,
     * never dispatch a null/empty target: HandlerFactory.handleEvent derefs handler.type and throws on
     * a null handler, and the backend contract emits target=[] exactly when the caret is not clickable.
     * Caret direction is derived purely from row.expanded -- stateless (Cliff C5).
     */
    private renderCaret(row: TreeTableRow, renderingSupport: RenderingSupport | undefined): JSX.Element {
        const iconClass = row.expanded ? TreeTable.CARET_ICON_EXPANDED : TreeTable.CARET_ICON_COLLAPSED;
        const hasTarget = Array.isArray(row.target) && row.target.length > 0;
        const handler: ServerHandler | undefined = hasTarget ? {
            type: "SERVER",
            target: row.target,
            contextId: ""
        } : undefined;

        return (
            <button
                type="button"
                className="tree-table-caret btn btn-link p-0"
                data-testid="tree-table-caret"
                aria-label={row.expanded ? "Collapse" : "Expand"}
                disabled={!hasTarget}
                style={{
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                    width: "1.25rem",
                    height: "1.25rem",
                    fontSize: "0.75rem",
                    lineHeight: 1,
                    marginRight: "0.25rem"
                }}
                onClick={handler ? HandlerFactory.onClick(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
            >
                <i className={`bi ${iconClass}`} aria-hidden="true"></i>
            </button>
        );
    }

    private renderRow(row: TreeTableRow, columns: TreeTableColumn[], renderingSupport: RenderingSupport | undefined): JSX.Element {
        return (
            <tr key={row.nodeId} data-node-id={row.nodeId}>
                {columns.map((column, columnIndex) => {
                    const cellValue = row.cells ? row.cells[column.key] : undefined;
                    return columnIndex === 0 ? (
                        <td key={column.key} style={{ paddingLeft: `${row.depth * DEPTH_INDENT_EM}em` }}>
                            {row.expandable && this.renderCaret(row, renderingSupport)}
                            <span className="tree-table-cell-label">{cellValue}</span>
                        </td>
                    ) : (
                        <td key={column.key}>{cellValue}</td>
                    );
                })}
            </tr>
        );
    }

    /**
     * Shared load-more <tr> renderer, reused for both the root loadMore (Slice 4) and each expanded
     * row's childLoadMore (Slice 5) -- same shape, same dispatch mechanics, only the React key/testid
     * and the Target differ. Dispatches loadMore.target as a server event: loadMore carries a raw
     * Target, not a full Handler, so it is wrapped into a ServerHandler before going through the same
     * HandlerFactory.onClick path every other per-node click uses. Per
     * react4j-optional-per-item-handler-tsx-guard, never dispatch a null/empty target --
     * HandlerFactory.handleEvent derefs handler.type and throws on a null handler, and the backend
     * contract emits target=[] exactly when available=false.
     */
    private renderLoadMoreRow(reactKey: string, testId: string, columns: TreeTableColumn[], loadMore: TreeTableLoadMore | null | undefined, renderingSupport: RenderingSupport | undefined): JSX.Element | null {
        if (!loadMore?.available) {
            return null;
        }
        const hasTarget = Array.isArray(loadMore.target) && loadMore.target.length > 0;
        const handler: ServerHandler | undefined = hasTarget ? {
            type: "SERVER",
            target: loadMore.target,
            contextId: ""
        } : undefined;

        return (
            <tr key={reactKey}>
                <td colSpan={Math.max(columns.length, 1)}>
                    <button
                        type="button"
                        className="btn btn-link"
                        data-testid={testId}
                        disabled={!hasTarget}
                        onClick={handler ? HandlerFactory.onClick(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor) : undefined}
                    >
                        Load more
                    </button>
                </td>
            </tr>
        );
    }

    /**
     * Flattens rows[] into <tr> elements while inserting each expanded row's childLoadMore control at
     * the correct position: immediately after the run of rows belonging to it -- i.e. after the last
     * consecutive following row whose depth > thisRow.depth, before the next row at the same-or-
     * shallower depth (placement rule from the Slice-5 backend). Implemented with a depth-keyed stack
     * rather than recursion, so the flat <tbody> shape (Cliff C4) is preserved: rows[] is already a
     * preorder-DFS flattening, so a pending childLoadMore's descendant run ends exactly when a
     * subsequently-visited row's depth is <= the pending row's own depth. Nested pending entries are
     * flushed deepest-first (LIFO), which also correctly orders a child's own childLoadMore before its
     * ancestor's.
     */
    private renderRows(rows: TreeTableRow[], columns: TreeTableColumn[], renderingSupport: RenderingSupport | undefined): JSX.Element[] {
        const elements: JSX.Element[] = [];
        const pending: TreeTableRow[] = [];

        const flushDueBefore = (depth: number) => {
            while (pending.length > 0 && pending[pending.length - 1].depth >= depth) {
                const pendingRow = pending.pop() as TreeTableRow;
                const el = this.renderLoadMoreRow(
                    `child-load-more-${pendingRow.nodeId}`,
                    `tree-table-child-load-more-${pendingRow.nodeId}`,
                    columns,
                    pendingRow.childLoadMore,
                    renderingSupport
                );
                if (el) {
                    elements.push(el);
                }
            }
        };

        rows.forEach((row) => {
            flushDueBefore(row.depth);
            elements.push(this.renderRow(row, columns, renderingSupport));
            if (row.expanded && row.childLoadMore) {
                pending.push(row);
            }
        });
        flushDueBefore(-Infinity);

        return elements;
    }

    public render(): JSX.Element {
        const node = this.props.node;
        const columns = node.columns || [];
        const rows = node.rows || [];
        const renderingSupport = this.context as RenderingSupport | undefined;
        const filtersVisible = node.filtersVisible === true;

        return (
            <table className={"table" + (node.stickyHeader === true ? " tree-table-sticky-header" : "")} aria-label={node.ariaLabel || undefined}>
                <thead>
                    <tr>
                        {columns.map((column, columnIndex) => (
                            /* aria-sort is what makes a sorted column audible: without it a screen reader reads the
                               header text and the sort control's label, but never says the table is currently ordered
                               by this column. "none" rather than omitting it, so the header announces that it is
                               sortable-but-unsorted rather than not sortable at all. */
                            <th key={column.key} scope="col"
                                aria-sort={node.sortEnabled === true && column.sortable === true
                                    ? (column.sortDirection === "ASCENDING" ? "ascending"
                                        : column.sortDirection === "DESCENDING" ? "descending" : "none")
                                    : undefined}>
                                {columnIndex === 0 && node.filterEnabled === true && this.renderFilterToggle(node, renderingSupport)}
                                {columnIndex === 0 && node.flatModeToggleEnabled === true && this.renderFlatModeToggle(node, renderingSupport)}
                                <span className="tree-table-header-title">{column.title}</span>
                                {node.sortEnabled === true && column.sortable === true && this.renderSortToggle(column, renderingSupport)}
                                {node.sortEnabled === true && column.sortable === true && this.shouldRenderSortPrioritySelect(node, column) && this.renderSortPrioritySelect(node, column, renderingSupport)}
                            </th>
                        ))}
                    </tr>
                    {node.filterEnabled === true && filtersVisible && (
                        <tr className="tree-table-filter-row" data-testid="tree-table-filter-row">
                            {columns.map((column) => (
                                <td key={column.key}>{column.filterable === true ? this.renderFilterInput(column, renderingSupport) : null}</td>
                            ))}
                        </tr>
                    )}
                </thead>
                <tbody>
                    {this.renderRows(rows, columns, renderingSupport)}
                    {this.renderLoadMoreRow("root-load-more", "tree-table-load-more", columns, node.loadMore, renderingSupport)}
                </tbody>
            </table>
        );
    }
}
