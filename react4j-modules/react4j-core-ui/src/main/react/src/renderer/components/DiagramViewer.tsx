import React from "react";
import { Node } from "../Renderer";
// DiagramViewer's own fit-to-container mechanics (viewBox/preserveAspectRatio letterboxing). Bundled
// with the framework rather than shipped in /css/custom.css - see ScrollbarContainer.css for the
// general convention this follows.
import "./DiagramViewer.css";

export interface DiagramViewerNode extends Node {
    svg: string;
    maxHeight: string;
    width: string;
    height?: string;
    interactive: boolean;
}

export interface Props {
    node: DiagramViewerNode;
}

/**
 * A fixed zoom ratio offered by the ratio control, expressed as a multiple of the Fit (base) viewBox.
 * "Fit" and "100%" are ONE entry (plan-261 Cliff C4) - the user-facing ratio list names five things, not
 * six, and Fit sits in its natural ascending position between 50% and 150%.
 *
 * plan-266 Cliff N4 added a SIXTH entry, "Whole diagram", between 50% and Fit. It carries the SAME numeric
 * scale as Fit (both are 1) but a DIFFERENT fit mode - `containMode: true` selects the incumbent
 * fill-and-letterbox (contain) rule instead of the new cover rule, so numeric scale alone can no longer
 * identify the selected option (see `selectedRatioOption` in render()). `key` is the <option value> the
 * <select> actually carries and is therefore what selectRatio dispatches on; `scale` is the numeric value
 * applied to state.
 */
const FIT_RATIO = 1;
interface RatioOption {
    key: string;
    scale: number;
    containMode: boolean;
    label: string;
}
const RATIO_OPTIONS: RatioOption[] = [
    { key: "0.5", scale: 0.5, containMode: false, label: "50%" },
    { key: "whole", scale: FIT_RATIO, containMode: true, label: "Whole diagram" },
    { key: "1", scale: FIT_RATIO, containMode: false, label: "Fit (100%)" },
    { key: "1.5", scale: 1.5, containMode: false, label: "150%" },
    { key: "2", scale: 2, containMode: false, label: "200%" },
    { key: "4", scale: 4, containMode: false, label: "400%" }
];

/**
 * The marker shown by the ratio control when the current scale (kept in sync by zoomIn/zoomOut, which
 * multiply by ZOOM_FACTOR and so land BETWEEN the fixed ratios) does not exactly match any RATIO_OPTIONS
 * entry. A neutral custom marker was chosen over a "nearest label" heuristic: snapping +/-20%-stepped
 * zoom to the nearest fixed label would silently misreport the actual scale (e.g. two zoom-ins from Fit
 * land at 1.44, closer to neither 100% nor 150% than the other in a way a user could rely on), whereas a
 * dedicated "Custom" option is always honest about the fact that the current view is not one of the fixed
 * ratios.
 */
const CUSTOM_RATIO_VALUE = "custom";

interface State {
    /** Whether the injected root <svg> carried a parseable viewBox - gates rendering the zoom/pan controls. */
    hasViewBox: boolean;
    /**
     * The current zoom scale as a multiple of the Fit (base) viewBox, kept as an explicit named source of
     * truth rather than derived by re-reading the rendered viewBox (plan-261 Cliff C4 / design P15-P11).
     * zoomIn/zoomOut/resetZoom/selectRatio all keep this in sync with the DOM mutation they perform.
     */
    scale: number;
    /**
     * plan-266 Cliff N4. `false` (the default, "cover"): the smallest axis is fit to the host and the larger
     * axis overflows into the scroll region (N4-C / "V8"). `true` ("Whole diagram" selected): the incumbent
     * fill-and-letterbox (contain) rule, byte-identically what Fit always rendered (plan-266 section 2.9a) -
     * also the rule a non-interactive thumbnail always uses, by construction rather than by a second
     * mechanism. Every action OTHER than selecting "Whole diagram" itself - zoomIn/zoomOut/a fixed ratio/
     * reset - returns this to `false`, because cover is the only mode this component scrolls in.
     */
    containMode: boolean;
}

interface ViewBox {
    x: number;
    y: number;
    width: number;
    height: number;
}

