
import React from "react";
import { Node } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Image, ImageNode } from "./Image";

export interface ImageIndexNode extends Node
{
    entries: ImageIndexEntry[];
}

export interface ImageIndexEntry
{
    title: I18nTextValue;
    id: string;
    image: string;
}

export interface Props
{
    node: ImageIndexNode;
    render: (node: Node) => JSX.Element;
}

export class ImageIndex extends React.Component<Props, {}>
{
    public static TYPE: string = "IMAGEINDEX";


    private chunk(entries: any[], size: number): any[][]
    {
        const result: any[][] = [];

        let columnCounter: number = 0;
        let elements: any[] = [];
        for (const entry of entries)   
        {
            //
            if (columnCounter === 0)
            {
                elements = new Array(size);
                result.push(elements);
            }

            //
            elements[columnCounter] = entry;

            //
            columnCounter++;
            if (columnCounter >= size)
            {
                columnCounter = 0;
            }
        }

        return result;
    }

    public render(): JSX.Element
    {
        const columns: ImageIndexEntry[][] = this.chunk(this.props.node.entries, 2);
        return (
            <div className="row">
                {
                    columns.map((column) =>
                        <div className="col-lg-6">
                            {
                                <div className="row">
                                    {
                                        column.map(cell =>
                                        (
                                            <div className="col-6">
                                                <div className="card">
                                                    <h5 className="card-header">{I18nRenderer.render(cell.title)}</h5>
                                                    <div className="card-body">
                                                        <p className="card-text">
                                                            <a href={"#" + cell.id}>
                                                                {this.props.render(
                                                                    {
                                                                        type: Image.TYPE,
                                                                        name: cell.title,
                                                                        image: cell.image
                                                                    } as ImageNode
                                                                )}
                                                            </a>
                                                        </p>
                                                    </div>

                                                </div>
                                            </div>
                                        )
                                        )
                                    }
                                </div>
                            }
                        </div>
                    )
                }
            </div>
        );

    }
}
