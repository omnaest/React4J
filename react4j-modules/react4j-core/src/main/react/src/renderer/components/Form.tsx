import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { DataContextManager } from "../data/DataContextManager";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Input } from "./form/Input";
import { ValidationMessageHelper } from "./form/helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./form/helper/FormDescriptionHelper";
import { DropDown, DropDownFormElement } from "./form/DropDown";

export interface FormNode extends Node {
    elements: FormElement[];
}

export interface FormElement {
    field: string;
    contextId: string;
    type: string;
    label: I18nTextValue;
    placeholder: I18nTextValue;
    description: I18nTextValue;
    disabled: boolean;
    required: boolean;
    range?: RangeFormElement;
    validationFeedback: ValidationFeedback;
}

export interface ValidationFeedback {
    valid: boolean;
    messages: ValidationMessage[];
}

export interface ValidationMessage {
    type: "valid" | "invalid";
    text: I18nTextValue;
}

export interface ButtonFormElement extends FormElement {
    text: I18nTextValue;
    onClick?: Handler;
}

export interface RangeFormElement {
    min: string;
    max: string;
    step: string;
}

export interface Props {
    node: FormNode;
    renderingSupport?: RenderingSupport;
}

interface State {
    updateCounter: number;
}

export class Form extends React.Component<Props, State> {
    public static TYPE: string = "FORM";

    private Form() {
        this.state = { updateCounter: 0 };
    }

    private handleInputChange(element: FormElement, value: string) {
        const updateCounter = DataContextManager.updateFieldByContext(element.contextId, element.field, value, this.props.renderingSupport?.uiContextAccessor);
        this.setState({ updateCounter: updateCounter });
    }

    private renderElement(htmlId: string, element: FormElement): React.ReactNode {
        if (element) {
            if (element.type === Input.TYPE) {
                return (
                    <Input
                        id={htmlId}
                        element={element}
                        onUpdate={(element, value) => this.handleInputChange(element, value)}
                        updateCounter={this.state?.updateCounter}
                        renderingSupport={this.props.renderingSupport}
                    />
                );
            }
            else if (element.type === DropDown.TYPE) {
                return (
                    <DropDown
                        id={htmlId}
                        element={element as DropDownFormElement}
                        onUpdate={(element, value) => this.handleInputChange(element, value)}
                        updateCounter={this.state.updateCounter}
                        renderingSupport={this.props.renderingSupport}
                    />
                );
            }
            else if (element.type === "BUTTON") {
                const buttonElement = element as ButtonFormElement;
                return (
                    <>
                        <button
                            id={htmlId}
                            type="button"
                            className="btn btn-primary"
                            aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, element.validationFeedback)}
                            onClick={HandlerFactory.onClick(buttonElement.onClick as Handler, this.props.renderingSupport?.uiContextAccessor, this.props.renderingSupport?.nodeContextAccessor)}
                        >{I18nRenderer.render(buttonElement.text)}</button>
                        {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                        {ValidationMessageHelper.renderValidationFeedback(htmlId, element.validationFeedback)}
                    </>
                );
            }
            else if (element.type === "RANGE") {
                const rangeElement = element.range;
                const validClassName = ValidationMessageHelper.determineFormControlClassName(element.validationFeedback);
                return (
                    <>
                        <label htmlFor={htmlId}>{I18nRenderer.render(element.label)}</label>
                        <input type="range"
                            id={htmlId}
                            className={"form-control form-range " + validClassName}
                            aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, element.validationFeedback)}
                            value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                            min={rangeElement?.min || 0}
                            max={rangeElement?.max || 100}
                            step={rangeElement?.step || 1}
                            disabled={element.disabled === true}
                            required={element.required === true}
                            onChange={(event) => this.handleInputChange(element, event.target.value)}
                        />
                        {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                        {ValidationMessageHelper.renderValidationFeedback(htmlId, element.validationFeedback)}
                    </>
                );
            }
        }
    }

    private renderValidationFeedback(htmlId: string, validationFeedback: ValidationFeedback): React.ReactNode {
        return;
    }

    public render(): JSX.Element {
        return (
            <form noValidate>
                {
                    this.props.node.elements.map((element) => {
                        const htmlId = element?.field;
                        return (
                            <div className="form-group" key={element.field}>
                                {this.renderElement(htmlId, element)}
                            </div>
                        );
                    })
                }
            </form>
        );
    }
}
