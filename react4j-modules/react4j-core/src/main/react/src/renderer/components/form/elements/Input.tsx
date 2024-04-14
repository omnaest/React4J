import React from "react";
import { FormElement } from "../Form";
import { DataContextManager } from "../../../data/DataContextManager";
import { I18nRenderer, I18nTextValue } from "../../I18nText";
import { RenderingSupport } from "../../../Renderer";
import { ValidationMessageHelper } from "../helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "../helper/FormDescriptionHelper";
import { FormLabelHelper } from "../helper/FormLabelHelper";
import { Form } from "react-bootstrap";

export interface InputFormElement extends FormElement {
    input: InputFormNode;
}

export interface InputFormNode {
    type: string;
    placeholder: I18nTextValue;
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

    private handleInputChange(element: InputFormElement, value: string) {
        this.props.onUpdate(element, value);
    }

    public render(): JSX.Element {
        const element = this.props.element;
        const uiContext = this.props.renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
        const htmlId = this.props.id;
        const ariaDescribedByValidation = ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field);
        return (
            <>
                {FormLabelHelper.renderLabel(htmlId, element.label)}
                <Form.Control
                    id={htmlId}
                    type={element.input?.type || "text"}
                    name={element.field}
                    placeholder={I18nRenderer.render(element.input?.placeholder)}
                    value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                    onChange={(event) => this.handleInputChange(element, event.target.value)}
                    required={element.required === true}
                    readOnly={element.readonly === true}
                    {...ValidationMessageHelper.determineFormControlValidationProperties(htmlId, uiContext, element.field)}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ariaDescribedByValidation}
                    aria-label={I18nRenderer.render(element.input?.placeholder)}
                />
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
            </>
        );
    }
}
