# react4j-core-ui

The browser client of React4J (`org.omnaest.react4j`): the TypeScript/React renderer
(`src/main/react/src/renderer`) and the `Backend`/`InFlightTracker` wrapper around Axios
(`src/main/react/src/backend`) that talks to `react4j-core`'s `/ui`, `/ui/event` and `/ui/upload*`
endpoints. Built as a CRA bundle and packaged into `react4j-core`'s static resources by the Maven
build.

## Build / test

From `src/main/react`:

```cmd
npm test    -- react-scripts test (Jest + React Testing Library)
npm run build -- react-scripts build (also the only real TypeScript compile/type-check in this module)
```

`npm test` in CI mode (`CI=true` env var, or `--watchAll=false`) runs once and exits; interactively it
watches. See the in-repo comment on `react4j-core-ui-jest-use-npm-test-and-run-build-for-tsc` (Jest
memory) for why automocking `Backend`/axios needs an explicit factory mock rather than
`jest.mock(path)` with no factory - axios's real ESM build is not transformable by CRA's default Jest
config.

## File upload: two client-side transports

`FileUpload.tsx` renders one `<input type=file>` regardless of which transport the server rendered for
this element - the element's `FileUploadFormNode.uploadUrl` and `.unbufferedTransport` (mirroring the
server's `FormFileUploadNode`, see `react4j-core-components/CLAUDE.md`) decide which `Backend` method
is called on file selection. See `react4j-core/CLAUDE.md` for the full server-side mechanism and
`react4j-core-components/CLAUDE.md` for the node/builder contract; this file records what the CLIENT
must get right, because both traps below are silent when missed - no compile error, no obvious runtime
error, just a corrupted or (worse) unprotected upload.

- **`unbufferedTransport` absent/false -> `Backend.uploadFile(...)`.** Unchanged: builds a `FormData`,
  posts `multipart/form-data` to `uploadUrl` (`ui/upload`).
- **`unbufferedTransport === true` -> `Backend.uploadFileRaw(...)`.** Posts the raw `File` itself as
  the request body - no `FormData`, no wrapper, no base64 - to `uploadUrl` (`ui/upload/raw`), with
  `X-Upload-Id` and `X-Filename` as headers and the file's own media type as `Content-Type`. Both
  methods increment/decrement the shared `InFlightTracker` around the request, in `finally`, so a
  failed round trip still settles the page-level busy indicator - preserve that if either method is
  ever touched again.

### Trap (a): `Content-Type` must never end up as a form content type

A `File.type` reported by the browser is routinely **empty** for an unrecognised extension. Left
unset, the HTTP client (or the server it talks to) is free to fall back to a default - and
`application/x-www-form-urlencoded` is exactly the one content type Tomcat parses into request
*parameters* even though it never runs its multipart resolver. Any filter or interceptor that reads a
request parameter would then consume the raw body before `FileUploadController#uploadFileRaw` ever
sees it, silently defeating the whole point of the unbuffered transport - the failure is invisible:
no exception, no log line, just an empty/corrupt upload. `Backend.uploadFileRaw` closes this by always
sending an explicit `Content-Type`: `file.type || "application/octet-stream"`, never left to a
default. Pinned by `Backend.uploadFileRaw.test.ts` (empty-`file.type` case).

### Trap (b): percent-encoding and `URLDecoder` disagree about `+`

`FileUploadController` decodes `X-Filename` with Java's `URLDecoder.decode(value, UTF_8)`, which turns
a **literal `+` into a space** - that is `application/x-www-form-urlencoded` decoding semantics, not
generic percent-decoding. `encodeURIComponent` is compatible: it escapes `+` to `%2B` and a space to
`%20`, so both round-trip correctly. **`encodeURI`, or sending the filename raw, is NOT compatible** -
either would leave a literal `+` in the header, and the server would silently turn it into a space,
corrupting any filename containing one. `Backend.uploadFileRaw` always encodes with
`encodeURIComponent(file.name)`. Pinned by `Backend.uploadFileRaw.test.ts` over two filenames: one
containing a space, one containing a literal `+`.
