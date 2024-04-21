
import { ElementMap } from "../../utils/Utils";


export interface DataContext {
    contextId: string;
    data: Data;
    updateCounter: number;
}

export interface Data extends ElementMap<any> {
}

export interface UIContextDataNode {
    contextId: string;
    data: UIContextData;
    internalData?: UIContextInternalData;
}

export interface UIContext extends UIContextDataNode {
    updateCounter: number;
}

export interface UIContextData {
    [key: string]: string | string[];
}

export interface UIContextInternalData {
    [key: string]: object;
}

export interface UIContextAccessor {
    getUIContextById(contextId: string): UIContext;

    updateUIContext(uiContext: UIContext): void;

    initializeUIContext(uiContext?: UIContextDataNode): void;
}

export class DataContextManager {
    private static contextIdToDataContext: ElementMap<DataContext> = {};

    public static getOrCreateDataContext(contextId: string): DataContext {
        this.initDataContextIfNotExisting(contextId);
        return this.contextIdToDataContext[contextId];
    }

    private static initDataContextIfNotExisting(contextId: string) {
        if (!this.contextIdToDataContext[contextId]) {
            this.contextIdToDataContext[contextId] = {
                contextId: contextId,
                data: {},
                updateCounter: 0
            };
        }
    }

    public static updateField(contextId: string, field: string, value: string): void {
        const dataContext = this.getOrCreateDataContext(contextId);
        dataContext.data[field] = value;
        dataContext.updateCounter++;
    }

    public static updateFieldByContext(contextId: string, field: string, value: string | string[], uiContextAccessor?: UIContextAccessor): number {
        if (contextId) {
            if (uiContextAccessor) {
                const uiContext = uiContextAccessor.getUIContextById(contextId);
                if (uiContext.data[field] !== value) {
                    uiContext.data[field] = value;
                    uiContext.updateCounter++;
                    uiContextAccessor.updateUIContext(uiContext);
                    return uiContext.updateCounter;
                }
            }
            else {
                console.error("Not able to update field " + field + " as ui context is unavailable: " + contextId);
            }
        }

        return 0;
    }

    public static getFieldValue(contextId: string, field: string, uiContextAccessor: UIContextAccessor | undefined): string | string[] {
        if (uiContextAccessor && contextId && field) {
            const uiContext = uiContextAccessor.getUIContextById(contextId);
            const result = uiContext?.data[field];
            return result ? String(result) : "";
        }
        else {
            return "";
        }
    }


    public static updateFieldContext(contextId: string, data: ElementMap<any>) {
        const dataContext = this.getOrCreateDataContext(contextId);
        dataContext.data = data;
        dataContext.updateCounter++;
    }
}
