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
 */
const FIT_RATIO = 1;
const RATIO_OPTIONS: { value: number; label: string }[] = [
    { value: 0.5, label: "50%" },
    { value: FIT_RATIO, label: "Fit (100%)" },
    { value: 1.5, label: "150%" },
    { value: 2, label: "200%" },
    { value: 4, label: "400%" }
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
}

interface ViewBox {
    x: number;
    y: number;
    width: number;
    height: number;
}

interface PanState {
    startClientX: number;
    startClientY: number;
    originX: number;
    originY: number;
}

const ZOOM_FACTOR = 1.2;

/**
 * Displays an SVG diagram, fitted into its container by default via pure CSS (viewBox +
 * preserveAspectRatio letterboxing, no measurement - see DiagramViewer.css), with optional zoom (in /
 * out / reset) and pan by rewriting the rendered `<svg>`'s `viewBox` around a base captured on mount.
 *
 * The root `<svg>`'s own `width`/`height` attributes are left untouched - CSS overrides them (see
 * DiagramViewer.css); stripping them while leaving `viewBox` alone renders at roughly 9x natural size,
 * a measured workspace finding.
 *
 * An SVG with no `viewBox` degrades to a static render: `hasViewBox` stays `false`, so the zoom/pan
 * controls are never shown and the pan handlers no-op - the diagram itself still renders normally.
 */
export class DiagramViewer extends React.Component<Props, State> {
    public static TYPE: string = "DIAGRAMVIEWER";

