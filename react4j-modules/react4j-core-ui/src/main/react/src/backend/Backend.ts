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

    public static sendEvent(target: Target, contextId: string, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor) {
        InFlightTracker.increment();
        return Axios.post(BackendUri.URI_UI_HANDLER, {
            target: target,
            dataWithContext: {
                contextId: contextId,
                data: uiContextAccessor?.getUIContextById(contextId).data,
                internalData: uiContextAccessor?.getUIContextById(contextId).internalData
            }
        }).then((response: AxiosResponse<ResponseBody>) => {
            const responseBody = response?.data;
            const dataWithContext = responseBody?.dataWithContext;
            if (dataWithContext) {
                uiContextAccessor?.updateUIContext({
                    contextId: dataWithContext.contextId,
                    data: dataWithContext.data,
                    internalData: dataWithContext.internalData,
                    updateCounter: 0
                });
            }

            const targetNode = responseBody?.targetNode;
            if (targetNode) {
                nodeContextAccessor?.updateNode(targetNode.node);
            }
        }).finally(() => {
            // CRITICAL: decrement in finally so a failed round-trip still settles the counter.
            InFlightTracker.decrement();
        });
    }

    public static fetchData(target: Target, pageIndex: number): Promise<DataPage> {
        return Axios.post(BackendUri.URI_UI_DATA_SOURCE, {
            target: target,
            pageIndex: pageIndex
        }).then((response: AxiosResponse<DataPage>) => {
            return response?.data;
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
