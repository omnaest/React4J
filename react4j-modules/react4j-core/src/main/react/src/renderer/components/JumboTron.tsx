
import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";

export interface JumbotronNode extends Node
{
    title: I18nTextValue;
    left: Node[];
    right: Node[];
}

export interface Props
{
    node: JumbotronNode;
    render: (node: Node) => JSX.Element;
}

export class JumboTron extends React.Component<Props, {}>
{

    public render(): JSX.Element
    {
        return (
            <div className="jumbotron">
                <h2>{I18nRenderer.render(this.props.node.title)}</h2>
                <br></br>
                <div className="container-fluid">
                    <div className="row">
                        <div className="col-md-6">
                            {this.props.node.left ? this.props.node.left.map(e => this.props.render(e)) : <></>}
                        </div>
                        <div className="col-md-6">
                            {this.props.node.right ? this.props.node.right.map(e => this.props.render(e)) : <></>}
                        </div>
                    </div>
                </div>
            </div>
        );

    }
}
