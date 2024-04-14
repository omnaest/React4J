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
    multiselect: boolean;
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
    onUpdate: (element: DropDownFormElement, value: string | string[]) => void
    updateCounter: number;
}

interface State {
    updateCounter: number;
}

export class DropDown extends React.Component<Props, State> {
    public static TYPE: string = "DROPDOWN";

    private handleInputChange(element: DropDownFormElement, value: string, options: HTMLOptionsCollection) {
        if (element.dropDown?.multiselect) {
            const values: string[] = [];
            for (var i = 0, l = options.length; i < l; i++) {
                if (options[i].selected) {
                    values.push(options[i].value);
                }
            }
            this.props.onUpdate(element, values);
        } else {
            this.props.onUpdate(element, value);
        }
    }

    public render(): JSX.Element {
        const element = this.props.element;
        const uiContext = this.props.renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
        const htmlId = this.props.id;
        const validClassName = ValidationMessageHelper.determineFormControlClassName(uiContext, element.field);
        const ariaDescribedByValidation = ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field);
        const value = DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor);
        return (
            <>
                {FormLabelHelper.renderLabelLegacy(htmlId, element.label)}
                <select
                    className={"form-control form-select " + validClassName}
                    id={htmlId}
                    value={value}
                    onChange={(event) => this.handleInputChange(element, event.target.value, event.target.options)}
                    required={element.required === true}
                    multiple={element.dropDown.multiselect}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ariaDescribedByValidation}
                >
                    {this.props.element?.dropDown?.options?.map((option) => (
                        <option selected={option.key === value} disabled={option.disabled} value={option.key}>{I18nRenderer.render(option.label)}</option>
                    ))}
                </select >
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
            </>
        );
    }
}
