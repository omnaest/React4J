import React from "react";
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

    public static renderNode(node: Node, renderingSupport?: RenderingSupport): JSX.Element {
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
            else {
                console.log("Invalid node " + node.type);
                console.log(node);
            }
        }
        return <></>;
    }
}

