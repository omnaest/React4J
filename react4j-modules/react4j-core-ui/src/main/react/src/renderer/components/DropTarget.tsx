import React from "react";
import { Node, Renderer, RenderingSupport, Target } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { HandlerFactory, ServerHandler } from "../handler/Handler";
import { UIContext, UIContextAccessor } from "../data/DataContextManager";
import { KeyboardDragState } from "./KeyboardDragState";
import "./DropTarget.css";

export type DropRelation = "BEFORE" | "AFTER" | "INTO";

/**
 * Frozen node-JSON contract emitted by DropTargetImpl (plan-235 S1, Cliff 1'): `dragIdFieldKey` /
 * `relationFieldKey` are the submitted-Data field keys this component must write the drop's dragId and
 * relation into BEFORE dispatching `dropTarget` - both `null` when no `onDrop` handler was registered
 * server-side, mirroring `dropTarget` being emitted as an empty ([]) Target in that case.
 */
export interface DropTargetNode extends Node {
    content: Node;
    dragIdFieldKey: string | null;
    relationFieldKey: string | null;
    dropTarget: Target;
}

export interface Props {
    node: DropTargetNode;
}

/**
 * Real DROPTARGET renderer (plan-235 S1). Copies TreeTable.tsx's write-then-dispatch mechanism verbatim
 * (see `writeDataField`/`commitFilter` there) rather than `DataContextManager.updateFieldByContext`,
 * because the root context id is the empty string, which is falsy in JS and would silently no-op that
 * wrapper's `if (contextId)` guard (TreeTable.tsx documents this verbatim; plan-235 S9.3 carries the same
 * trap into this brief).
 */
export class DropTarget extends React.Component<Props, {}> {
    public static TYPE: string = "DROPTARGET";
    public static contextType = RenderingSupportContext;

    private rootRef = React.createRef<HTMLDivElement>();

    /**
     * Same accessor-level primitive TreeTable.writeDataField uses - see that method's own comment for why
     * `DataContextManager.updateFieldByContext` must NOT be used for the root ("") context.
     */
    private writeDataField(fieldKey: string, value: string, uiContextAccessor: UIContextAccessor | undefined): void {
        const uiContext: UIContext | undefined = uiContextAccessor?.getUIContextById("");
        if (uiContext && uiContext.data[fieldKey] !== value) {
            uiContext.data[fieldKey] = value;
            uiContext.updateCounter++;
            uiContextAccessor?.updateUIContext(uiContext);
        }
    }

    /**
     * Writes BOTH keys, THEN dispatches (plan-235 S9.3, mirrors TreeTable.commitFilter's write-then-dispatch
     * ordering exactly). Guarded (react4j-optional-per-item-handler-tsx-guard): a DropTarget with no
     * server-side onDrop emits null field keys and an empty dropTarget, and this never writes nor
     * dispatches in that case - HandlerFactory.handleEvent dereferences handler.type and throws on a null
     * handler.
     * <p>
     * Returns whether it genuinely consumed the event (plan-235 S1 corrective round 2, Defect B): callers
     * use this to decide whether to {@code stopPropagation()} - only a DropTarget that actually dispatched
     * may swallow the event on behalf of an ancestor DropTarget that would otherwise have handled it.
     */
    private commitDrop(dragId: string, relation: DropRelation, renderingSupport: RenderingSupport | undefined): boolean {
        const node = this.props.node;
        const hasTarget = Array.isArray(node.dropTarget) && node.dropTarget.length > 0;
        if (!node.dragIdFieldKey || !node.relationFieldKey || !hasTarget) {
            return false;
        }
        this.writeDataField(node.dragIdFieldKey, dragId, renderingSupport?.uiContextAccessor);
        this.writeDataField(node.relationFieldKey, relation, renderingSupport?.uiContextAccessor);
        const handler: ServerHandler = { type: "SERVER", target: node.dropTarget, contextId: "" };
        HandlerFactory.handleEvent(handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor);
        return true;
    }

    /**
     * The browser's own drop-eligibility rule (plan-235 S9.5c): without calling preventDefault() here, NO
     * drop ever fires - a disposable Playwright probe proved this with a negative control (twelve
     * `dragover` events, zero drops, when this was omitted). This is not style, it is load-bearing.
     */
    private handleDragOver = (event: React.DragEvent<HTMLDivElement>): void => {
        event.preventDefault();
    };

    /**
     * Where within this target's OWN rendered bounds the pointer was released: top third -> BEFORE,
     * bottom third -> AFTER, middle -> INTO. Falls back to INTO when the root element cannot be measured
     * (e.g. not yet mounted).
     */
    private relationForPointer(event: React.DragEvent<HTMLDivElement>): DropRelation {
        const element = this.rootRef.current;
        if (!element) {
            return "INTO";
        }
        const rect = element.getBoundingClientRect();
        const offset = event.clientY - rect.top;
        const third = rect.height / 3;
        if (third > 0 && offset < third) {
            return "BEFORE";
        } else if (third > 0 && offset > third * 2) {
            return "AFTER";
        } else {
            return "INTO";
        }
    }

    /**
     * plan-235 S1 corrective round 2 (Defect B). `stopPropagation()` fires ONLY when {@link #commitDrop}
     * genuinely consumed the event (returned true) - a DropTarget with no registered handler must not
     * swallow a drop an ancestor DropTarget would otherwise have handled. Without this, nesting a
     * list-level DropTarget around item-level ones makes one physical drop fire BOTH handlers - two
     * independent `/ui/event` round trips for one gesture, the outer one (its relation computed from the
     * whole list's bounds) able to silently override the inner one's correct placement.
     */
    private handleDrop = (event: React.DragEvent<HTMLDivElement>): void => {
        event.preventDefault();
        const dragId = event.dataTransfer.getData("text/plain");
        if (!dragId) {
            return;
        }
        const relation = this.relationForPointer(event);
        const renderingSupport = this.context as RenderingSupport | undefined;
        const consumed = this.commitDrop(dragId, relation, renderingSupport);
        if (consumed) {
            event.stopPropagation();
        }
    };

    /**
     * Keyboard drop half of the ONE keyboard drag-and-drop path (AC-S1-7): Space/Enter while this target
     * is focused drops whatever `Draggable` last "picked up" via {@link KeyboardDragState}, always as
     * relation `INTO` - a keyboard activation carries no pointer position to derive BEFORE/AFTER from.
     */
    private handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>): void => {
        if (event.key === " " || event.key === "Enter") {
            const dragId = KeyboardDragState.getGrabbedDragId();
            if (dragId) {
                event.preventDefault();
                const renderingSupport = this.context as RenderingSupport | undefined;
                const consumed = this.commitDrop(dragId, "INTO", renderingSupport);
                if (consumed) {
                    event.stopPropagation();
                }
                KeyboardDragState.clear();
            }
        }
    };

    public render(): JSX.Element {
        const node = this.props.node;
        const hasTarget = Array.isArray(node.dropTarget) && node.dropTarget.length > 0;
        return (
            <div
                ref={this.rootRef}
                className="react4j-drop-target"
                tabIndex={hasTarget ? 0 : undefined}
                role={hasTarget ? "button" : undefined}
                aria-label={hasTarget ? "Drop target" : undefined}
                data-testid="drop-target"
                onDragOver={this.handleDragOver}
                onDrop={this.handleDrop}
                onKeyDown={hasTarget ? this.handleKeyDown : undefined}
            >
                {Renderer.render(node.content)}
            </div>
        );
    }
}
