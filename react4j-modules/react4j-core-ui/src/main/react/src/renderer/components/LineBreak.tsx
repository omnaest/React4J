import React from "react";
import { Node } from "../Renderer";
import { I18nTextValue } from "./I18nText";

export interface LineBreakNode extends Node
{
    text: I18nTextValue;
    link: string;
}

export interface Props
{
    node: LineBreakNode;
}

export class LineBreak extends React.Component<Props, {}>
{
    public static TYPE: string = "LINEBREAK";

    public render(): JSX.Element
    {
        return (
            <br></br>
        );
    }
}
