import React from "react";
import { I18nRenderer, I18nTextValue } from "../../I18nText";
import { Form } from "react-bootstrap";

export class FormLabelHelper {
    public static renderLabelLegacy(htmlId: string, label: I18nTextValue, className?: string): React.ReactNode {
        return (
            <label
                id={FormLabelHelper.determineLabelHtmlId(htmlId)}
                htmlFor={htmlId}
                className={className || ""}
            >{I18nRenderer.render(label)}</label>
        );
    }

    public static renderLabel(htmlId: string, label: I18nTextValue, className?: string): React.ReactNode {
        return (
            <Form.Label
                id={FormLabelHelper.determineLabelHtmlId(htmlId)}
                htmlFor={htmlId}
                className={className || ""}
            >{I18nRenderer.render(label)}</Form.Label>
        );
    }

    public static determineLabelHtmlId(htmlId: string): string {
        return htmlId + "_description";
    }
}