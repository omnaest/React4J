import Axios, { AxiosResponse } from "axios";
import { DataContextManager, UIContextAccessor } from "../renderer/data/DataContextManager";
import { Node, NodeContextAccessor } from "../renderer/Renderer";
import { ElementMap } from "../utils/Utils";

export class BackendUri
{
    public static LOCALE_CONTEXT: string = document.documentElement.lang;
    public static URI_UI = (BackendUri.LOCALE_CONTEXT ? BackendUri.LOCALE_CONTEXT + "/" : "") + "ui";
    public static URI_UI_HANDLER = BackendUri.URI_UI + "/event";
}

export class Target extends Array<string>
{
}

interface DataWithContext
{
    contextId: string;
    data: ElementMap<any>;
}

interface ResponseBody
{
    dataWithContext: DataWithContext;
    target: Target;
    targetNode: TargetNode;
}

interface TargetNode
{
    target: Target;
    node: Node;
}

export class Backend
{
    public static getUI()
    {
        return Axios.get(BackendUri.URI_UI);
    }

    public static getUISubNode(target: Target): Promise<Node>
    {
        return Axios.post(BackendUri.URI_UI, {
            target: target
        }).then((response: AxiosResponse<TargetNode>) => response?.data?.node);
    }


    public static sendEvent(target: Target, contextId: string, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor)
    {
        return Axios.post(BackendUri.URI_UI_HANDLER, {
            target: target,
            dataWithContext: {
                contextId: contextId,
                data: uiContextAccessor?.getUIContextById(contextId).data
            }
        }).then((response: AxiosResponse<ResponseBody>) =>
        {
            const responseBody = response.data;

            const dataWithContext = responseBody.dataWithContext;
            if (dataWithContext)
            {
                uiContextAccessor?.updateUIContext({
                    contextId: dataWithContext.contextId,
                    data: dataWithContext.data,
                    updateCounter: 0
                });
            }

            const targetNode = responseBody.targetNode;
            if (targetNode)
            {
                nodeContextAccessor?.updateNode(targetNode.node);
            }
        });
    }
}
