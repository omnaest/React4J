import React from "react";
import { FormElement } from "../Form";
import { RenderingSupport } from "../../../Renderer";
import { DataContextManager } from "../../../data/DataContextManager";
import { Handler, HandlerFactory, ServerHandler } from "../../../handler/Handler";
import { Backend, UploadReceipt } from "../../../../backend/Backend";
import { FormLabelHelper } from "../helper/FormLabelHelper";
import { FormDescriptionHelper } from "../helper/FormDescriptionHelper";
import { ValidationMessageHelper } from "../helper/ValidationMessageHelper";
import { Form } from "react-bootstrap";

export interface FileUploadFormElement extends FormElement {
    fileUpload: FileUploadFormNode;
}

export interface FileUploadFormNode {
    uploadUrl: string;
    uploadId: string;
    accept?: string;
    maxSize?: number;
    onComplete: ServerHandler;
}

export interface Props {
    id: string;
    element: FileUploadFormElement;
    renderingSupport?: RenderingSupport;
    onUpdate: (element: FileUploadFormElement, value: string) => void;
    updateCounter: number;
}

type UploadStatus = "idle" | "uploading" | "uploaded" | "error";

interface State {
    status: UploadStatus;
    filename?: string;
    errorMessage?: string;
}

export class FileUpload extends React.Component<Props, State> {
    public static TYPE: string = "FILE_UPLOAD";

    constructor(props: Props) {
        super(props);
        this.state = { status: "idle" };
    }

    private handleFileSelected(event: React.ChangeEvent<HTMLInputElement>) {
        const element = this.props.element;
        const file = event.target.files && event.target.files[0];
        if (!file) {
            return;
        }

        const fileUploadNode = element.fileUpload;
        this.setState({ status: "uploading", errorMessage: undefined });

        Backend.uploadFile(fileUploadNode.uploadUrl, fileUploadNode.uploadId, file)
            .then((receipt: UploadReceipt) => {
                this.setState({ status: "uploaded", filename: receipt.filename });

                // Note: unlike Input/DropDown/FormCheckbox, this write + notify sequence is driven directly here
                // (not via props.onUpdate -> Form.handleInputChange) because the wire contract's onComplete
                // handler IS the completion notification for this element; routing through the generic
                // Form onChange as well would double-fire a server round-trip that the FILE_UPLOAD contract
                // does not define. The onUpdate prop is retained only for Props-shape parity with sibling elements.
                const reference = receipt.filename || receipt.uploadId;
                DataContextManager.updateFieldByContext(element.contextId, element.field, reference, this.props.renderingSupport?.uiContextAccessor);

                HandlerFactory.handleEvent(fileUploadNode.onComplete as Handler, this.props.renderingSupport?.uiContextAccessor, this.props.renderingSupport?.nodeContextAccessor);
            })
            .catch(() => {
                this.setState({ status: "error", errorMessage: "Upload failed" });
            });
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
                    type="file"
                    name={element.field}
                    accept={element.fileUpload?.accept}
                    disabled={element.disabled === true || this.state.status === "uploading"}
                    required={element.required === true}
                    readOnly={element.readonly === true}
                    onChange={(event) => this.handleFileSelected(event as React.ChangeEvent<HTMLInputElement>)}
                    {...ValidationMessageHelper.determineFormControlValidationProperties(htmlId, uiContext, element.field)}
                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ariaDescribedByValidation}
                />
                {this.state.status === "uploading" && <div role="status">Uploading...</div>}
                {this.state.status === "uploaded" && <div role="status">Uploaded: {this.state.filename}</div>}
                {this.state.status === "error" && <div role="alert" className="text-danger">{this.state.errorMessage}</div>}
                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
            </>
        );
    }
}