/**
 * A drag in progress, captured at mousedown: the pointer's start position plus the scroll offsets the
 * host held at that moment. Drag-pan writes `scrollLeft`/`scrollTop` (plan-265) - it no longer rewrites
 * the `viewBox`, so it needs no viewBox units and no host measurement to convert into them.
 */
interface PanState {
    startClientX: number;
    startClientY: number;
    originScrollLeft: number;
    originScrollTop: number;
}

/** The host's scroll offsets and content-box size, sampled BEFORE a scale change (see adjustScrollToPreserveCentre). */
interface ScrollViewport {
    scrollLeft: number;
    scrollTop: number;
    clientWidth: number;
    clientHeight: number;
}

const ZOOM_FACTOR = 1.2;

/**
 * The CSS custom property through which the current zoom scale reaches layout - the single seam between
 * this component's React state and DiagramViewer.css, one-way and private to the pair. The stylesheet
 * sizes the injected `<svg>` as `calc(100% * var(--diagram-viewer-scale, 1))` on both axes.
 *
 * The property is deliberately OMITTED rather than emitted as `1` at Fit, and omitted entirely for a
 * non-interactive viewer: the stylesheet's `var(..., 1)` fallback then makes "Fit renders exactly as it
 * always did, with no scroll region" structural rather than conditional - there is no value to get wrong
 * and no branch to forget. See DiagramViewer.css for the full mechanism and its measurements.
 */
const SCALE_CSS_PROPERTY = "--diagram-viewer-scale";

/**
 * plan-266 Cliff N4. The CSS custom property carrying the diagram's own ratio (parsed from the already-read
 * `baseViewBox`, never measured) into DiagramViewer.css's cover formula - `aspect-ratio: var(--diagram-viewer-
 * aspect)` on `.diagram-viewer-canvas`. This is authored data, not rendered geometry: `viewBox` is an SVG
 * attribute string, read once on mount exactly as `baseViewBox` itself already is, before any layout the
 * property could race with. Set whenever `baseViewBox` is known, regardless of interactive/cover/contain -
 * harmless where the cover rule that reads it is not selected (thumbnails, "Whole diagram").
 */
const ASPECT_CSS_PROPERTY = "--diagram-viewer-aspect";

/**
 * Displays an SVG diagram, fitted into its container by default via pure CSS (viewBox +
 * preserveAspectRatio letterboxing, no measurement - see DiagramViewer.css), with optional zoom (in /
 * out / reset) and two-axis scrolling.
 *
 * Zoom changes the injected `<svg>`'s RENDERED BOX SIZE, never its `viewBox`: the scale is published to
 * DiagramViewer.css through the {@link SCALE_CSS_PROPERTY} custom property, the stylesheet sizes the svg
 * at `calc(100% * scale)` on both axes, and the host is `overflow: auto`. So zooming in makes the element
 * genuinely larger than its scroll container and the browser's own scrollbars, wheel, shift+wheel, arrow
 * keys and (retargeted) drag-pan all traverse it on both axes - which is what lets a user follow a long
 * edge across a zoomed diagram without losing their place. **The `viewBox` is read once on mount and
 * never written after**, and that invariant is load-bearing: element-sizing zoom and viewBox zoom are
 * mutually exclusive mechanisms, and running both would fight. It is pinned by a test
 * (DiagramViewer.test.tsx) and by plan-265 AC-4.
 *
 * Changing the scale preserves the point the viewport was centred on, by adjusting the scroll offsets in
 * proportion to the scale change (see adjustScrollToPreserveCentre) - so zooming does not throw away
 * where the user had scrolled to. Returning to Fit needs no explicit scroll reset: at scale 1 the svg's
 * box equals the host's content box, the scrollable overflow disappears, and the browser clamps both
 * offsets back to 0 on its own.
 *
 * The root `<svg>`'s own `width`/`height` attributes are left untouched - CSS overrides them (see
 * DiagramViewer.css); stripping them while leaving `viewBox` alone renders at roughly 9x natural size,
 * a measured workspace finding.
 *
 * The zoom controls are a BAND ABOVE THE SCROLL REGION, not an overlay on it (plan-266 Cliff N2-B,
 * answering the ruling "the dropdown and the zoom tooling should stay on top"). They are rendered as a
 * normal-flow sibling emitted BEFORE the host, and `.diagram-viewer` is a column flex container - so the
 * band cannot overlap the diagram and cannot scroll away with it, structurally rather than by a z-index
 * or an inset somebody has to keep correct. They were absolutely positioned at the viewer's top-left
 * until plan-266; see DiagramViewer.css for what that arrangement cost and what replaced it.
 *
 * An SVG with no `viewBox` degrades to a static render: `hasViewBox` stays `false`, so the zoom/pan
 * controls are never shown and the scale never leaves 1 - the diagram itself still renders normally, and
 * with no overflow there is nothing for a drag or a key press to move. The same `showControls` flag gates
 * the band, so a non-interactive thumbnail emits no band element and reserves no height for one.
 *
 * AUTO-FIT IS COVER, NOT CONTAIN (plan-266 Cliff N4). The default fit - and every fixed ratio except "Whole
 * diagram" - fits the SMALLER axis of the diagram to the host and lets the LARGER axis overflow into the
 * scroll region, rather than letterboxing the whole diagram into the box. This is a pure-CSS mechanism (see
 * DiagramViewer.css, ".diagram-viewer-canvas"): a wrapper div carries `aspect-ratio` (from `baseViewBox`,
 * published via {@link ASPECT_CSS_PROPERTY}) plus a `min-width`/`min-height` pair scaled by
 * {@link SCALE_CSS_PROPERTY}, and the injected `<svg>` is `position: absolute` filling it - out of flow, so
 * the wrapper's own content size is 0x0 and both minima are always violated, which is the one CSS-minimum-
 * resolution branch that produces cover rather than contain. No JS measurement of rendered geometry is
 * involved anywhere in this mechanism, matching the user's own constraint. "Whole diagram" (state.containMode)
 * opts back into the incumbent contain rule - byte-identically what Fit always rendered - and is also what a
 * non-interactive thumbnail always uses, since `showCover` below is gated on `interactive`.
 */
