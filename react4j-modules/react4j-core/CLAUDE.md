# react4j-core

The runtime service layer of React4J (`org.omnaest.react4j`): the `/ui` and `/ui/event` endpoints, the
`ReactUIService` rendering pipeline, and the file-upload endpoints (`FileUploadController`,
`FileUploadService`). Depends on `react4j-core-components` (the component/UploadChannel model) and
`react4j-core-ui` (the bundled frontend).

## Build

```cmd
mvn clean install
```

No embedded servlet container is on this module's classpath (it parents `CommonsParent`, not
`CommonsSpringBootParent`) - it is a library, consumed by Spring Boot apps that bring their own
`spring-boot-starter-web`. `@SpringBootTest(webEnvironment = MOCK)` works here (`FileUploadEndToEndTest`);
`webEnvironment = RANDOM_PORT` does not, for want of a real embedded container - see "Real-container upload
tests live in `react4j-ui-test`" below before adding one.

## File upload: two transports

`FileUploadController` exposes two upload endpoints. Both funnel through the same
`FileUploadService`/`UploadChannelRegistry` path into the application's `UploadChannel`
(`react4j-core-components`), so a consuming app writes one `UploadChannel` implementation regardless of
which transport delivers the bytes.

### `/ui/upload` (multipart, the default) - buffers a plaintext temp file per part

This is what every existing consumer uses today, unchanged. **It is important to understand what it
actually does before reaching for it in a confidentiality-sensitive application**: Spring's
`StandardServletMultipartResolver` detects the `multipart/*` content type and hands the request to the
servlet container's own multipart parser *before* `FileUploadController` ever runs.
`FileUploadConfiguration`'s `MultipartConfigElement` leaves `fileSizeThreshold` at Tomcat's default of `0`,
so **every part of every multipart upload - including a plain form field, not only the file part - is
written to a plaintext temporary file on disk before `UploadChannel.consume(...)` is invoked**, and deleted
only at the end of the request. Measured (plan-154 Spike S3-alpha): the spill file appears at
`<java.io.tmpdir>\tomcat.<port>.<random>\work\Tomcat\localhost\ROOT\upload_<uuid>_<seq>.tmp`, its SHA-256
equals the payload's, and it survives for the whole request. `spring.servlet.multipart.*` size properties
are inert in any React4J app, because `FileUploadConfiguration` already supplies the only
`MultipartConfigElement` and Spring Boot's `MultipartAutoConfiguration` backs off (`@ConditionalOnMissingBean`)
once that bean exists - the operative size knobs are `react4j.upload.max-file-size` /
`react4j.upload.max-request-size` (default 25 MB each).

**So: an encrypting (or otherwise confidentiality-sensitive) `UploadChannel` sitting downstream of this
transport does not, by itself, mean "no plaintext copy of the upload exists anywhere" - the container already
made one, one layer below the channel.** This is a property of the transport, not something an
`UploadChannel` implementation can fix from inside `consume(...)`.

### `/ui/upload/raw` (opt-in, unbuffered) - no temp file, no size-proportional heap buffer

An element opts in via `Form.FileUploadFormElement#withUnbufferedTransport()`; an element that does not is
completely unaffected - same multipart transport, same behaviour, same defaults, as today. Reach for it when
a channel's contract genuinely requires that no plaintext copy of the upload ever touches disk (e.g. an
encrypting sink), or when uploads are large enough that the multipart transport's disk round trip is a real
cost.

**Mechanism.** A request whose `Content-Type` is not `multipart/*` is never wrapped by Spring's multipart
resolver at all - `DispatcherServlet` never calls into `MultipartResolver`, so nothing parses or spills it.
`FileUploadController#uploadFileRaw` reads `HttpServletRequest.getInputStream()` directly and never calls
`getPart`/`getParts`/`getParameter` or anything else that would trigger container-side multipart parsing.
Measured (plan-154 Spike S3-alpha): 500 MB round-tripped with **zero** temp files and a flat **~27-30 MB**
peak heap under a 256 MB heap cap; 200 MB passed under a 96 MB cap.

**Wire shape.** The uploadId travels as the `X-Upload-Id` request header - deliberately a header, not a
query parameter, so it is not the kind of thing a default access log records. The client filename travels
as the `X-Filename` header, **percent-encoded (UTF-8, `URLEncoder`/`URLDecoder`)** - a raw HTTP header value
is not UTF-8 by default, so an un-encoded non-ASCII filename would be mangled; percent-encoding keeps the
header value itself pure ASCII regardless of how the container handles header charsets. The declared content
type travels on the request's own `Content-Type` header - any value that does not start with `multipart/` is
safe on this transport by construction, so no separate header is needed for it.

