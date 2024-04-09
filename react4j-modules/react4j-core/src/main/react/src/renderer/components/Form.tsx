import React from "react";
import { Node, RenderingSupport } from "../Renderer";
import { DataContextManager } from "../data/DataContextManager";
import { Handler, HandlerFactory } from "../handler/Handler";
import { I18nRenderer, I18nTextValue } from "./I18nText";
import { Input } from "./form/Input";
import { ValidationMessageHelper } from "./form/helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./form/helper/FormDescriptionHelper";
import { DropDown, DropDownFormElement } from "./form/DropDown";
import { RenderingSupportHelper } from "../support/RenderingSupportHelper";
import RerenderingContainer from "./RerenderingContainer";
import LocalRerenderingContainer from "./LocalRerenderingContainer";
import { RerenderingHelper } from "../support/RerenderingHelper";

export interface FormNode extends Node {
    elements: FormElement[];
    responsive: boolean;
}

export interface FormElement {
    field: string;
    contextId: string;
    type: string;
    label: I18nTextValue;
    placeholder: I18nTextValue;
    description: I18nTextValue;
    disabled: boolean;
    readonly: boolean;
    required: boolean;
    range?: RangeFormElement;
    button?: ButtonFormElement;
    validationFeedback: ValidationFeedback;
    colspan?: string;
}

export interface ValidationFeedback {
    valid: boolean;
    messages: ValidationMessage[];
}

export interface ValidationMessage {
    type: "VALID" | "INVALID";
    text: I18nTextValue;
}

export interface ButtonFormElement {
    text: I18nTextValue;
    onClick?: Handler;
    outline?: boolean;
    variant?: "" | "primary" | "secondary" | "success" | "danger" | "warning" | "info" | "light" | "dark" | "link"
    size: "" | "sm" | "lg";
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

    public Form() {
        this.state = { updateCounter: 0 };
    }

    private handleInputChange(element: FormElement, value: string | string[], renderingSupport?: RenderingSupport) {
        const updateCounter = DataContextManager.updateFieldByContext(element.contextId, element.field, value, renderingSupport?.uiContextAccessor);
        this.setState({ updateCounter: updateCounter });
    }

    private renderElement(htmlId: string, element: FormElement): React.ReactNode {
        return RerenderingHelper.wrapIntoRerenderingContainer([element.contextId],
            (renderingSupport) => {
                if (element) {
                    if (element.type === Input.TYPE) {
                        return (
                            <Input
                                id={htmlId}
                                element={element}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === DropDown.TYPE) {
                        return (
                            <DropDown
                                id={htmlId}
                                element={element as DropDownFormElement}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === "BUTTON") {
                        const buttonElement = element.button as ButtonFormElement;
                        const uiContext = renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
                        const buttonClassPrefix = buttonElement.outline ? " btn-outline-" : " btn-";
                        const buttonVariantClassName = buttonElement.variant ? buttonClassPrefix + buttonElement.variant : "";
                        const buttonSizeClassName = buttonElement.size ? " btn-" + buttonElement.size : "";
                        const fullWidthClassName = element.colspan ? "w-100 " : "";
                        return (
                            <>
                                <label htmlFor={htmlId}>{I18nRenderer.render(element.label)}&nbsp;</label>
                                <button
                                    id={htmlId}
                                    type="button"
                                    disabled={element.disabled === true}
                                    className={fullWidthClassName + "mt-0 btn" + buttonVariantClassName + buttonSizeClassName}
                                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, element.validationFeedback)}
                                    onClick={HandlerFactory.onClick(buttonElement.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor)}
                                >{I18nRenderer.render(buttonElement.text)}</button>
                                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
                            </>
                        );
                    }
                    else if (element.type === "RANGE") {
                        const rangeElement = element.range;
                        const uiContext = renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
                        const validClassName = ValidationMessageHelper.determineFormControlClassName(uiContext, element.field);
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
                                    onChange={(event) => this.handleInputChange(element, event.target.value, renderingSupport)}
                                />
                                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
                            </>
                        );
                    }
                }
                return (<></>);
            }

        )
    }

    public render(): JSX.Element {
        const useLayout = this.props.node.elements?.map((element) => !!element.colspan).reduce((l, r) => l || r);
        const defaultColSpan = useLayout ? "col-12" : "";
        const responseSegment = this.props.node.responsive !== false ? "md-" : "";
        return (
            <form className={useLayout ? "row g-3" : ""} noValidate>
                {
                    this.props.node.elements.map((element) => {
                        const htmlId = element?.field;
                        const colSpan = element.colspan ? "col-" + responseSegment + element.colspan : defaultColSpan;
                        return (
                            <div className={colSpan} key={element.field}>
                                {this.renderElement(htmlId, element)}
                            </div>
                        );
                    })
                }
            </form>
        );
    }
}