export class DiagramViewer extends React.Component<Props, State> {
    public static TYPE: string = "DIAGRAMVIEWER";

    public state: State = { hasViewBox: false, scale: FIT_RATIO, containMode: false };

    private svgHostRef = React.createRef<HTMLDivElement>();
    private baseViewBox: ViewBox | null = null;
    private panState: PanState | null = null;

    public componentDidMount(): void {
        this.initializeSvg();
    }

    public componentDidUpdate(prevProps: Props): void {
        if (prevProps.node.svg !== this.props.node.svg) {
            this.baseViewBox = null;
            this.initializeSvg();
        }
    }

    public componentWillUnmount(): void {
        this.detachWindowDragListeners();
    }

    private getSvgElement(): SVGSVGElement | null {
        const host = this.svgHostRef.current;
        return host ? host.querySelector("svg") : null;
    }

    private parseViewBox(value: string | null): ViewBox | null {
        if (!value) {
            return null;
        }
        const parts = value.trim()
                            .split(/\s+/)
                            .map(Number);
        if (parts.length !== 4 || parts.some((n) => Number.isNaN(n))) {
            return null;
        }
        return { x: parts[0], y: parts[1], width: parts[2], height: parts[3] };
    }

    private initializeSvg(): void {
        const svg = this.getSvgElement();
        if (!svg) {
            return;
        }
        // The default SVG value already means "letterbox, don't distort" - set explicitly so a source
        // SVG that set a different (e.g. stretching) value never defeats the fit-to-container contract.
        svg.setAttribute("preserveAspectRatio", "xMidYMid meet");

        this.baseViewBox = this.parseViewBox(svg.getAttribute("viewBox"));
        const hasViewBox = this.baseViewBox !== null;
        if (hasViewBox !== this.state.hasViewBox || this.state.scale !== FIT_RATIO || this.state.containMode) {
            this.setState({ hasViewBox, scale: FIT_RATIO, containMode: false });
        }
    }

