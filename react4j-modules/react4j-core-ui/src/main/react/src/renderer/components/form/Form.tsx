import React from "react";
import "./Form.css";
import { Node, RenderingSupport } from "../../Renderer";
import { DataContextManager } from "../../data/DataContextManager";
import { BusyScopeContext } from "../../support/BusyScopeContext";
import { Handler, HandlerFactory } from "../../handler/Handler";
import { I18nRenderer, I18nTextValue } from "../I18nText";
import { Input, InputFormElement } from "./elements/Input";
import { ValidationMessageHelper } from "./helper/ValidationMessageHelper";
import { FormDescriptionHelper } from "./helper/FormDescriptionHelper";
import { DropDown, DropDownFormElement } from "./elements/DropDown";
import { RenderingSupportHelper } from "../../support/RenderingSupportHelper";
import RerenderingContainer from "../RerenderingContainer";
import LocalRerenderingContainer from "../LocalRerenderingContainer";
import { RerenderingHelper } from "../../support/RerenderingHelper";
import { FormCheckbox, FormCheckboxFormElement } from "./elements/FormCheckbox";
import { FileUpload, FileUploadFormElement, FileUploadFormNode } from "./elements/FileUpload";

export interface FormNode extends Node {
    /** Lay the controls out on one line as a Bootstrap input group (see Form.withInlineControls). */
    inlineControls?: boolean;
    elements: FormElement[];
    responsive: boolean;
    onChange?: Handler;
}

export interface FormElement {
    field: string;
    contextId: string;
    type: string;
    label: I18nTextValue;
    description: I18nTextValue;
    disabled: boolean;
    readonly: boolean;
    required: boolean;
    range?: RangeFormElement;
    button?: ButtonFormElement;
    fileUpload?: FileUploadFormNode;
    colspan?: string;
}

export interface ValidationFeedback {
    valid: boolean;
    messages: ValidationMessage[];
}

export interface ValidationMessage {
    type: "VALID" | "INVALID";
    text: I18nTextValue;
}

export interface ButtonFormElement {
    text: I18nTextValue;
    onClick?: Handler;
    outline?: boolean;
    variant?: "" | "primary" | "secondary" | "success" | "danger" | "warning" | "info" | "light" | "dark" | "link"
    size: "" | "sm" | "lg";
}

export interface RangeFormElement {
    min: string;
    max: string;
    step: string;
}

export interface Props {
    node: FormNode;
    renderingSupport?: RenderingSupport;
}

interface State {
    updateCounter: number;
    /**
     * How many round trips this form's own controls have outstanding. Counted rather than a boolean because a
     * form may hold several buttons, and a boolean would let whichever settled first declare the form idle while
     * another was still working.
     */
    busyCount: number;
}

export class Form extends React.Component<Props, State> {
    public static TYPE: string = "FORM";

    /**
     * Was `public Form()` - a METHOD that happens to share the class's name, not a constructor, and therefore
     * never called. `this.state` was consequently null for this component's entire life, which is why every
     * reference to it in here is written `this.state?.x`: the optional chaining was not defensive style, it was
     * load-bearing, and it hid the defect rather than reporting it.
     *
     * Renaming it to an actual constructor changes the initial value of `updateCounter` from undefined to 0.
     * Children compare successive values of it to decide whether to re-read their field, and 0 is what the
     * original author plainly intended, so this restores the behaviour that was written rather than altering it.
     */
    public constructor(props: Props) {
        super(props);
        this.state = { updateCounter: 0, busyCount: 0 };
    }

    /**
     * Enter in a text input reaches here as an implicit form submission.
     *
     * The browser's own submission is always wrong for a React4J form: it has no action and no method, so the
     * browser navigates to the current page with every field appended as a query string - a full reload, with the
     * typed value silently NOT delivered to the server handler and left in the URL and browser history instead.
     *
     * Cancelling it is necessary but not sufficient: doing only that makes Enter do nothing at all, which is worse
     * than it looks, because a text field that ignores Enter reads as broken. So the default is reproduced rather
     * than removed - the browser would activate the form's first submit button, and this activates the form's first
     * enabled button, which is the same thing given React4J renders every form button as type="button".
     *
     * Clicking a button directly does NOT come through here (a type="button" never submits), so there is no risk of
     * one click being counted twice.
     */
    private handleSubmit = (event: React.FormEvent<HTMLFormElement>): void => {
        event.preventDefault();
        const firstEnabledButton = event.currentTarget.querySelector<HTMLButtonElement>("button:not([disabled])");
        firstEnabledButton?.click();
    };

