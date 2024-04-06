import React from "react";
import { ValidationFeedback } from "../../Form";
import { I18nRenderer } from "../../I18nText";
import { UIContext } from "../../../data/DataContextManager";
import { ElementMap } from "../../../../utils/Utils";

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

    public static determineValidationFeedbackHtmlIds(htmlId: string, validationFeedback: ValidationFeedback): string[] {
        return validationFeedback?.messages?.map((message, index) =>
            ValidationMessageHelper.determineValidationMessageId(htmlId, index)
        ) || [];
    }

    public static determineValidationFeedbackJoinedHtmlIds(htmlId: string, validationFeedback: ValidationFeedback): string {
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
            <div id={ValidationMessageHelper.determineValidationMessageId(htmlId, index)}
                className={message.type === "VALID" ? "valid-feedback" : "invalid-feedback"} >
                {I18nRenderer.render(message.text)}
            </div>
        ));
    }
}