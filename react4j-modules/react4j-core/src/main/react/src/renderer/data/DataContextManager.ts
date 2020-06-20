import Axios from "axios";
import { Backend, Target } from "../../backend/Backend";
import { ElementMap } from "../../utils/Utils";

export interface DataContext 
{
    contextId: string;
    data: Data;
    updateCounter: number;
}

export interface Data extends ElementMap<any>
{
}

export class DataContextManager
{

    private static contextIdToDataContext: ElementMap<DataContext> = {};

    public static getOrCreateDataContext(contextId: string): DataContext
    {
        this.initDataContextIfNotExisting(contextId);
        return this.contextIdToDataContext[contextId];
    }

    private static initDataContextIfNotExisting(contextId: string)
    {
        if (!this.contextIdToDataContext[contextId])
        {
            this.contextIdToDataContext[contextId] = {
                contextId: contextId,
                data: {},
                updateCounter: 0
            };
        }
    }

    public static updateField(contextId: string, field: string, value: string): void
    {
        const dataContext = this.getOrCreateDataContext(contextId);
        dataContext.data[field] = value;
        dataContext.updateCounter++;
    }

    public static updateFieldContext(contextId: string, data: ElementMap<any>)
    {
        const dataContext = this.getOrCreateDataContext(contextId);
        dataContext.data = data;
        dataContext.updateCounter++;
    }
}
