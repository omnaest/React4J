import React from "react";
import "./Form.css";
import { Node, RenderingSupport } from "../../Renderer";
import { DataContextManager } from "../../data/DataContextManager";
import { Handler, HandlerFactory } from "../../handler/Handler";
import { I18nRenderer, I18nTextValue } from "../I18nText";
import { Input, InputFormElement } from "./elements/Input";
import { ValidationMessageHelper } from "./helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./helper/FormDescriptionHelper";
import { DropDown, DropDownFormElement } from "./elements/DropDown";
import { RenderingSupportHelper } from "../../support/RenderingSupportHelper";
import RerenderingContainer from "../RerenderingContainer";
import LocalRerenderingContainer from "../LocalRerenderingContainer";
import { RerenderingHelper } from "../../support/RerenderingHelper";
import { FormCheckbox, FormCheckboxFormElement } from "./elements/FormCheckbox";
import { FileUpload, FileUploadFormElement, FileUploadFormNode } from "./elements/FileUpload";

export interface FormNode extends Node {
    /** Lay the controls out on one line as a Bootstrap input group (see Form.withInlineControls). */
    inlineControls?: boolean;
    elements: FormElement[];
    responsive: boolean;
    onChange?: Handler;
}

export interface FormElement {
    field: string;
    contextId: string;
    type: string;
    label: I18nTextValue;
    description: I18nTextValue;
    disabled: boolean;
    readonly: boolean;
    required: boolean;
    range?: RangeFormElement;
    button?: ButtonFormElement;
    fileUpload?: FileUploadFormNode;
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

    /**
     * Enter in a text input reaches here as an implicit form submission.
     *
     * The browser's own submission is always wrong for a React4J form: it has no action and no method, so the
     * browser navigates to the current page with every field appended as a query string - a full reload, with the
     * typed value silently NOT delivered to the server handler and left in the URL and browser history instead.
     *
     * Cancelling it is necessary but not sufficient: doing only that makes Enter do nothing at all, which is worse
     * than it looks, because a text field that ignores Enter reads as broken. So the default is reproduced rather
     * than removed - the browser would activate the form's first submit button, and this activates the form's first
     * enabled button, which is the same thing given React4J renders every form button as type="button".
     *
     * Clicking a button directly does NOT come through here (a type="button" never submits), so there is no risk of
     * one click being counted twice.
     */
    private handleSubmit = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        const firstEnabledButton = event.currentTarget.querySelector<HTMLButtonElement>("button:not([disabled])");
        firstEnabledButton?.click();
    };

    private handleInputChange(element: FormElement, value: string | string[], renderingSupport?: RenderingSupport) {
        const updateCounter = DataContextManager.updateFieldByContext(element.contextId, element.field, value, renderingSupport?.uiContextAccessor);
        this.setState({ updateCounter: updateCounter });

        HandlerFactory.handleEvent(this.props.node?.onChange as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor);
    }

    private renderElement(htmlId: string, element: FormElement): React.ReactNode {
        return RerenderingHelper.wrapIntoRerenderingContainer([element.contextId],
            (renderingSupport) => {
                if (element) {
                    if (element.type === Input.TYPE) {
                        return (
                            <Input
                                id={htmlId}
                                element={element as InputFormElement}
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
                    else if (element.type === FormCheckbox.TYPE) {
                        return (
                            <FormCheckbox
                                id={htmlId}
                                element={element as FormCheckboxFormElement}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === FileUpload.TYPE) {
                        return (
                            <FileUpload
                                id={htmlId}
                                element={element as FileUploadFormElement}
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
                                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field)}
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
                                    className={"form-range " + validClassName}
                                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field)}
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
        const inline = this.props.node.inlineControls === true;
        return (
            <form className={useLayout && !inline ? "row g-3" : ""} noValidate onSubmit={this.handleSubmit}>
                {
                    /*
                     * Inline mode drops the per-element column wrapper: an input group joins its children into one
                     * control, and a div around each of them breaks that join, leaving the button detached below the
                     * input. See Form.withInlineControls.
                     */
                    inline
                        ? (
                            <div className="input-group">
                                {this.props.node.elements.map((element) => (
                                    <React.Fragment key={element.field}>{this.renderElement(element?.field, element)}</React.Fragment>
                                ))}
                            </div>
                        )
                        : this.props.node.elements.map((element) => {
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
