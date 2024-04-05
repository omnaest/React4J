import React from "react";
import { I18nRenderer, I18nTextValue } from "../../I18nText";

export class FormDescriptionHelper {
    public static renderDescription(htmlId: string, description: I18nTextValue): React.ReactNode {
        return (
            <small id={FormDescriptionHelper.determineDescriptionHtmlId(htmlId)}
                className="form-text text-muted"
            >{I18nRenderer.render(description)}</small>
        );
    }

    public static determineDescriptionHtmlId(htmlId: string): string {
        return htmlId + "_description";
    }
}