**The global size ceiling still applies, and is enforced by the controller itself.**
`FileUploadConfiguration`'s `MultipartConfigElement` is the coarse global ceiling on the multipart transport,
enforced by the container's own multipart parsing - which never runs on this transport, so that enforcement
would otherwise silently vanish. `FileUploadController#uploadFileRaw` reads the same
`react4j.upload.max-file-size` / `react4j.upload.max-request-size` properties, rejects a request whose
declared `Content-Length` already exceeds the ceiling before touching the stream, and additionally wraps the
request body in a `BoundedInputStream` at that ceiling so an unknown-length (chunked) body is bounded too -
both map to the same `413` the multipart transport produces. A channel implementing `UploadChannel` directly
(not via `AbstractUploadChannel`) still gets this global bound automatically, because it is baked into the
`InputStream` the channel reads, not layered on top by the channel.

**Real-container upload tests live in `react4j-ui-test`, not here.** Proving "no temp file" needs a real
embedded servlet container (`@SpringBootTest(webEnvironment = RANDOM_PORT)`); `MockMvc` runs no container and
so cannot produce, or fail to produce, a spill - a green result there would be vacuous. This module has no
embedded container on its classpath (see "Build" above), and none should be added here just for a test -
`react4j-ui-test` already carries one transitively via `CommonsSpringBootParent`, so
`UnbufferedUploadTransportEndToEndTest`/`UnbufferedUploadTransportGlobalCeilingTest` live there instead.

### A related finding, not used by this transport but worth knowing

`tomcat-embed-core` - already on every Spring Boot 3 web app's classpath, React4J's included - ships a
complete **streaming** multipart parser at `org.apache.tomcat.util.http.fileupload.*`, with a public
`getItemIterator`, that never spills to disk either. It keeps the multipart wire format (unlike the raw
transport above) at the cost of a hand-rolled parsing loop. Anyone who later needs a non-buffering transport
that speaks multipart needs no new dependency for it. Two things worth recording alongside it, so nobody
re-derives them: `commons-fileupload2-jakarta-servlet` **does not exist** on Maven Central (the Servlet-6
artifact is `commons-fileupload2-jakarta-servlet6`, and its newest release is `2.0.0-M5`, a milestone with no
GA); and `spring.servlet.multipart.resolve-lazily=true` does make a controller reading the raw body possible
even though React4J supplies the `MultipartConfigElement`, but its measured failure mode with the flag left
at its default (`false`) is silent: **HTTP 200 with an empty item list, while still writing plaintext spill
files** - which is why it is not the mechanism `/ui/upload/raw` uses.

## `UploadChannelRegistry` is bounded, not just idempotent

`UploadChannelRegistryImpl` (`service.internal.upload`) is self-bounding: an entry not registered or looked
up for longer than a configured time-to-live is removed from both its internal maps (not merely hidden from
`lookup`), and the total entry count is additionally capped with least-recently-used eviction. Both bounds
are independent - a time-to-live bounds growth per unit time, a capacity cap bounds a burst inside one
window - and neither substitutes for the other. Configurable via `react4j.upload.registry.time-to-live-minutes`
(default `30`) and `react4j.upload.registry.max-entries` (default `1000`). Do not reach for Commons
`DurationLimitedCache` here: it is a lazy read-side expiry gate that never removes entries, so it fixes the
symptom and leaves the defect.

The impl has two public constructors: `UploadChannelRegistryImpl(long timeToLiveMinutes, int maxEntries)`,
annotated `@Autowired` and the one Spring actually wires, which supplies `Clock.systemUTC()` internally; and
`UploadChannelRegistryImpl(Clock clock, long timeToLiveMinutes, int maxEntries)`, used only by tests
(`UploadChannelRegistryImplTest`, `FileUploadServiceImplTest`, `FileUploadControllerTest`) to advance time
deterministically without sleeping. React4J publishes no `Clock` bean - a `@ConditionalOnMissingBean(Clock.class)`
bean was tried and reverted: it widened this library's published Spring surface for all eleven consuming
applications (plan-156 cliff X8), and - independent of that - the "always wins" guarantee it advertised did
not hold, because `@ConditionalOnMissingBean` is only order-safe on auto-configuration classes processed after
user configuration, and `FileUploadConfiguration` is a plain `@Configuration` class with no defined ordering
relative to an embedding app's own `@Configuration` classes; an app that declares its own `Clock` bean could
have collided with this one at startup (`BeanDefinitionOverrideException`) depending on registration order. A
periodic `@Scheduled` sweep (piggybacking on
`ReactUIAutoConfiguration`'s existing `@EnableScheduling`) plus an opportunistic sweep on every `register`/
`lookup` call bound staleness even if nothing else happens. `UploadChannelRegistry` the interface is
unchanged - eviction is entirely internal to the impl.