    public state: State = { hasViewBox: false, scale: FIT_RATIO };

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
        if (hasViewBox !== this.state.hasViewBox || this.state.scale !== FIT_RATIO) {
            this.setState({ hasViewBox, scale: FIT_RATIO });
        }
    }

    private currentViewBox(): ViewBox | null {
        const svg = this.getSvgElement();
        return svg ? this.parseViewBox(svg.getAttribute("viewBox")) : null;
    }

    private applyViewBox(viewBox: ViewBox): void {
        const svg = this.getSvgElement();
        if (!svg) {
            return;
        }
        svg.setAttribute("viewBox", `${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`);
    }

    private zoom(factor: number): void {
        if (!this.baseViewBox) {
            return;
        }
        const current = this.currentViewBox() || this.baseViewBox;
        const newWidth = current.width / factor;
        const newHeight = current.height / factor;
        // Zoom around the CURRENT viewBox's center, so repeated zoom-in/out keeps whatever the user has
        // already panned to centered, rather than recentering on the base every time.
        const centerX = current.x + current.width / 2;
        const centerY = current.y + current.height / 2;
        this.applyViewBox({
            x: centerX - newWidth / 2,
            y: centerY - newHeight / 2,
            width: newWidth,
            height: newHeight
        });
        // Keep the explicit scale state in sync (plan-261 Cliff C4). +/- multiply by ZOOM_FACTOR, so the
        // result generally lands BETWEEN the fixed ratios - the ratio control shows a neutral "Custom"
        // marker in that case rather than snapping to a misleading nearest label (see CUSTOM_RATIO_VALUE).
        this.setState({ scale: this.baseViewBox.width / newWidth });
    }

    private zoomIn = (): void => {
        this.zoom(ZOOM_FACTOR);
    };

    private zoomOut = (): void => {
        this.zoom(1 / ZOOM_FACTOR);
    };

    /**
     * Applies one of the fixed RATIO_OPTIONS ratios (everything except Fit/100%, which is wired straight
     * to resetZoom below and never recomputed - plan-261 Cliff C4). Jumps to an absolute view at the
     * requested ratio, centered on the BASE viewBox's center - a deterministic preset, not a relative
     * adjustment of wherever the user last panned to.
     */
    private applyRatio(ratio: number): void {
        if (!this.baseViewBox) {
            return;
        }
        const base = this.baseViewBox;
        const newWidth = base.width / ratio;
        const newHeight = base.height / ratio;
        const centerX = base.x + base.width / 2;
        const centerY = base.y + base.height / 2;
        this.applyViewBox({
            x: centerX - newWidth / 2,
            y: centerY - newHeight / 2,
            width: newWidth,
            height: newHeight
        });
        this.setState({ scale: ratio });
    }

    private selectRatio = (event: React.ChangeEvent<HTMLSelectElement>): void => {
        const value = event.target.value;
        if (value === CUSTOM_RATIO_VALUE) {
            return;
        }
        const ratio = Number(value);
        if (ratio === FIT_RATIO) {
            // Fit and 100% are one entry, wired to the existing resetZoom, which restores baseViewBox
            // EXACTLY - not recomputed via applyRatio(1), which would (redundantly, but not identically
            // in a floating-point sense) reconstruct the same box from its own center/width/height parts.
            this.resetZoom();
            return;
        }
        this.applyRatio(ratio);
    };

    private resetZoom = (): void => {
        if (this.baseViewBox) {
            this.applyViewBox(this.baseViewBox);
            this.setState({ scale: FIT_RATIO });
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

    private handleMouseDown = (event: React.MouseEvent<HTMLDivElement>): void => {
        if (!this.props.node.interactive || !this.baseViewBox) {
            return;
        }
        const current = this.currentViewBox() || this.baseViewBox;
        this.panState = {
            startClientX: event.clientX,
            startClientY: event.clientY,
            originX: current.x,
            originY: current.y
        };
        this.attachWindowDragListeners();
    };

    private handleWindowMouseMove = (event: MouseEvent): void => {
        if (!this.panState || !this.baseViewBox) {
            return;
        }
        const current = this.currentViewBox() || this.baseViewBox;
        const host = this.svgHostRef.current;
        if (!host) {
            return;
        }
        // Convert a screen-pixel drag distance into viewBox units via the host's rendered size, so pan
        // speed matches whatever the current zoom level is, not the base one. Falls back to a 1:1 scale
        // when the host has no measured size yet (e.g. not yet laid out).
        const rect = host.getBoundingClientRect();
        const scaleX = rect.width > 0 ? current.width / rect.width : 1;
        const scaleY = rect.height > 0 ? current.height / rect.height : 1;
        const deltaX = (event.clientX - this.panState.startClientX) * scaleX;
        const deltaY = (event.clientY - this.panState.startClientY) * scaleY;
        this.applyViewBox({
            x: this.panState.originX - deltaX,
            y: this.panState.originY - deltaY,
            width: current.width,
            height: current.height
        });
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
        const selectedRatioOption = RATIO_OPTIONS.find((option) => option.value === this.state.scale);
        const ratioSelectValue = selectedRatioOption ? String(selectedRatioOption.value) : CUSTOM_RATIO_VALUE;

        return (
            <div className="diagram-viewer" style={style}>
                <div
                    ref={this.svgHostRef}
                    className="diagram-viewer-svg-host"
                    onMouseDown={node.interactive ? this.handleMouseDown : undefined}
                    // eslint-disable-next-line react/no-danger -- server-rendered SVG markup; see class doc
                    dangerouslySetInnerHTML={{ __html: node.svg || "" }}
                />
                {showControls && (
                    <div className="diagram-viewer-controls">
                        <select
                            className="form-select form-select-sm"
                            aria-label="Zoom ratio"
                            value={ratioSelectValue}
                            onChange={this.selectRatio}
                        >
                            {!selectedRatioOption && <option value={CUSTOM_RATIO_VALUE}>Custom</option>}
                            {RATIO_OPTIONS.map((option) => (
                                <option key={option.value} value={option.value}>{option.label}</option>
                            ))}
                        </select>
                        <button type="button" aria-label="Zoom in" onClick={this.zoomIn}>+</button>
                        <button type="button" aria-label="Zoom out" onClick={this.zoomOut}>-</button>
                        <button type="button" aria-label="Reset zoom" onClick={this.resetZoom}>Reset</button>
                    </div>
                )}
            </div>
        );
    }
}
