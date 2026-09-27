// Mock axios itself (not the Backend module) via an explicit factory -- same rationale as
// Backend.inFlight.test.ts: this lets the REAL Backend.ts (and its real InFlightTracker wiring) load
// and run under Jest while swapping out only axios's network calls.
jest.mock("axios", () => ({
    __esModule: true,
    default: {
        post: jest.fn(),
        get: jest.fn()
    }
}));

import Axios from "axios";
import { Backend } from "./Backend";
import { InFlightTracker } from "./InFlightTracker";

const mockedPost = Axios.post as jest.MockedFunction<typeof Axios.post>;

beforeEach(() => {
    jest.clearAllMocks();
    InFlightTracker.resetForTests();
});

/**
 * AC-1 / AC-R7: the raw-body transport posts the FILE ITSELF as the body (no FormData, no wrapper)
 * to the rendered uploadUrl, with X-Upload-Id and a non-form Content-Type carried as headers.
 */
test("uploadFileRaw posts the raw file body with X-Upload-Id and the file's own Content-Type", async () => {
    mockedPost.mockResolvedValue({
        data: { uploadId: "upload-1", filename: "photo.png", size: 5, contentType: "image/png" }
    });

    const file = new File(["hello"], "photo.png", { type: "image/png" });
    await Backend.uploadFileRaw("ui/upload/raw", "upload-1", file);

    expect(mockedPost).toHaveBeenCalledTimes(1);
    const [url, data, config] = mockedPost.mock.calls[0];
    expect(url).toBe("ui/upload/raw");
    // The raw file itself is the body -- not a FormData wrapper.
    expect(data).toBe(file);
    expect(data instanceof FormData).toBe(false);
    expect(config?.headers).toMatchObject({
        "X-Upload-Id": "upload-1",
        "Content-Type": "image/png"
    });
});

/**
 * Trap (a), S3-AC-R8/AC-2: Content-Type must never be multipart/form-data, and -- the dangerous case
 * -- an empty File.type (routine for an unrecognised extension) must fall back to
 * application/octet-stream explicitly, never be left for the HTTP client to default to
 * application/x-www-form-urlencoded (the one content type Tomcat parses into request parameters,
 * silently consuming the body before the handler runs).
 */
test("uploadFileRaw falls back to application/octet-stream when file.type is empty, never a form content type", async () => {
    mockedPost.mockResolvedValue({
        data: { uploadId: "upload-2", filename: "data.bin", size: 3, contentType: "application/octet-stream" }
    });

    // An unrecognised extension routinely reports an empty type -- the trap this test pins.
    const file = new File(["abc"], "data.bin", { type: "" });
    await Backend.uploadFileRaw("ui/upload/raw", "upload-2", file);

    const [, , config] = mockedPost.mock.calls[0];
    expect(config?.headers?.["Content-Type"]).toBe("application/octet-stream");
    expect(config?.headers?.["Content-Type"]).not.toBe("multipart/form-data");
    expect(config?.headers?.["Content-Type"]).not.toBe("application/x-www-form-urlencoded");
});

/**
 * Trap (b), S3-AC-R8/AC-3, first filename: a space. The server decodes X-Filename with Java's
 * URLDecoder.decode(value, UTF_8), which is compatible with encodeURIComponent (space -> %20).
 */
test("uploadFileRaw percent-encodes a filename containing a space via encodeURIComponent", async () => {
    mockedPost.mockResolvedValue({
        data: { uploadId: "upload-3", filename: "my file.txt", size: 3, contentType: "text/plain" }
    });

    const file = new File(["abc"], "my file.txt", { type: "text/plain" });
    await Backend.uploadFileRaw("ui/upload/raw", "upload-3", file);

    const [, , config] = mockedPost.mock.calls[0];
    const encodedFilename = config?.headers?.["X-Filename"];
    expect(encodedFilename).toBe(encodeURIComponent("my file.txt"));
    expect(encodedFilename).toBe("my%20file.txt");
    // URLDecoder.decode(value, UTF_8) semantics, pinned client-side: %20 -> space (never left literal).
    expect(decodeURIComponent(encodedFilename as string)).toBe("my file.txt");
});

/**
 * Trap (b), S3-AC-R8/AC-3, second filename: a literal '+'. Java's URLDecoder.decode turns a literal
 * '+' into a space, so the client MUST escape it to %2B (encodeURIComponent does; encodeURI and a raw
 * header value do not) or the filename would silently arrive at the server with a space in place of
 * the '+'.
 */
test("uploadFileRaw percent-encodes a literal '+' in a filename as %2B, not left as a raw '+'", async () => {
    mockedPost.mockResolvedValue({
        data: { uploadId: "upload-4", filename: "a+b.txt", size: 3, contentType: "text/plain" }
    });

    const file = new File(["abc"], "a+b.txt", { type: "text/plain" });
    await Backend.uploadFileRaw("ui/upload/raw", "upload-4", file);

    const [, , config] = mockedPost.mock.calls[0];
    const encodedFilename = config?.headers?.["X-Filename"] as string;
    expect(encodedFilename).toBe(encodeURIComponent("a+b.txt"));
    expect(encodedFilename).toBe("a%2Bb.txt");
    // encodeURI would leave '+' unescaped -- explicitly pin that this is NOT what happened.
    expect(encodedFilename).not.toContain("+");
    // Simulates the server's URLDecoder.decode(value, UTF_8): must recover the literal '+', not a space.
    expect(decodeURIComponent(encodedFilename)).toBe("a+b.txt");
});