    /**
     * The single place the scale is changed. Publishes the new scale to state (and from there, via render,
     * to {@link SCALE_CSS_PROPERTY}), then re-centres the viewport on whatever it was centred on before.
     *
     * The viewport sample has to be taken HERE, before `setState`, because by the time the adjustment runs
     * the layout has already changed and the old offsets are gone. The adjustment itself runs in the
     * `setState` completion callback, which React invokes after the DOM has been updated - so reading
     * `scrollWidth`/`clientWidth` there reflects the new box (the read forces the pending layout), while
     * `componentDidUpdate` would need a prevState comparison to tell a scale change from any other
     * re-render.
     */
    private applyScale(scale: number, containMode: boolean = false): void {
        const host = this.svgHostRef.current;
        const before: ScrollViewport | null = host
            ? { scrollLeft: host.scrollLeft, scrollTop: host.scrollTop, clientWidth: host.clientWidth, clientHeight: host.clientHeight }
            : null;
        const previousScale = this.state.scale;
        this.setState({ scale, containMode }, () => this.adjustScrollToPreserveCentre(before, previousScale, scale));
    }

    /**
     * Keeps the point the viewport was centred on centred across a scale change. A content offset `p` in
     * the old layout lands at `p * (scale / previousScale)` in the new one, and the old viewport centre sat
     * at `scrollLeft + clientWidth / 2` - so the new offset is that, rescaled, minus half the viewport.
     * Clamped into the host's actual scrollable range, which also covers the Fit case: at scale 1 there is
     * no scrollable range at all, so both offsets clamp to 0 and returning to Fit needs no special case.
     */
    private adjustScrollToPreserveCentre(before: ScrollViewport | null, previousScale: number, scale: number): void {
        const host = this.svgHostRef.current;
        if (!host || !before || previousScale <= 0) {
            return;
        }
        const factor = scale / previousScale;
        const targetLeft = (before.scrollLeft + before.clientWidth / 2) * factor - host.clientWidth / 2;
        const targetTop = (before.scrollTop + before.clientHeight / 2) * factor - host.clientHeight / 2;
        host.scrollLeft = Math.max(0, Math.min(targetLeft, host.scrollWidth - host.clientWidth));
        host.scrollTop = Math.max(0, Math.min(targetTop, host.scrollHeight - host.clientHeight));
    }

    private zoom(factor: number): void {
        if (!this.baseViewBox) {
            return;
        }
        // Relative to the CURRENT scale, so repeated zoom-in/out compounds from where the user is, exactly
        // as the previous viewBox-rewriting implementation did. +/- multiply by ZOOM_FACTOR, so the result
        // generally lands BETWEEN the fixed ratios - the ratio control shows a neutral "Custom" marker in
        // that case rather than snapping to a misleading nearest label (see CUSTOM_RATIO_VALUE).
        this.applyScale(this.state.scale * factor);
    }

    private zoomIn = (): void => {
        this.zoom(ZOOM_FACTOR);
    };

    private zoomOut = (): void => {
        this.zoom(1 / ZOOM_FACTOR);
    };

    /**
     * Applies one of the fixed RATIO_OPTIONS ratios (everything except Fit/100%, which is wired straight
     * to resetZoom below - plan-261 Cliff C4). Jumps to an absolute scale, keeping the currently centred
     * point centred like every other scale change.
     */
    private applyRatio(ratio: number): void {
        if (!this.baseViewBox) {
            return;
        }
        this.applyScale(ratio);
    }

    /**
     * Dispatches on the RATIO_OPTIONS `key` the <select> actually carries (plan-266 Cliff N4) - NOT on the
     * numeric scale, because "Whole diagram" and "Fit" share the same scale (1) and are distinguished only by
     * `containMode`.
     */
    private selectRatio = (event: React.ChangeEvent<HTMLSelectElement>): void => {
        const key = event.target.value;
        if (key === CUSTOM_RATIO_VALUE) {
            return;
        }
        const option = RATIO_OPTIONS.find((candidate) => candidate.key === key);
        if (!option) {
            return;
        }
        if (option.containMode) {
            this.applyScale(option.scale, true);
            return;
        }
        if (option.scale === FIT_RATIO) {
            // Fit and 100% are one entry, wired to the existing resetZoom (plan-261 Cliff C4).
            this.resetZoom();
            return;
        }
        this.applyRatio(option.scale);
    };

    /**
     * Returns to Fit. The scroll position needs no explicit reset and deliberately does not get one: at
     * scale 1 the svg's box equals the host's content box, so the scrollable overflow disappears and the
     * browser clamps both offsets to 0 by itself (the clamp in adjustScrollToPreserveCentre lands on the
     * same answer). That is why this button's label stays "Reset zoom" and not "Reset view" - resetting
     * the zoom is the whole action; losing the scroll offset is a consequence of there being nowhere left
     * to scroll, not a second thing the button does.
     */
    private resetZoom = (): void => {
        if (this.baseViewBox) {
            this.applyScale(FIT_RATIO);
        }
    };

