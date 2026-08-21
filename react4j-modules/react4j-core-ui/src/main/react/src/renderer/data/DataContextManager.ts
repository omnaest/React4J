
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

    /**
     * EVERY context the page currently holds.
     *
     * WHY THIS EXISTS. Each component keeps its own state in the submitted data under its own contextId --
     * TreeTable's mode, filters, sort and window all live in the root ("") context, a Form's fields under its
     * own. An event used to post only the context it came from, so the server re-rendered every OTHER component
     * from defaults: a chat submission reset the table beside it to tree mode with no filters, discarding what
     * the user and the agent had just set. The page's state has to travel together for a round trip to be able
     * to render the page.
     *
     * NOT a snapshot: callers must treat the returned contexts as live and read them immediately.
     */
    getAllUIContexts(): UIContext[];

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
