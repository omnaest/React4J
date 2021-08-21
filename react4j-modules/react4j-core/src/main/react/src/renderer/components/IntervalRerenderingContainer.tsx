import React, { Dispatch } from "react";
import { Node, Renderer, Target } from "../Renderer";
import { Backend } from "../../backend/Backend";

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
    currentNode: IntervalRerenderingContainerNode;
}

export class IntervalRerenderingContainer extends React.Component<Props, State>
{
    public static TYPE: string = "INTERVALRERENDERINGCONTAINER";

    private static INTERVAL_TIMER_KEY_EMPTY: number = -1;
    private intervalTimerKey: number = IntervalRerenderingContainer.INTERVAL_TIMER_KEY_EMPTY;

    public constructor(props: Props)
    {
        super(props);
        this.state = {
            currentNode: props.node
        };
    }

    public componentDidUpdate(prevProps: Props)
    {
        if (this.props?.node && this.props?.node !== prevProps?.node)
        {
            this.initializeCurrentNode();
        }
    }

    private initializeCurrentNode()
    {
        this.setState({ currentNode: this.props.node });
    }

    public componentDidMount()
    {
        this.initializeCurrentNode();
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
        if (target && this.state.currentNode.active !== false)
        {
            Backend.getUISubNode(target).then((node) => this.setState({ currentNode: node as IntervalRerenderingContainerNode }));
        }
    }

    public render(): JSX.Element
    {
        return (
            <>
                {Renderer.render(this.state?.currentNode?.content)}
            </>
        );
    }
}