    private attachWindowDragListeners(): void {
        window.addEventListener("mousemove", this.handleWindowMouseMove);
        window.addEventListener("mouseup", this.handleWindowMouseUp);
    }

    private detachWindowDragListeners(): void {
        window.removeEventListener("mousemove", this.handleWindowMouseMove);
        window.removeEventListener("mouseup", this.handleWindowMouseUp);
    }

    /**
     * Drag-pan, RETARGETED from the `viewBox` to the host's own scroll offsets (plan-265 constraint 3 -
     * kept, not deleted). Dragging still moves the diagram; only the mechanism underneath changed, and it
     * got simpler: screen pixels are now scroll pixels, so there is no conversion into viewBox units and no
     * host measurement to do it with.
     *
     * `preventDefault` suppresses the browser's own text/image drag, which would otherwise hijack the
     * gesture. Because that also suppresses the click's implicit focus, focus is then moved explicitly -
     * losing it would make the scroll host silently un-keyboard-drivable straight after a drag, which is
     * exactly the interaction a user tracing a line reaches for next.
     */
    private handleMouseDown = (event: React.MouseEvent<HTMLDivElement>): void => {
        const host = this.svgHostRef.current;
        if (!this.props.node.interactive || !host) {
            return;
        }
        event.preventDefault();
        host.focus();
        this.panState = {
            startClientX: event.clientX,
            startClientY: event.clientY,
            originScrollLeft: host.scrollLeft,
            originScrollTop: host.scrollTop
        };
        this.attachWindowDragListeners();
    };

    private handleWindowMouseMove = (event: MouseEvent): void => {
        const host = this.svgHostRef.current;
        if (!this.panState || !host) {
            return;
        }
        // Content follows the pointer: dragging LEFT moves the diagram left, i.e. reveals content further
        // to the right, i.e. INCREASES scrollLeft - hence "origin minus delta". The browser clamps both
        // assignments into the host's scrollable range, so no clamping is needed here.
        host.scrollLeft = this.panState.originScrollLeft - (event.clientX - this.panState.startClientX);
        host.scrollTop = this.panState.originScrollTop - (event.clientY - this.panState.startClientY);
    };

    private handleWindowMouseUp = (): void => {
        this.panState = null;
        this.detachWindowDragListeners();
    };

