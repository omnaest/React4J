import { Backend, Target } from "../../backend/Backend";
import { UIContextAccessor } from "../data/DataContextManager";
import { NodeContextAccessor } from "../Renderer";

export interface Handler {
    type: "SERVER" | "CLIENT";
}

export interface ServerHandler extends Handler {
    type: "SERVER",
    target: Target,
    contextId: string;
}

export class HandlerFactory {
    public static onClick(handler: Handler, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor) {
        return (event: React.MouseEvent) => {
            event.preventDefault();
            HandlerFactory.handleEvent(handler, uiContextAccessor, nodeContextAccessor);
        };
    }

    public static handleEvent(handler: Handler, uiContextAccessor?: UIContextAccessor, nodeContextAccessor?: NodeContextAccessor) {
        if (handler && handler.type === "SERVER") {
            const serverHandler = (handler as ServerHandler);
            Backend.sendEvent(serverHandler.target, serverHandler.contextId, uiContextAccessor, nodeContextAccessor);
        }
        else {
            throw new Error("Handler type " + handler.type + " not supported");
        }
    }
}
