import React from "react";
import { Node, Renderer } from "../Renderer";
import { ListViewElementNode } from "./ListViewElement";
import { ButtonNode } from "./Button";
import { I18nTextValue } from "./I18nText";

export interface ListViewNode extends Node {
    element: Node;
}

export interface Props {
    node: ListViewNode;
}

export class ListView extends React.Component<Props, {}> {
    public static TYPE: string = "LISTVIEW";

    public render(): JSX.Element {
        const elements = [this.props.node.element, this.props.node.element];
        return (
            <>
                <div >
                    {elements?.map((element, index) => (
                        <span key={index}>{Renderer.render(element)}</span>
                    ))}
                </div>
            </>
        );
    }
}
