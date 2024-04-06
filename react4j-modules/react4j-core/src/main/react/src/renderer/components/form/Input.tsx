import React from "react";
import { FormElement } from "../Form";
import { DataContextManager } from "../../data/DataContextManager";
import { I18nRenderer } from "../I18nText";
import { RenderingSupport } from "../../Renderer";
import { ValidationMessageHelper } from "./helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./helper/FormDescriptionHelper";
import { FormLabelHelper } from "./helper/FormLabelHelper";

export interface InputFormElement extends FormElement {
}

export interface Props {
    id: string;
    element: InputFormElement
    renderingSupport?: RenderingSupport;
    onUpdate: (element: InputFormElement, value: string) => void
    updateCounter: number;
}

export class Input extends React.Component<Props> {
    public static TYPE: string = "INPUT";

    private handleInputChange(element: FormElement, value: string) {
        this.props.onUpdate(element, value);
    }

    public render(): JSX.Element {
        const element = this.props.element;
        const uiContext = this.props.renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
        const htmlId = this.props.id;
        const validClassName = ValidationMessageHelper.determineFormControlClassName(uiContext, element.field);
        const ariaDescribedByValidation = ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, element.validationFeedback);
        return (
            <>
                {FormLabelHelper.renderLabel(htmlId, element.label)}
                <input
                    id={htmlId}
                    className={"form-control " + validClassName}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ariaDescribedByValidation}
                    placeholder={I18nRenderer.render(element.placeholder)}
                    aria-label={I18nRenderer.render(element.placeholder)}
                    required={element.required === true}
                    value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                    onChange={(event) => this.handleInputChange(element, event.target.value)}
                />
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
            </>
        );
    }
}
