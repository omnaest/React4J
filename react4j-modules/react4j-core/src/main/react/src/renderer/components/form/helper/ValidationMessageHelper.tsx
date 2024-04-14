import React from "react";
import { ValidationFeedback } from "../../Form";
import { I18nRenderer } from "../../I18nText";
import { UIContext } from "../../../data/DataContextManager";
import { ElementMap } from "../../../../utils/Utils";
import { Form } from "react-bootstrap";
import { FeedbackType } from "react-bootstrap/esm/Feedback";

export class ValidationMessageHelper {

    public static determineAllFeedbackValidationSummary(validationFeedback: ValidationFeedback): "valid" | "invalid" | "untouched" {
        if (!validationFeedback) {
            return "untouched";
        } else {
            return validationFeedback?.valid !== false ? "valid" : "invalid";
        }
    }

    private static determineValidationFeedback(uiContext: UIContext | undefined, fieldName: string) {
        const fieldToValidationFeedback = uiContext?.internalData?.["validationFeedback"] as unknown as ElementMap<ValidationFeedback>;
        return fieldToValidationFeedback?.[fieldName];
    }

    public static determineFormControlClassName(uiContext: UIContext | undefined, fieldName: string) {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        const validSummary = ValidationMessageHelper.determineAllFeedbackValidationSummary(validationFeedback);
        if (validSummary === "valid") {
            return "is-valid";
        }
        else if (validSummary === "invalid") {
            return "is-invalid";
        }
        else {
            return "";
        }
    }

    public static determineFormControlIsValid(uiContext: UIContext | undefined, fieldName: string) {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        const validSummary = ValidationMessageHelper.determineAllFeedbackValidationSummary(validationFeedback);
        if (validSummary === "valid") {
            return true;
        }
        else {
            return undefined;
        }
    }

    public static determineFormControlValidationProperties(htmlId: string, uiContext: UIContext | undefined, fieldName: string) {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        const isValid = !validationFeedback?.messages?.map(message => message?.type === "INVALID").reduce((l, r) => l || r);
        return {
            isValid: this.determineFormControlIsValid(uiContext, fieldName),
            isInvalid: this.determineFormControlIsInValid(uiContext, fieldName),
            feedback: this.renderValidationFeedback2(htmlId, uiContext, fieldName),
            feedbackType: (isValid ? "valid" : "invalid") as FeedbackType
        }
    }

    public static determineFormControlIsInValid(uiContext: UIContext | undefined, fieldName: string) {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        const validSummary = ValidationMessageHelper.determineAllFeedbackValidationSummary(validationFeedback);
        if (validSummary === "invalid") {
            return true;
        }
        else {
            return undefined;
        }
    }

    public static determineValidationFeedbackHtmlIds(htmlId: string, validationFeedback: ValidationFeedback): string[] {
        return validationFeedback?.messages?.map((message, index) =>
            ValidationMessageHelper.determineValidationMessageId(htmlId, index)
        ) || [];
    }

    public static determineValidationFeedbackJoinedHtmlIds(htmlId: string, uiContext: UIContext | undefined, fieldName: string): string {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        return validationFeedback?.messages?.map((message, index) =>
            ValidationMessageHelper.determineValidationMessageId(htmlId, index)
        )?.join(" ") || "";
    }

    private static determineValidationMessageId(htmlId: string, index: number): string {
        return htmlId + "_validation_message_" + index;
    }

    public static renderValidationFeedback(htmlId: string, uiContext: UIContext | undefined, fieldName: string): React.ReactNode {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        return validationFeedback?.messages?.map((message, index) =>
        (
            <Form.Control.Feedback
                id={ValidationMessageHelper.determineValidationMessageId(htmlId, index)}
                type={message.type === "VALID" ? "valid" : "invalid"}
            >{I18nRenderer.render(message.text)}</Form.Control.Feedback>));
    }

    public static renderValidationFeedback2(htmlId: string, uiContext: UIContext | undefined, fieldName: string): React.ReactNode {
        const validationFeedback: ValidationFeedback = ValidationMessageHelper.determineValidationFeedback(uiContext, fieldName);
        return validationFeedback?.messages?.map((message, index) =>
        (
            <div
                id={ValidationMessageHelper.determineValidationMessageId(htmlId, index)}
            >{I18nRenderer.render(message.text)}</div>));
    }

}