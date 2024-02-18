import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { DataContextManager } from "../data/DataContextManager";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";

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
    disabled: boolean;
    range?: RangeFormElement;
}

export interface ButtonFormElement extends FormElement
{
    text: I18nTextValue;
    onClick?: Handler;
}

export interface RangeFormElement 
{
    min: string;
    max: string;
    step: string;
}

export interface Props
{
    node: FormNode;
    renderingSupport?: RenderingSupport;
}

interface State
{
    updateCounter: number;
}

export class Form extends React.Component<Props, State>
{
    public static TYPE: string = "FORM";

    private handleInputChange(element: FormElement, value: string)
    {
        const updateCounter = DataContextManager.updateFieldByContext(element.contextId, element.field, value, this.props.renderingSupport?.uiContextAccessor);
        this.setState({ updateCounter: updateCounter });
    }

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
                        value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                        onChange={(event) => this.handleInputChange(element, event.target.value)}
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
                        onClick={HandlerFactory.onClick(buttonElement.onClick as Handler, this.props.renderingSupport?.uiContextAccessor, this.props.renderingSupport?.nodeContextAccessor)}
                    >{I18nRenderer.render(buttonElement.text)}</button>
                );
            }
            else if (element.type === "RANGE")
            {
                const rangeElement = element.range;
                return (
                    <input type="range"
                        id={element.field}
                        className="form-control form-range"
                        value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                        min={rangeElement?.min || 0}
                        max={rangeElement?.max || 100}
                        step={rangeElement?.step || 1}
                        disabled={element.disabled === true}
                        onChange={(event) => this.handleInputChange(element, event.target.value)}
                    />
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
                        <div className="form-group" key={element.field}>
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
