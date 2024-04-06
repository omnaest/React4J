import React from "react";
import { FormElement } from "../Form";
import { DataContextManager } from "../../data/DataContextManager";
import { I18nRenderer, I18nTextValue } from "../I18nText";
import { RenderingSupport } from "../../Renderer";
import { ValidationMessageHelper } from "./helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./helper/FormDescriptionHelper";
import { FormLabelHelper } from "./helper/FormLabelHelper";

export interface DropDownFormElement extends FormElement {
    dropDown: DropDownNode;
}

export interface DropDownNode {
    options: DropDownOption[];
}

export interface DropDownOption {
    key: string;
    label: I18nTextValue;
    disabled?: boolean;
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
        const uiContext = this.props.renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
        const htmlId = this.props.id;
        const validClassName = ValidationMessageHelper.determineFormControlClassName(uiContext, element.field);
        const ariaDescribedByValidation = ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, this.props.element?.validationFeedback);
        return (
            <>
                {FormLabelHelper.renderLabel(htmlId, element.label)}
                <select
                    className={"form-control form-select " + validClassName}
                    id={htmlId}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ariaDescribedByValidation}
                    required={element.required === true}
                    value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                    onChange={(event) => this.handleInputChange(element, event.target.value)}
                >
                    {this.props.element?.dropDown?.options?.map((option) => (
                        <option disabled={option.disabled} value={option.key}>{I18nRenderer.render(option.label)}</option>
                    ))}
                </select >
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
            </>
        );
    }
}