    private formElement: HTMLFormElement | null = null;

    /**
     * Whether this form is waiting on the server, and therefore whether its controls are out of action.
     */
    private isBusy(): boolean {
        return this.state.busyCount > 0;
    }

    /**
     * Called by this form's own buttons, and by any nested Button component through BusyScopeContext.
     *
     * On settle, focus is returned to the first field. Disabling a control removes it from the focus order, so the
     * browser drops focus to <body> - without this, every submission would end with the caret gone and the user
     * reaching for the mouse to type the next thing. Only done when focus actually was lost, so it never steals
     * focus from somewhere the user deliberately moved it to while waiting.
     */
    private reportBusy = (busy: boolean): void => {
        this.setState((previous) => ({ busyCount: Math.max(0, previous.busyCount + (busy ? 1 : -1)) }), () => {
            if (!this.isBusy()) {
                this.restoreFocusIfItWasLost();
            }
        });
    };

    private restoreFocusIfItWasLost(): void {
        const focusWasLost = !document.activeElement || document.activeElement === document.body;
        if (!focusWasLost) {
            return;
        }
        this.formElement?.querySelector<HTMLElement>("input:not([disabled]), textarea:not([disabled]), select:not([disabled])")?.focus();
    }

    /**
     * Wraps a control's own click handler so the form learns when the round trip it started has settled.
     */
    private trackBusyWhile(onClick: (event: React.MouseEvent<HTMLButtonElement>) => Promise<void>) {
        return (event: React.MouseEvent<HTMLButtonElement>): void => {
            this.reportBusy(true);
            onClick(event).finally(() => this.reportBusy(false));
        };
    }

    private handleInputChange(element: FormElement, value: string | string[], renderingSupport?: RenderingSupport) {
        const updateCounter = DataContextManager.updateFieldByContext(element.contextId, element.field, value, renderingSupport?.uiContextAccessor);
        this.setState({ updateCounter: updateCounter });

        HandlerFactory.handleEvent(this.props.node?.onChange as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor);
    }

