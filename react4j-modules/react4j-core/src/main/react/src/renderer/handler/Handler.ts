import Axios from "axios";
import { Backend, Target } from "../../backend/Backend";

export interface Handler
{
    type: "SERVER" | "CLIENT";
}

export interface ServerHandler extends Handler
{
    type: "SERVER",
    target: Target,
    contextId: string;
}

export class HandlerFactory
{
    public static onClick(handler: Handler)
    {
        if (handler && handler.type === "SERVER")
        {
            const serverHandler = (handler as ServerHandler);
            return (event: React.MouseEvent) =>
            {
                event.preventDefault();
                Backend.sendEvent(serverHandler.target, serverHandler.contextId);
            };
        }
        else
        {
            return () =>
            {
                throw "Handler type " + handler.type + " not supported";
            }
        }
    }
}
