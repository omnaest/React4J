import React from "react";
import { I18nRenderer, I18nTextValue } from "../../I18nText";
import { Form } from "react-bootstrap";

export class FormDescriptionHelper {
    public static renderDescription(htmlId: string, description: I18nTextValue): React.ReactNode {
        return (
            <small id={FormDescriptionHelper.determineDescriptionHtmlId(htmlId)}
                className="form-text text-muted"
            >{I18nRenderer.render(description)}</small>
        );
    }

    public static renderDescription2(htmlId: string, description: I18nTextValue): React.ReactNode {
        return (
            <Form.Text
                id={FormDescriptionHelper.determineDescriptionHtmlId(htmlId)}
                muted
            >{I18nRenderer.render(description)}</Form.Text>
        );
    }

    public static determineDescriptionHtmlId(htmlId: string): string {
        return htmlId + "_description";
    }
}