    private renderElement(htmlId: string, element: FormElement, inline: boolean = false): React.ReactNode {
        return RerenderingHelper.wrapIntoRerenderingContainer([element.contextId],
            (renderingSupport) => {
                if (element) {
                    if (element.type === Input.TYPE) {
                        return (
                            <Input
                                id={htmlId}
                                inline={inline}
                                element={element as InputFormElement}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === DropDown.TYPE) {
                        return (
                            <DropDown
                                id={htmlId}
                                element={element as DropDownFormElement}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === FormCheckbox.TYPE) {
                        return (
                            <FormCheckbox
                                id={htmlId}
                                element={element as FormCheckboxFormElement}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === FileUpload.TYPE) {
                        return (
                            <FileUpload
                                id={htmlId}
                                element={element as FileUploadFormElement}
                                onUpdate={(element, value) => this.handleInputChange(element, value, renderingSupport)}
                                updateCounter={this.state?.updateCounter}
                                renderingSupport={renderingSupport}
                            />
                        );
                    }
                    else if (element.type === "BUTTON") {
                        const buttonElement = element.button as ButtonFormElement;
                        const uiContext = renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
                        const buttonClassPrefix = buttonElement.outline ? " btn-outline-" : " btn-";
                        const buttonVariantClassName = buttonElement.variant ? buttonClassPrefix + buttonElement.variant : "";
                        const buttonSizeClassName = buttonElement.size ? " btn-" + buttonElement.size : "";
                        const fullWidthClassName = element.colspan ? "w-100 " : "";
                        return (
                            <>
                                {/* Inline mode emits ONLY the control. Bootstrap flattens the corners of every
                                    .input-group child that is not first or last, so a label before the button or a
                                    description after it makes the button neither - and the group renders square at
                                    both ends instead of rounded. */}
                                {!inline && <label htmlFor={htmlId}>{I18nRenderer.render(element.label)}&nbsp;</label>}
                                <button
                                    id={htmlId}
                                    type="button"
                                    disabled={element.disabled === true}
                                    aria-busy={this.isBusy() || undefined}
                                    className={fullWidthClassName + "mt-0 btn" + buttonVariantClassName + buttonSizeClassName}
                                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field)}
                                    onClick={this.trackBusyWhile(HandlerFactory.onClick(buttonElement.onClick as Handler, renderingSupport?.uiContextAccessor, renderingSupport?.nodeContextAccessor))}
                                >
                                    {/* The spinner replaces nothing - it is added beside the label, so the button
                                        keeps its width and the row does not reflow the moment it is pressed. */}
                                    {this.isBusy() && <><span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />&nbsp;</>}
                                    {I18nRenderer.render(buttonElement.text)}
                                </button>
                                {!inline && FormDescriptionHelper.renderDescription(htmlId, element.description)}
                                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
                            </>
                        );
                    }
                    else if (element.type === "RANGE") {
                        const rangeElement = element.range;
                        const uiContext = renderingSupport?.uiContextAccessor?.getUIContextById(element.contextId);
                        const validClassName = ValidationMessageHelper.determineFormControlClassName(uiContext, element.field);
                        return (
                            <>
                                <label htmlFor={htmlId}>{I18nRenderer.render(element.label)}</label>
                                <input type="range"
                                    id={htmlId}
                                    className={"form-range " + validClassName}
                                    aria-describedby={FormDescriptionHelper.determineDescriptionHtmlId(htmlId) + " " + ValidationMessageHelper.determineValidationFeedbackJoinedHtmlIds(htmlId, uiContext, element.field)}
                                    value={DataContextManager.getFieldValue(element.contextId, element.field, this.props.renderingSupport?.uiContextAccessor)}
                                    min={rangeElement?.min || 0}
                                    max={rangeElement?.max || 100}
                                    step={rangeElement?.step || 1}
                                    disabled={element.disabled === true}
                                    required={element.required === true}
                                    onChange={(event) => this.handleInputChange(element, event.target.value, renderingSupport)}
                                />
                                {FormDescriptionHelper.renderDescription(htmlId, element.description)}
                                {ValidationMessageHelper.renderValidationFeedback(htmlId, uiContext, element.field)}
                            </>
                        );
                    }
                }
                return (<></>);
            }

        )
    }

    public render(): JSX.Element {
        const useLayout = this.props.node.elements?.map((element) => !!element.colspan).reduce((l, r) => l || r);
        const defaultColSpan = useLayout ? "col-12" : "";
        const responseSegment = this.props.node.responsive !== false ? "md-" : "";
        const inline = this.props.node.inlineControls === true;
        return (
            // The layout classes sit on the FIELDSET rather than the form: a plain wrapper between the form and
            // its columns would break Bootstrap's row/column relationship, and the fieldset has to be an ancestor
            // of every control for `disabled` to reach them.
            //
            // A fieldset rather than a disabled prop on each element, because that is the one mechanism HTML
            // already has for taking a whole form out of action - it reaches controls this component renders
            // today and any it renders later, with nothing to remember to thread through.
            <BusyScopeContext.Provider value={{ reportBusy: this.reportBusy }}>
            <form noValidate onSubmit={this.handleSubmit} ref={(element) => { this.formElement = element; }}>
            <fieldset className={"react4j-form-fieldset " + (useLayout && !inline ? "row g-3" : "")} disabled={this.isBusy()}>
                {
                    /*
                     * Inline mode drops the per-element column wrapper: an input group joins its children into one
                     * control, and a div around each of them breaks that join, leaving the button detached below the
                     * input. See Form.withInlineControls.
                     */
                    inline
                        ? (
                            <div className="input-group">
                                {this.props.node.elements.map((element) => (
                                    <React.Fragment key={element.field}>{this.renderElement(element?.field, element, true)}</React.Fragment>
                                ))}
                            </div>
                        )
                        : this.props.node.elements.map((element) => {
                            const htmlId = element?.field;
                            const colSpan = element.colspan ? "col-" + responseSegment + element.colspan : defaultColSpan;
                            return (
                                <div className={colSpan} key={element.field}>
                                    {this.renderElement(htmlId, element)}
                                </div>
                            );
                        })
                }
            </fieldset>
            </form>
            </BusyScopeContext.Provider>
        );
    }
}
