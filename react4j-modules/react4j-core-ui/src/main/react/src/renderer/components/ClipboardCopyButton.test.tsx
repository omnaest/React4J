import React from "react";
import { render, fireEvent, screen, act } from "@testing-library/react";
import { ClipboardCopyButton, ClipboardCopyButtonNode } from "./ClipboardCopyButton";

function createNode(text: string, clearAfterDurationMillis: number): ClipboardCopyButtonNode {
    return {
        target: [],
        type: ClipboardCopyButton.TYPE,
        label: { DEFAULT: "Copy" },
        text: text,
        clearAfterDurationMillis: clearAfterDurationMillis
    };
}

describe("ClipboardCopyButton", () => {
    let writeTextMock: jest.Mock;

    beforeEach(() => {
        jest.useFakeTimers();
        writeTextMock = jest.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, "clipboard", {
            value: { writeText: writeTextMock },
            configurable: true
        });
    });

    afterEach(() => {
        jest.useRealTimers();
        // @ts-ignore - undo the test-only override so later test files see an unpolluted navigator
        delete (navigator as any).clipboard;
    });

    test("renders a button carrying the label only - never the copied text itself", async () => {
        const node = createNode("s3cr3t-value", 5000);
        render(<ClipboardCopyButton node={node} />);

        const button = await screen.findByRole("button");
        expect(button).toHaveTextContent("Copy");
        expect(button).not.toHaveTextContent("s3cr3t-value");
        expect(screen.queryByText("s3cr3t-value")).toBeNull();
    });

    test("clicking the button writes the node's text to the clipboard", async () => {
        const node = createNode("s3cr3t-value", 5000);
        render(<ClipboardCopyButton node={node} />);

        const button = await screen.findByRole("button");
        await act(async () => {
            fireEvent.click(button);
        });

        expect(writeTextMock).toHaveBeenNthCalledWith(1, "s3cr3t-value");
    });

    test("the clipboard is NOT cleared before the configured duration elapses", async () => {
        const node = createNode("s3cr3t-value", 5000);
        render(<ClipboardCopyButton node={node} />);

        const button = await screen.findByRole("button");
        await act(async () => {
            fireEvent.click(button);
        });

        await act(async () => {
            jest.advanceTimersByTime(4999);
            await Promise.resolve();
        });

        expect(writeTextMock).toHaveBeenCalledTimes(1);
        expect(writeTextMock).not.toHaveBeenCalledWith("");
    });

    test("the clipboard is overwritten with the empty string once the configured duration elapses", async () => {
        const node = createNode("s3cr3t-value", 5000);
        render(<ClipboardCopyButton node={node} />);

        const button = await screen.findByRole("button");
        await act(async () => {
            fireEvent.click(button);
        });

        await act(async () => {
            jest.advanceTimersByTime(5000);
            await Promise.resolve();
        });

        expect(writeTextMock).toHaveBeenCalledTimes(2);
        expect(writeTextMock).toHaveBeenNthCalledWith(2, "");
    });

    test("clicking when navigator.clipboard is unavailable (insecure context) does not throw", async () => {
        // @ts-ignore - simulate the insecure-context case where the browser never exposes navigator.clipboard
        delete (navigator as any).clipboard;

        const node = createNode("s3cr3t-value", 5000);
        render(<ClipboardCopyButton node={node} />);

        const button = await screen.findByRole("button");
        expect(() => fireEvent.click(button)).not.toThrow();
    });
});
