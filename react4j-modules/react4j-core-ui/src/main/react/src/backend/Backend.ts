import Axios, { AxiosResponse } from "axios";
import { UIContextAccessor } from "../renderer/data/DataContextManager";
import { Node, NodeContextAccessor } from "../renderer/Renderer";
import { ElementMap } from "../utils/Utils";
import { InFlightTracker } from "./InFlightTracker";

export class BackendUri {
    public static LOCALE_CONTEXT: string | null = document.documentElement.lang && document.documentElement.lang !== "%LOCALE%" ? document.documentElement.lang : null;
    public static URI_UI = (process.env.REACT_APP_BASEURL || "") + (BackendUri.LOCALE_CONTEXT ? BackendUri.LOCALE_CONTEXT + "/" : "") + "ui";
    public static URI_UI_HANDLER = BackendUri.URI_UI + "/event";
    public static URI_UI_DATA_SOURCE = BackendUri.URI_UI + "/data/query";
    public static URI_UPLOAD = (process.env.REACT_APP_BASEURL || "") + (BackendUri.LOCALE_CONTEXT ? BackendUri.LOCALE_CONTEXT + "/" : "") + "ui/upload";

    /**
     * Resolves a (possibly server-supplied) relative uri segment, e.g. "ui/upload", against the same
     * base + optional locale prefix used by URI_UI/URI_UPLOAD. Absolute uris (http/https) are returned unchanged.
     */
    public static resolve(uri: string): string {
        if (!uri) {
            return BackendUri.URI_UPLOAD;
        }
        if (/^https?:\/\//i.test(uri)) {
            return uri;
        }
        return (process.env.REACT_APP_BASEURL || "") + (BackendUri.LOCALE_CONTEXT ? BackendUri.LOCALE_CONTEXT + "/" : "") + uri;
    }
}

export class Target extends Array<string> {
}

interface DataWithContext {
    internalData: InternalData;
    contextId: string;
    data: ElementMap<any>;
}

interface InternalData extends ElementMap<any> { }

interface ResponseBody {
    dataWithContext: DataWithContext;
    dataWithContexts?: DataWithContext[];
    target: Target;
    targetNode: TargetNode;
}

interface TargetNode {
    target: Target;
    node: Node;
}

export interface DataPage {
    elements: ElementData[];
}

export interface ElementData {
    fieldToValue: InternalData;
}

export interface UploadReceipt {
    uploadId: string;
    filename: string;
    size: number;
    contentType: string;
}

export class Backend {
    public static getUI() {
        return Axios.get(BackendUri.URI_UI);
    }

    public static getUISubNode(target: Target): Promise<Node> {
        return Axios.post(BackendUri.URI_UI, {
            target: target
        }).then((response: AxiosResponse<TargetNode>) => response?.data?.node);
    }

    /**
     * Posts the WHOLE page's data, not only the context the event came from.
     *
     * `dataWithContext` still names the originating context -- it is the event's identity, and what the response
     * echoes back. `dataWithContexts` carries every context alongside it, because the server renders the whole
     * page in response and each component reads its own state out of the submitted data. Sending one context
     * meant every other component rendered from defaults: a chat submission came back with the page's TreeTable
     * reset to tree mode and unfiltered, having discarded a view the user switched on and a filter the agent had
     * just applied.
     */
    public static sendEvent(target: Target, contextId: string, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor) {
        InFlightTracker.increment();
        return Axios.post(BackendUri.URI_UI_HANDLER, {
            target: target,
            dataWithContext: {
                contextId: contextId,
                data: uiContextAccessor?.getUIContextById(contextId).data,
                internalData: uiContextAccessor?.getUIContextById(contextId).internalData
            },
            dataWithContexts: uiContextAccessor?.getAllUIContexts().map((uiContext) => ({
                contextId: uiContext.contextId,
                data: uiContext.data,
                internalData: uiContext.internalData
            }))
        }).then((response: AxiosResponse<ResponseBody>) => {
            const responseBody = response?.data;
            // Every context the server echoed, each carrying only the fields it owns.
            //
            // WHY NOT JUST THE ORIGINATING ONE. A handler can change a field belonging to a component somewhere
            // else on the page -- that is what driving another component amounts to -- and applying only the
            // context the event came from would drop that change on the floor. The server decides which context
            // each field belongs to; this side simply applies what it is told.
            const echoedContexts = responseBody?.dataWithContexts?.length
                ? responseBody.dataWithContexts
                : (responseBody?.dataWithContext ? [responseBody.dataWithContext] : []);
            echoedContexts.forEach((echoed) => {
                uiContextAccessor?.updateUIContext({
                    contextId: echoed.contextId,
                    data: echoed.data,
                    internalData: echoed.internalData,
                    updateCounter: 0
                });
            });

            const targetNode = responseBody?.targetNode;
            if (targetNode) {
                nodeContextAccessor?.updateNode(targetNode.node);
            }
        }).finally(() => {
            // CRITICAL: decrement in finally so a failed round-trip still settles the counter.
            InFlightTracker.decrement();
        });
    }

    /**
     * Tracked like every other round trip. It was not, which meant a table fetching its next page produced no
     * feedback anywhere - the one case a page-level indicator is unambiguously for, and the one it was missing.
     */
    public static fetchData(target: Target, pageIndex: number): Promise<DataPage> {
        InFlightTracker.increment();
        return Axios.post(BackendUri.URI_UI_DATA_SOURCE, {
            target: target,
            pageIndex: pageIndex
        }).then((response: AxiosResponse<DataPage>) => {
            return response?.data;
        }).finally(() => {
            // CRITICAL: decrement in finally so a failed round-trip still settles the counter.
            InFlightTracker.decrement();
        });
    }

    public static uploadFile(uploadUrl: string, uploadId: string, file: File): Promise<UploadReceipt> {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("uploadId", uploadId);
        InFlightTracker.increment();
        return Axios.post(BackendUri.resolve(uploadUrl), formData, {
            headers: {
                "Content-Type": "multipart/form-data"
            }
        }).then((response: AxiosResponse<UploadReceipt>) => {
            return response?.data;
        }).finally(() => {
            // CRITICAL: decrement in finally so a failed round-trip still settles the counter.
            InFlightTracker.decrement();
        });
    }
}