    public render(): JSX.Element {
        const node = this.props.node;
        const style: React.CSSProperties = {
            width: node.width,
            maxHeight: node.maxHeight,
            // Omit the key entirely when unset - a plain `height: node.height` would still set the key
            // (to `undefined`), and criterion C5(2) requires a consumer that never calls withHeight to see
            // byte-for-byte unchanged markup.
            ...(node.height ? { height: node.height } : {})
        };
        const showControls = node.interactive && this.state.hasViewBox;
        // Matched on BOTH scale and containMode (plan-266 Cliff N4) - scale alone is ambiguous between "Whole
        // diagram" and "Fit", which share the value 1.
        const selectedRatioOption = RATIO_OPTIONS.find(
            (option) => option.scale === this.state.scale && option.containMode === this.state.containMode
        );
        const ratioSelectValue = selectedRatioOption ? selectedRatioOption.key : CUSTOM_RATIO_VALUE;
        // Emitted ONLY away from Fit, and only for an interactive viewer: the stylesheet's var(..., 1)
        // fallback then renders a thumbnail and an unzoomed viewer at exactly 100% with no scroll region,
        // by construction rather than by a value this branch has to get right (see SCALE_CSS_PROPERTY).
        const hostStyle: React.CSSProperties | undefined =
            node.interactive && this.state.scale !== FIT_RATIO
                ? ({ [SCALE_CSS_PROPERTY]: String(this.state.scale) } as React.CSSProperties)
                : undefined;
        // plan-266 Cliff N4 - cover is selected only for an interactive viewer with a parseable viewBox that
        // has NOT chosen "Whole diagram". Thumbnails (interactive === false) never carry this class, so their
        // rendering is untouched by cover's existence, by construction (section 2.9a).
        const coverMode = node.interactive && this.state.hasViewBox && !this.state.containMode;
        const hostClassName = "diagram-viewer-svg-host" + (coverMode ? " diagram-viewer-svg-host--cover" : "");
        // The ratio the cover formula scales - authored data (the already-parsed base viewBox), never
        // rendered geometry. Set whenever known; inert under the contain rule, which does not read it.
        const canvasStyle: React.CSSProperties | undefined = this.baseViewBox
            ? ({ [ASPECT_CSS_PROPERTY]: `${this.baseViewBox.width} / ${this.baseViewBox.height}` } as React.CSSProperties)
            : undefined;

        return (
            <div className="diagram-viewer" style={style}>
                {/*
                  * The control band, emitted BEFORE the scroll host and as a normal-flow sibling of it, so
                  * DiagramViewer.css's column flex chain lays it out as a row above the diagram rather than
                  * on top of it (plan-266 Cliff N2-B). DOM order IS the visual order here - there is no
                  * "order" property and no absolute positioning left to override it - so this block must
                  * stay first. It renders only when `showControls` holds, which is what keeps a
                  * non-interactive thumbnail free of any band and of the height one would reserve.
                  */}
                {showControls && (
                    <div className="diagram-viewer-controls" role="group" aria-label="Diagram zoom controls">
                        <select
                            className="form-select form-select-sm"
                            aria-label="Zoom ratio"
                            value={ratioSelectValue}
                            onChange={this.selectRatio}
                        >
                            {!selectedRatioOption && <option value={CUSTOM_RATIO_VALUE}>Custom</option>}
                            {RATIO_OPTIONS.map((option) => (
                                <option key={option.key} value={option.key}>{option.label}</option>
                            ))}
                        </select>
                        <button type="button" aria-label="Zoom in" onClick={this.zoomIn}>+</button>
                        <button type="button" aria-label="Zoom out" onClick={this.zoomOut}>-</button>
                        <button type="button" aria-label="Reset zoom" onClick={this.resetZoom}>Reset</button>
                    </div>
                )}
                {/*
                  * The viewport: a ROW flex container wrapping the scroll host, and it exists for exactly one
                  * measured reason (plan-266 S1, hypothesis H-COL falsified - see DiagramViewer.css). The svg
                  * is sized "calc(100% * scale)", and a percentage height only resolves against a containing
                  * block whose height is DEFINITE. Until plan-266 the host got that definiteness from being
                  * cross-axis stretched by .diagram-viewer's row layout. Making .diagram-viewer a COLUMN
                  * container to stack the band moved height onto the MAIN axis, where it is not definite, and
                  * the svg silently fell back to its intrinsic aspect-ratio height (measured 11745px in a
                  * 560px host). This element restores the row axis the host needs, one level in.
                  */}
                <div className="diagram-viewer-viewport">
                    <div
                        ref={this.svgHostRef}
                        className={hostClassName}
                        style={hostStyle}
                        // The scroll host must be genuinely drivable, not merely scrollable: a tabIndex is what
                        // makes the browser's own arrow-key scrolling reach it, and the labelled region is what
                        // names it for a screen reader. Non-interactive thumbnails get none of this - they have
                        // no overflow to traverse and must stay byte-identically unchanged.
                        tabIndex={node.interactive ? 0 : undefined}
                        role={node.interactive ? "region" : undefined}
                        aria-label={node.interactive ? "Scrollable diagram" : undefined}
                        onMouseDown={node.interactive ? this.handleMouseDown : undefined}
                    >
                        {/*
                          * plan-266 Cliff N4 (N4-C / "V8"). The injected svg moved one level in, from the host
                          * directly to this wrapper - both the cover formula (aspect-ratio + minima, applied
                          * when the host carries "--cover") and the incumbent contain formula (width/height:
                          * calc(100% * scale), the default) size THIS element; the svg itself is always
                          * "position: absolute" filling it. See DiagramViewer.css for the full mechanism.
                          */}
                        <div
                            className="diagram-viewer-canvas"
                            style={canvasStyle}
                            // eslint-disable-next-line react/no-danger -- server-rendered SVG markup; see class doc
                            dangerouslySetInnerHTML={{ __html: node.svg || "" }}
                        />
                    </div>
                </div>
            </div>
        );
    }
}
