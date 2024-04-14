import React from "react";
import { I18nRenderer, I18nTextValue } from "../../I18nText";
import { FormLabelHelper } from "../helper/FormLabelHelper";
import { FormElement } from "../Form";
import { RenderingSupport } from "../../../Renderer";
import { ValidationMessageHelper } from "../helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "../helper/FormDescriptionHelper";
import { DataContextManager } from "../../../data/DataContextManager";
import { Form, FormCheck } from "react-bootstrap";
import Feedback from "react-bootstrap/esm/Feedback";

export interface FormCheckboxFormElement extends FormElement {
    checkbox: FromCheckboxNode;
}

export interface FromCheckboxNode {
    checkboxType: "regular" | "switch"
}

export interface Props {
    id: string;
    element: FormCheckboxFormElement
    renderingSupport?: RenderingSupport;
    onUpdate: (element: FormCheckboxFormElement, value: string) => void
    updateCounter: number;
}

export class FormCheckbox extends React.Component<Props, {}> {
    public static TYPE: string = "FORMCHECKBOX";

    private handleInputChange(element: FormCheckboxFormElement, value: string) {
        this.props.onUpdate(element, value);
    }

    public render(): JSX.Element {
        const element = this.props.element;
        const uiContext = this.props.renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
        const htmlId = this.props.id;
        const ariaDescribedByValidation = ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field);
        const value = DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor);
        return (
            <>
                <Form.Check
                    type={element.checkbox?.checkboxType === "switch" ? "switch" : "checkbox"}
                    id={htmlId}
                    label={I18nRenderer.render(element.label)}
                    checked={value === "true"}
                    onChange={(event) => this.handleInputChange(element, event.target.checked ? "true" : "false")}
                    disabled={element.disabled}
                    readOnly={element.readonly}
                    required={element.required}
                    {...ValidationMessageHelper.determineFormControlValidationProperties(htmlId, uiContext, element.field)}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field)}
                />
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
            </>
        );
    }
}
