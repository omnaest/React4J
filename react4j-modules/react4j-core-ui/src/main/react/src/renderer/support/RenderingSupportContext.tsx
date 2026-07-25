import React from "react";
import type { RenderingSupport } from "../Renderer";

/**
 * Ambient RenderingSupport for the current re-render region, provided by RerenderingContainer /
 * LocalRerenderingContainer and consumed by leaf dispatchers (Button, ToggleButton, Modal, Offcanvas,
 * SplitButton, DropdownItem, Pagination, ...) so that server-driven event handlers can reach
 * uiContextAccessor/nodeContextAccessor without every intermediate container (Composite, Card, ...)
 * having to forward it as a prop. Absent context (e.g. rendered outside any RerenderingContainer)
 * resolves to undefined, which HandlerFactory treats as a safe no-op.
 *
 * Deliberately kept in its OWN module (not in Renderer.tsx): Renderer.tsx imports every leaf
 * component (Button, ToggleButton, Modal, ...) at module top-level to build its type dispatch table,
 * so any RUNTIME value exported by Renderer.tsx and read back eagerly by one of those leaf
 * components (e.g. a class `static contextType = ...` field, which is evaluated at class-definition
 * time, i.e. at module-load time, not lazily at render time) hits a circular-import half-initialized
 * module and silently resolves to undefined. Only the RenderingSupport TYPE is imported back from
 * Renderer.tsx here, via `import type`, which is erased at compile time and creates no runtime edge.
 */
export const RenderingSupportContext = React.createContext<RenderingSupport | undefined>(undefined);
