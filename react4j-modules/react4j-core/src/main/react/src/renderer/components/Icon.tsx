import React from "react";
import { Node } from "../Renderer";

export interface IconNode extends Node
{
    icon: string;
}

export interface Props
{
    node: IconNode;
}

export class Icon extends React.Component<Props, {}>
{
    public static TYPE: string = "ICON";

    public render(): JSX.Element
    {
        return (
            <>
                {this.props.node.icon ? <i className={"fas fa-" + this.props.node.icon}>&nbsp;</i> : <></>}
            </>
        );
    }
}
