import React from "react";
import ReactDOM from "react-dom";
import { JumboTron, JumbotronNode } from "./components/JumboTron";
import { UnorderedListNode, UnorderedList } from "./components/UnorderedList";
import { ImageNode, Image } from "./components/Image";
import { Button, ButtonNode } from "./components/Button";
import { ImageIndex, ImageIndexNode } from "./components/ImageIndex";
import { NavigationBar, NavigationBarNode } from "./components/NavigationBar";
import { Container, ContainerNode } from "./components/Container";
import { Row, RowNode } from "./components/Row";
import { Cell, CellNode } from "./components/Cell";
import { Heading, HeadingNode } from "./components/Heading";
import { BlockQuote, BlockQuoteNode } from "./components/Blockquote";
import { Card, CardNode } from "./components/Card";
import { Composite, CompositeNode } from "./components/Composite";
import { Paragraph, ParagraphNode } from "./components/Paragraph";
import { Table, TableNode } from "./components/Table";
import { TreeTable, TreeTableNode } from "./components/TreeTable";
import { Anker, AnkerNode } from "./components/Anker";
import { VerticalContentSwitcher, VerticalContentSwitcherNode } from "./components/VerticalContentSwitcher";
import { HomePage, HomePageNode } from "./components/HomePage";
import { Form, FormNode } from "./components/form/Form";
import { Text, TextNode } from "./components/Text";
import { ScrollbarContainerNode, ScrollbarContainer } from "./components/ScrollbarContainer";
import { AnkerButton, AnkerButtonNode } from "./components/AnkerButton";
import { LineBreakNode, LineBreak } from "./components/LineBreak";
import { Toaster, ToasterNode } from "./components/Toaster";
import { Icon, IconNode } from "./components/Icon";
import { PaddingContainer, PaddingContainerNode } from "./components/PaddingContainer";
import { TextAlignmentContainer, TextAlignmentContainerNode } from "./components/TextAlignmentContainer";
import RerenderingContainer, { RerenderingContainerNode } from "./components/RerenderingContainer";
import { UIContext, UIContextAccessor, UIContextDataNode } from "./data/DataContextManager";
import { ProgressBar, ProgressBarNode } from "./components/ProgressBar";
import { IntervalRerenderingContainer, IntervalRerenderingContainerNode } from "./components/IntervalRerenderingContainer";
import { RatioContainer, RatioContainerNode } from "./components/RatioContainer";
import { IFrameContainer, IFrameContainerNode } from "./components/IFrameContainer";
import { SizedContainer, SizedContainerNode } from "./components/SizedContainer";
import { Range, RangeNode } from "./components/Range";
import { NativeHtml, NativeHtmlNode } from "./components/NativeHtml";
import { RerenderingHelper } from "./support/RerenderingHelper";
import { RenderingSupportHelper } from "./support/RenderingSupportHelper";
import { ListView, ListViewNode } from "./components/ListView";
import { ListViewElement, ListViewElementNode } from "./components/ListViewElement";
import { Badge, BadgeNode } from "./components/Badge";
import { Spinner, SpinnerNode } from "./components/Spinner";
import { Placeholder, PlaceholderNode } from "./components/Placeholder";
import { Alert, AlertNode } from "./components/Alert";
import { Breadcrumb, BreadcrumbNode } from "./components/Breadcrumb";
import { Pagination, PaginationNode } from "./components/Pagination";
import { Stack, StackNode } from "./components/Stack";
import { Figure, FigureNode } from "./components/Figure";
import { Tabs, TabsNode } from "./components/Tabs";
import { Accordion, AccordionNode } from "./components/Accordion";
import { Modal, ModalNode } from "./components/Modal";
import { Offcanvas, OffcanvasNode } from "./components/Offcanvas";
import { Tooltip, TooltipNode } from "./components/Tooltip";
import { Popover, PopoverNode } from "./components/Popover";
import { Collapse, CollapseNode } from "./components/Collapse";
import { ToggleButton, ToggleButtonNode } from "./components/ToggleButton";
import { Dropdown, DropdownNode } from "./components/Dropdown";
import { SplitButton, SplitButtonNode } from "./components/SplitButton";
import { Carousel, CarouselNode } from "./components/Carousel";

export interface Node {
    target: Target;
    uiContextIds?: string[];
    uiContextData?: UIContextDataNode;
    type: string;
}

export interface Target extends Array<string> {
}

export interface RenderingSupport {
    uiContextAccessor: UIContextAccessor,
    nodeContextAccessor: NodeContextAccessor
}

export interface NodeContextAccessor {
    updateNode(node: Node): void;
}

