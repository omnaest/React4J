import React from "react";
import { FormElement } from "../Form";
import { DataContextManager } from "../../data/DataContextManager";
import { I18nRenderer, I18nTextValue } from "../I18nText";
import { RenderingSupport } from "../../Renderer";
import { ValidationMessageHelper } from "./helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./helper/FormDescriptionHelper";
import { FormLabelHelper } from "./helper/FormLabelHelper";

export interface DropDownFormElement extends FormElement {
    options: DropDownOption[];
}

export interface DropDownOption {
    key: string;
    label: I18nTextValue;
}

export interface Props {
    id: string;
    element: DropDownFormElement
    renderingSupport?: RenderingSupport;
    onUpdate: (element: DropDownFormElement, value: string) => void
    updateCounter: number;
}

interface State {
    updateCounter: number;
}

export class DropDown extends React.Component<Props, State> {
    public static TYPE: string = "DROPDOWN";

    private handleInputChange(element: DropDownFormElement, value: string) {
        this.props.onUpdate(element, value);
    }

    public render(): JSX.Element {
        const element = this.props.element;
        const htmlId = this.props.id;
        const validClassName = ValidationMessageHelper.determineFormControlClassName(element.validationFeedback);
        const ariaDescribedByValidation = ValidationMessageHelper.determineValidationFeedbackHtmlIds(htmlId, this.props.element?.validationFeedback).join(" ");
        return (
            <>
                {FormLabelHelper.renderLabel(htmlId, element.label)}
                <select
                    className={"form-select " + validClassName}
                    id={htmlId}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ariaDescribedByValidation}
                    required={element.required !== false}
                    value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                    onChange={(event) => this.handleInputChange(element, event.target.value)}
                >
                    {this.props.element?.options?.map((option) => (
                        <option selected disabled value={option.key}>{I18nRenderer.render(option.label)}</option>
                    ))}
                </select >
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                {ValidationMessageHelper.renderValidationFeedback(htmlId, element.validationFeedback)}
            </>
        );
    }
}
