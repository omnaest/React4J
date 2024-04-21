import React from "react";
import { RenderingSupport } from "../Renderer";
import { I18nRenderer, I18nTextValue } from "../components/I18nText";
import { DataContextManager } from "../data/DataContextManager";

export interface ValueNode {
    type: string;
}

export interface I18nTextValueNode extends ValueNode {
    value: I18nTextValue;
}

export interface TextFieldNode extends ValueNode {
    contextId: string;
    fieldName: string;
}

export class ValueRenderer {
    public static render(node: ValueNode, renderingSupport?: RenderingSupport): string | string[] {
        if (node) {
            if (node.type === "I18NTEXT") {
                return I18nRenderer.render((node as I18nTextValueNode).value);
            }
            else if (node.type === "TEXTFIELD") {
                const textNode = node as TextFieldNode;
                return DataContextManager.getFieldValue(textNode.contextId, textNode.fieldName, renderingSupport?.uiContextAccessor);
            }
            else {
                console.log("Invalid value node " + node.type);
                console.log(node);
            }
        }
        return "";
    }



}

