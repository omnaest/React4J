import React from "react";
import { Node, Renderer } from "../Renderer";
import { KeyboardDragState } from "./KeyboardDragState";
import "./Draggable.css";

/**
 * Frozen node-JSON contract emitted by DraggableImpl (plan-235 S1): a purely client-side drag source -
 * no server handler, no routable Target of its own. See DropTarget.tsx for the receiving half.
 */
export interface DraggableNode extends Node {
    content: Node;
    dragId: string;
}

export interface Props {
    node: DraggableNode;
}

interface State {
    grabbed: boolean;
}

/**
 * Real DRAGGABLE renderer (plan-235 S1). Marks its rendered content `draggable=true` and, on `dragstart`,
 * writes its `dragId` onto the browser's native `DataTransfer` payload (`text/plain`) - the channel a
 * `DropTarget` elsewhere on the page reads the identity back from on `drop`. HTML5 drag-and-drop keeps
 * this state entirely inside the browser's own drag machinery, never in React/Redux (plan-235 S9.5c probe
 * finding).
 *
 * Keyboard affordance (AC-S1-7): focusable (`tabIndex=0`) with an `aria-label` describing the pick-up
 * action; Space/Enter toggles a local "grabbed" flag (reflected via `aria-grabbed`) and records/clears the
 * dragId in {@link KeyboardDragState} so a focused `DropTarget` can then receive it via its OWN
 * Space/Enter. Deliberately bounded to this ONE keyboard path - not a second parallel interaction model.
 */
export class Draggable extends React.Component<Props, State> {
    public static TYPE: string = "DRAGGABLE";

    constructor(props: Props) {
        super(props);
        this.state = { grabbed: false };
    }

    private handleDragStart = (event: React.DragEvent<HTMLDivElement>): void => {
        event.dataTransfer.setData("text/plain", this.props.node.dragId || "");
        event.dataTransfer.effectAllowed = "move";
    };

    /**
     * plan-235 S1 corrective round 2 (Defect A). `stopPropagation()` on the handled path is load-bearing,
     * not defensive: the demo composition nests `DropTarget(Draggable(content))`, so without it a pick-up
     * keydown bubbles into the Draggable's OWN enclosing `DropTarget`, whose `handleKeyDown` reads the
     * JUST-SET {@link KeyboardDragState} synchronously (before this handler even returns) and self-drops -
     * dispatching a spurious `/ui/event` and clearing the grabbed state before the user ever reaches the
     * real drop target. Empirically confirmed red-before-green in `Draggable.test.tsx`/`DropTarget.test.tsx`.
     * An unhandled key must NOT stop propagation - only the path that actually toggles pick-up/put-down.
     */
    private handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>): void => {
        if (event.key === " " || event.key === "Enter") {
            event.preventDefault();
            event.stopPropagation();
            const grabbed = !this.state.grabbed;
            this.setState({ grabbed });
            KeyboardDragState.setGrabbedDragId(grabbed ? this.props.node.dragId : null);
        }
    };

    public render(): JSX.Element {
        const node = this.props.node;
        return (
            <div
                className="react4j-draggable"
                draggable={true}
                tabIndex={0}
                role="button"
                aria-grabbed={this.state.grabbed}
                aria-label={`Draggable item ${node.dragId || ""}`}
                data-testid="draggable"
                data-drag-id={node.dragId}
                onDragStart={this.handleDragStart}
                onKeyDown={this.handleKeyDown}
            >
                {Renderer.render(node.content)}
            </div>
        );
    }
}
