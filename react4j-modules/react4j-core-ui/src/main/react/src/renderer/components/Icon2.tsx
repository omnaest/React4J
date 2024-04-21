import React from "react";
import { Node } from "../Renderer";

export interface Icon2Node extends Node
{
    icon: string;
}

export interface Props
{
    node: Icon2Node;
}

export class Icon2 extends React.Component<Props, {}>
{
    public static TYPE: string = "ICON2";

    public render(): JSX.Element
    {
        return (
            <>
                {this.props.node.icon ? <i className={"bi bi-" + this.props.node.icon}></i> : <></>}
            </>
        );
    }
}
