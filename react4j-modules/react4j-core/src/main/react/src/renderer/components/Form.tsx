import React from "react";
import { Node, Renderer } from "../Renderer";
import { I18nTextValue, I18nRenderer } from "./I18nText";
import { HandlerFactory, Handler } from "../handler/Handler";
import { DataContextManager } from "../data/DataContextManager";

export interface FormNode extends Node
{
    elements: FormElement[];
}

export interface FormElement
{
    field: string;
    contextId: string;
    type: string;
    label: I18nTextValue;
    placeholder: I18nTextValue;
    description: I18nTextValue;
}

export interface ButtonFormElement extends FormElement
{
    text: I18nTextValue;
    onClick?: Handler;
}

export interface Props
{
    node: FormNode;
}

export class Form extends React.Component<Props, {}>
{
    public static TYPE: string = "FORM";

    private renderElement(element: FormElement): React.ReactNode
    {
        if (element)
        {
            if (element.type === "INPUT")
            {
                return (
                    <input
                        id={element.field}
                        className="form-control"
                        aria-describedby={element.field + "_description"}
                        placeholder={I18nRenderer.render(element.placeholder)}
                        onChange={(event) => DataContextManager.updateField(element.contextId, element.field, event.target.value)}
                    />
                );
            } else if (element.type === "BUTTON")
            {
                const buttonElement = element as ButtonFormElement;
                return (
                    <button
                        id={element.field}
                        type="button"
                        className="btn btn-primary"
                        aria-describedby={element.field + "_description"}
                        onClick={HandlerFactory.onClick(buttonElement.onClick as Handler)}
                    >{I18nRenderer.render(buttonElement.text)}</button>
                );
            }
        }
    }

    public render(): JSX.Element
    {
        return (
            <form>
                {
                    this.props.node.elements.map((element) =>
                        (
                            <div className="form-group">
                                <label htmlFor={element.field}>{I18nRenderer.render(element.label)}</label>
                                {this.renderElement(element)}
                                <small id={element.field + "_description"}
                                    className="form-text text-muted"
                                >{I18nRenderer.render(element.description)}</small>
                            </div>
                        )
                    )
                }
            </form>
        );
    }
}