/**
 * C1 testability hook (plan-74 Goal 3a): a stable, server-derived DOM selector.
 *
 * A transparent, non-DOM-emitting wrapper (its own render() returns the child element
 * unmodified, so it never introduces a wrapper <div>/layout node) that, once its subtree is
 * mounted/updated, locates the child's real underlying host DOM element and stamps a
 * `data-location` attribute onto it — the node's `target` Location path joined with ".".
 * This is the ONE generalization point: every node type dispatched by Renderer.renderNode is
 * wrapped here, so no per-component edits are needed even when a node type renders through a
 * sub-component (e.g. react-bootstrap) that itself performs no prop spreading.
 */
interface LocationAttributeProps {
    target: Target;
    children: JSX.Element;
}

class LocationAttribute extends React.Component<LocationAttributeProps, {}> {
    private applyDataLocationAttribute(): void {
        // eslint-disable-next-line react/no-find-dom-node -- deliberate: the only generic way to
        // reach the actual rendered host element of an arbitrary, already-built child subtree
        // without adding a wrapper DOM node or touching every component (see class doc above).
        const domNode = ReactDOM.findDOMNode(this);
        if (domNode instanceof Element) {
            domNode.setAttribute("data-location", (this.props.target || []).join("."));
        }
    }

    public componentDidMount(): void {
        this.applyDataLocationAttribute();
    }

    public componentDidUpdate(): void {
        this.applyDataLocationAttribute();
    }

    public render(): JSX.Element {
        return this.props.children;
    }
}

export class Renderer {
    public static render(node: Node, renderingSupport?: RenderingSupport): JSX.Element {
        if (node?.uiContextData?.contextId) {
            return RerenderingHelper.wrapIntoRerenderingContainer([node.uiContextData.contextId],
                (renderingSupport) => {
                    renderingSupport?.uiContextAccessor.initializeUIContext(node.uiContextData);
                    return this.renderWithRelatedUIContexts(node, renderingSupport);
                });
        }
        else {
            return this.renderWithRelatedUIContexts(node, renderingSupport);
        }
    }

    public static renderWithRelatedUIContexts(node: Node, renderingSupport?: RenderingSupport): JSX.Element {
        if ((node?.uiContextIds?.length || 0) > 0) {
            return RerenderingHelper.wrapIntoRerenderingContainer(node.uiContextIds,
                (renderingSupport) => {
                    return this.renderNode(node, renderingSupport);
                });
        } else if (node) {
            return this.renderNode(node, renderingSupport);
        }
        else {
            return <></>;
        }
    }

    /**
     * The single render-dispatch point for every server node (C1 emission point): resolves the
     * node-type-specific element via renderNodeElement, then wraps it with LocationAttribute so
     * every rendered node systematically carries a `data-location` DOM attribute.
     */
    public static renderNode(node: Node, renderingSupport?: RenderingSupport): JSX.Element {
        const element = this.renderNodeElement(node, renderingSupport);
        if (node && node.target) {
            return <LocationAttribute target={node.target}>{element}</LocationAttribute>;
        }
        return element;
    }

