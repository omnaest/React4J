import React from "react";
import { Backend } from "../../backend/Backend";
import { Node, Renderer, Target } from "../Renderer";
import { RenderingSupportContext } from "../support/RenderingSupportContext";
import { RenderingSupportHelper, UIContextsState, UpdateActions } from "../support/RenderingSupportHelper";

export interface IntervalRerenderingContainerNode extends Node
{
    content: Node;
    intervalDuration: number;
    active: boolean;
}

export interface Props
{
    node: IntervalRerenderingContainerNode;
}

interface State
{
}

type PropsWithReduxStore = Props & UIContextsState & UpdateActions;

class IntervalRerenderingContainer extends React.Component<PropsWithReduxStore, State>
{
    public static TYPE: string = "INTERVALRERENDERINGCONTAINER";

    private static INTERVAL_TIMER_KEY_EMPTY: number = -1;
    private intervalTimerKey: number = IntervalRerenderingContainer.INTERVAL_TIMER_KEY_EMPTY;

    public componentDidMount()
    {
        this.intervalTimerKey = setInterval(() => this.reloadChildrenAndRefresh(), this.props.node.intervalDuration);
    }

    public componentWillUnmount()
    {
        this.clearCurrentIntervalTimerIfPresent();
    }

    private clearCurrentIntervalTimerIfPresent()
    {
        if (this.intervalTimerKey !== IntervalRerenderingContainer.INTERVAL_TIMER_KEY_EMPTY)
        {
            clearInterval(this.intervalTimerKey);
            this.intervalTimerKey = IntervalRerenderingContainer.INTERVAL_TIMER_KEY_EMPTY;
        }
    }

    private reloadChildrenAndRefresh()
    {
        const target: Target = this.props?.node?.target;
        if (target && this.props.node.active !== false)
        {
            Backend.getUISubNode(target).then((node) => this.props.updateNodeAction(node as IntervalRerenderingContainerNode));
        }
    }

    public render(): JSX.Element
    {
        const renderingSupport = RenderingSupportHelper.newRenderingSupport(this.props, this.props);
        return (
            <RenderingSupportContext.Provider value={renderingSupport}>
                {
                    Renderer.render(this.props.node?.content, renderingSupport)
                }
            </RenderingSupportContext.Provider>
        );
    }
}

export default RenderingSupportHelper.connect<typeof IntervalRerenderingContainer>(IntervalRerenderingContainer, (props: Props) => props.node?.content?.uiContextIds, (props: Props) => props.node);
