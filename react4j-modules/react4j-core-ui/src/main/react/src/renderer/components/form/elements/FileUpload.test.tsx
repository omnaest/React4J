import React from "react";
import { render, fireEvent, screen } from "@testing-library/react";
import { FileUpload, FileUploadFormElement } from "./FileUpload";
import { Backend } from "../../../../backend/Backend";
import { UIContext, UIContextAccessor, UIContextDataNode } from "../../../data/DataContextManager";
import { NodeContextAccessor } from "../../../Renderer";

// Explicit factory mock (no automock): automocking would require Jest to load the real Backend module,
// which transitively pulls in axios's ESM-only build; CRA's default transformIgnorePatterns does not
// transform node_modules, so that path fails with "Cannot use import statement outside a module".
// A factory mock never evaluates the real module, so it sidesteps that pre-existing jest-config gap.
jest.mock("../../../../backend/Backend", () => ({
    Backend: {
        uploadFile: jest.fn(),
        sendEvent: jest.fn(),
        getUI: jest.fn(),
        getUISubNode: jest.fn(),
        fetchData: jest.fn()
    },
    BackendUri: {
        URI_UI: "ui",
        URI_UI_HANDLER: "ui/event",
        URI_UI_DATA_SOURCE: "ui/data/query",
        URI_UPLOAD: "ui/upload",
        resolve: jest.fn((uri: string) => uri)
    }
}));

const mockedUploadFile = Backend.uploadFile as jest.MockedFunction<typeof Backend.uploadFile>;
const mockedSendEvent = Backend.sendEvent as jest.MockedFunction<typeof Backend.sendEvent>;

function createUIContextAccessor(): UIContextAccessor {
    const contexts: { [contextId: string]: UIContext } = {
        ctx1: { contextId: "ctx1", data: {}, internalData: {}, updateCounter: 0 }
    };
    return {
        getUIContextById: (contextId: string) => contexts[contextId],
        getAllUIContexts: () => Object.values(contexts),
        updateUIContext: (uiContext: UIContext) => {
            contexts[uiContext.contextId] = uiContext;
        },
        initializeUIContext: (uiContext?: UIContextDataNode) => {
            if (uiContext) {
                contexts[uiContext.contextId] = { ...uiContext, updateCounter: 0 };
            }
        }
    };
}

function createElement(): FileUploadFormElement {
    return {
        field: "avatar",
        contextId: "ctx1",
        type: FileUpload.TYPE,
        label: { DEFAULT: "Avatar" },
        description: { DEFAULT: "" },
        disabled: false,
        readonly: false,
        required: false,
        fileUpload: {
            uploadUrl: "ui/upload",
            uploadId: "upload-123",
            accept: "image/*",
            maxSize: 10485760,
            onComplete: {
                type: "SERVER",
                target: ["form", "avatar"],
                contextId: "ctx1"
            }
        }
    };
}

beforeEach(() => {
    jest.clearAllMocks();
});

test("renders a file input honoring the accept hint", () => {
    const element = createElement();
    const uiContextAccessor = createUIContextAccessor();

    render(
        <FileUpload
            id="avatar"
            element={element}
            onUpdate={jest.fn()}
            updateCounter={0}
            renderingSupport={{ uiContextAccessor, nodeContextAccessor: {} as NodeContextAccessor }}
        />
    );

    const input = document.getElementById("avatar") as HTMLInputElement;
    expect(input).toBeInTheDocument();
    expect(input.type).toBe("file");
    expect(input.accept).toBe("image/*");
});

test("uploads the selected file then writes the field and fires onComplete", async () => {
    const element = createElement();
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    mockedUploadFile.mockResolvedValue({
        uploadId: "upload-123",
        filename: "photo.png",
        size: 42,
        contentType: "image/png"
    });

    render(
        <FileUpload
            id="avatar"
            element={element}
            onUpdate={jest.fn()}
            updateCounter={0}
            renderingSupport={{ uiContextAccessor, nodeContextAccessor }}
        />
    );

    const input = document.getElementById("avatar") as HTMLInputElement;
    const file = new File(["hello"], "photo.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });

    expect(mockedUploadFile).toHaveBeenCalledWith("ui/upload", "upload-123", file);

    // findByText polls the DOM until the async upload-then-notify chain (promise resolution + setState)
    // has flushed, without relying on the top-level `waitFor` helper (unavailable in this dependency tree —
    // see the note above the jest.mock call).
    expect(await screen.findByText(/Uploaded: photo.png/)).toBeInTheDocument();

    expect(mockedSendEvent).toHaveBeenCalledWith(
        ["form", "avatar"],
        "ctx1",
        uiContextAccessor,
        nodeContextAccessor
    );
    expect(uiContextAccessor.getUIContextById("ctx1").data["avatar"]).toBe("photo.png");
});

test("shows an error and does not fire onComplete when the upload fails", async () => {
    const element = createElement();
    const uiContextAccessor = createUIContextAccessor();
    const nodeContextAccessor = {} as NodeContextAccessor;

    mockedUploadFile.mockRejectedValue(new Error("network error"));

    render(
        <FileUpload
            id="avatar"
            element={element}
            onUpdate={jest.fn()}
            updateCounter={0}
            renderingSupport={{ uiContextAccessor, nodeContextAccessor }}
        />
    );

    const input = document.getElementById("avatar") as HTMLInputElement;
    const file = new File(["hello"], "photo.png", { type: "image/png" });
    fireEvent.change(input, { target: { files: [file] } });

    expect(await screen.findByRole("alert")).toBeInTheDocument();
    expect(mockedSendEvent).not.toHaveBeenCalled();
    expect(uiContextAccessor.getUIContextById("ctx1").data["avatar"]).toBeUndefined();
});
