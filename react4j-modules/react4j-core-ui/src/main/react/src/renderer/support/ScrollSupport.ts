/**
 * Finding the element that actually scrolls, and scrolling it.
 *
 * WHY THIS IS NOT ONE LINE. The obvious implementation - set scrollTop on the element you are holding - silently
 * does nothing whenever that element is not itself the scrolling one, which in a nested layout is most of the
 * time. React4J hit exactly that: a scroll-to-bottom that had never worked, failing quietly because assigning
 * scrollTop to a non-scrolling element is legal and simply has no effect. Walking up to the nearest ancestor that
 * genuinely scrolls is what makes it work.
 *
 * EXTRACTED because a second component needed it. It began as a private method of ScrollbarContainer; a pending
 * block that appears without a server round trip has to scroll itself into view, and a second copy of a walk
 * whose whole purpose is to avoid a silent no-op is a second chance to get it wrong.
 */
export class ScrollSupport {
    /**
     * The nearest ancestor of {@code start} - itself included - that both declares a scrolling overflow and has
     * content taller than its box.
     *
     * Both conditions matter: a container can declare {@code overflow-y: auto} and not be scrolling because its
     * content fits, in which case it is not the element a caller wants either.
     */
    public static findScrollableAncestor(start: HTMLElement | null): HTMLElement | null {
        let candidate: HTMLElement | null = start;
        while (candidate) {
            const overflowY = window.getComputedStyle(candidate).overflowY;
            const scrolls = overflowY === "auto" || overflowY === "scroll";
            if (scrolls && candidate.scrollHeight > candidate.clientHeight) {
                return candidate;
            }
            candidate = candidate.parentElement;
        }
        return null;
    }

    /**
     * Scrolls whatever actually scrolls around {@code start} to its bottom. A no-op when nothing does, which is
     * the correct outcome rather than an error - a short transcript that fits on screen has no bottom to reach.
     */
    public static scrollToBottom(start: HTMLElement | null): void {
        const scrollable = ScrollSupport.findScrollableAncestor(start);
        if (scrollable) {
            scrollable.scrollTop = scrollable.scrollHeight;
        }
    }
}