    private static renderNodeElement(node: Node, renderingSupport?: RenderingSupport): JSX.Element {
        if (node) {
            if (node.type === "JUMBOTRON") {
                return <JumboTron node={node as JumbotronNode} />;
            }
            else if (node.type === RerenderingContainer.TYPE) {
                return <RerenderingContainer node={node as RerenderingContainerNode} />
            }
            else if (node.type === IntervalRerenderingContainer.TYPE) {
                return <IntervalRerenderingContainer node={node as IntervalRerenderingContainerNode} />
            }
            else if (node.type === UnorderedList.TYPE) {
                return <UnorderedList node={node as UnorderedListNode} />
            }
            else if (node.type === Image.TYPE) {
                return <Image node={node as ImageNode} />;
            }
            else if (node.type === Icon.TYPE) {
                return <Icon node={node as IconNode} />;
            }
            else if (node.type === Button.TYPE) {
                return <Button node={node as ButtonNode} />;
            }
            else if (node.type === ImageIndex.TYPE) {
                return <ImageIndex
                    node={node as ImageIndexNode}
                    render={node => this.render(node)}
                />
            }
            else if (node.type === PaddingContainer.TYPE) {
                return <PaddingContainer node={node as PaddingContainerNode} />;
            }
            else if (node.type === NavigationBar.TYPE) {
                return <NavigationBar node={node as NavigationBarNode} />
            }
            else if (node.type === Container.TYPE) {
                return <Container node={node as ContainerNode} />
            }
            else if (node.type === Row.TYPE) {
                return <Row node={node as RowNode} />
            }
            else if (node.type === Cell.TYPE) {
                return <Cell node={node as CellNode} />
            }
            else if (node.type === Heading.TYPE) {
                return <Heading node={node as HeadingNode} />
            }
            else if (node.type === BlockQuote.TYPE) {
                return <BlockQuote node={node as BlockQuoteNode} />
            }
            else if (node.type === Card.TYPE) {
                return <Card node={node as CardNode} />
            }
            else if (node.type === Composite.TYPE) {
                return <Composite node={node as CompositeNode} />
            }
            else if (node.type === Paragraph.TYPE) {
                return <Paragraph node={node as ParagraphNode} />
            }
            else if (node.type === Table.TYPE) {
                return <Table node={node as TableNode} />
            }
            else if (node.type === TreeTable.TYPE) {
                return <TreeTable node={node as TreeTableNode} />
            }
            else if (node.type === Anker.TYPE) {
                return <Anker node={node as AnkerNode} />
            }
            else if (node.type === AnkerButton.TYPE) {
                return <AnkerButton node={node as AnkerButtonNode} />
            }
            else if (node.type === LineBreak.TYPE) {
                return <LineBreak node={node as LineBreakNode} />
            }
            else if (node.type === VerticalContentSwitcher.TYPE) {
                return <VerticalContentSwitcher node={node as VerticalContentSwitcherNode} />
            }
            else if (node.type === HomePage.TYPE) {
                return <HomePage node={node as HomePageNode} />
            }
            else if (node.type === Form.TYPE) {
                return <Form node={node as FormNode} renderingSupport={renderingSupport} />
            }
            else if (node.type === ScrollbarContainer.TYPE) {
                return <ScrollbarContainer node={node as ScrollbarContainerNode} />
            }
            else if (node.type === Text.TYPE) {
                return <Text node={node as TextNode} />
            }
            else if (node.type === TextAlignmentContainer.TYPE) {
                return <TextAlignmentContainer node={node as TextAlignmentContainerNode} />
            }
            else if (node.type === Toaster.TYPE) {
                return <Toaster node={node as ToasterNode} />
            }
            else if (node.type === ProgressBar.TYPE) {
                return <ProgressBar node={node as ProgressBarNode} />
            }
            else if (node.type === RatioContainer.TYPE) {
                return <RatioContainer node={node as RatioContainerNode} />
            }
            else if (node.type === IFrameContainer.TYPE) {
                return <IFrameContainer node={node as IFrameContainerNode} />
            }
            else if (node.type === SizedContainer.TYPE) {
                return <SizedContainer node={node as SizedContainerNode} />
            }
            else if (node.type === Range.TYPE) {
                return <Range node={node as RangeNode} />
            }
            else if (node.type === NativeHtml.TYPE) {
                return <NativeHtml node={node as NativeHtmlNode} />
            }
            else if (node.type === ListView.TYPE) {
                return <ListView node={node as ListViewNode} />
            }
            else if (node.type === ListViewElement.TYPE) {
                return <ListViewElement node={node as ListViewElementNode} />
            }
            else if (node.type === Badge.TYPE) {
                return <Badge node={node as BadgeNode} />
            }
            else if (node.type === Spinner.TYPE) {
                return <Spinner node={node as SpinnerNode} />
            }
            else if (node.type === Placeholder.TYPE) {
                return <Placeholder node={node as PlaceholderNode} />
            }
            else if (node.type === Alert.TYPE) {
                return <Alert node={node as AlertNode} />
            }
            else if (node.type === Breadcrumb.TYPE) {
                return <Breadcrumb node={node as BreadcrumbNode} />
            }
            else if (node.type === Pagination.TYPE) {
                return <Pagination node={node as PaginationNode} />
            }
            else if (node.type === Stack.TYPE) {
                return <Stack node={node as StackNode} />
            }
            else if (node.type === Figure.TYPE) {
                return <Figure node={node as FigureNode} />
            }
            else if (node.type === Tabs.TYPE) {
                return <Tabs node={node as TabsNode} />
            }
            else if (node.type === Accordion.TYPE) {
                return <Accordion node={node as AccordionNode} />
            }
            else if (node.type === Modal.TYPE) {
                return <Modal node={node as ModalNode} />
            }
            else if (node.type === Offcanvas.TYPE) {
                return <Offcanvas node={node as OffcanvasNode} />
            }
            else if (node.type === Tooltip.TYPE) {
                return <Tooltip node={node as TooltipNode} />
            }
            else if (node.type === Popover.TYPE) {
                return <Popover node={node as PopoverNode} />
            }
            else if (node.type === Collapse.TYPE) {
                return <Collapse node={node as CollapseNode} />
            }
            else if (node.type === ToggleButton.TYPE) {
                return <ToggleButton node={node as ToggleButtonNode} />
            }
            else if (node.type === Dropdown.TYPE) {
                return <Dropdown node={node as DropdownNode} />
            }
            else if (node.type === SplitButton.TYPE) {
                return <SplitButton node={node as SplitButtonNode} />
            }
            else if (node.type === Carousel.TYPE) {
                return <Carousel node={node as CarouselNode} />
            }
            else {
                console.log("Invalid node " + node.type);
                console.log(node);
            }
        }
        return <></>;
    }
}

