import Axios from "axios";
import { DataContextManager } from "../renderer/data/DataContextManager";
import { ElementMap } from "../utils/Utils";

export class BackendUri
{
    public static URI_UI = "ui";
    public static URI_UI_HANDLER = BackendUri.URI_UI + "/event";
}

export class Target extends Array
{
}

interface DataWithContext
{
    contextId: string;
    data: ElementMap<any>;
}

export class Backend
{
    public static getUI()
    {
        return Axios.get(BackendUri.URI_UI);
    }

    public static sendEvent(target: Target, contextId: string)
    {
        return Axios.post(BackendUri.URI_UI_HANDLER, {
            target: target,
            dataWithContext: {
                contextId: contextId,
                data: DataContextManager.getOrCreateDataContext(contextId).data
            }
        }).then((response) =>
        {
            const dataWithContext = response.data as DataWithContext;
            if (dataWithContext)
            {
                DataContextManager.updateFieldContext(dataWithContext.contextId, dataWithContext.data);
            }
        });
    }
}
