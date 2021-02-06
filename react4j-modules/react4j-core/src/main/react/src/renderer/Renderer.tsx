import React from "react";
import { JumboTron, JumbotronNode } from "./components/JumboTron";
import { ListNode, List } from "./components/List";
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
import { Form, FormNode } from "./components/Form";
import { Text, TextNode } from "./components/Text";
import { ScrollbarContainerNode, ScrollbarContainer } from "./components/ScrollbarContainer";
import { AnkerButton, AnkerButtonNode } from "./components/AnkerButton";

export interface Node
{
    type: string;
}

export class Renderer
{

    public static render(node: Node): JSX.Element
    {
        if (node)
        {
            if (node.type === "JUMBOTRON")
            {
                return <JumboTron
                    node={node as JumbotronNode}
                    render={node => this.render(node)}
                />;
            }
            else if (node.type === "LIST")
            {
                return <List node={node as ListNode} />
            }
            else if (node.type === Image.TYPE)
            {
                return <Image node={node as ImageNode} />;
            }
            else if (node.type === Button.TYPE)
            {
                return <Button node={node as ButtonNode} />;
            }
            else if (node.type === ImageIndex.TYPE)
            {
                return <ImageIndex
                    node={node as ImageIndexNode}
                    render={node => this.render(node)}
                />
            }
            else if (node.type === NavigationBar.TYPE)
            {
                return <NavigationBar node={node as NavigationBarNode} />
            }
            else if (node.type === Container.TYPE)
            {
                return <Container node={node as ContainerNode} />
            }
            else if (node.type === Row.TYPE)
            {
                return <Row node={node as RowNode} />
            }
            else if (node.type === Cell.TYPE)
            {
                return <Cell node={node as CellNode} />
            }
            else if (node.type === Heading.TYPE)
            {
                return <Heading node={node as HeadingNode} />
            }
            else if (node.type === BlockQuote.TYPE)
            {
                return <BlockQuote node={node as BlockQuoteNode} />
            }
            else if (node.type === Card.TYPE)
            {
                return <Card node={node as CardNode} />
            }
            else if (node.type === Composite.TYPE)
            {
                return <Composite node={node as CompositeNode} />
            }
            else if (node.type === Paragraph.TYPE)
            {
                return <Paragraph node={node as ParagraphNode} />
            }
            else if (node.type === Table.TYPE)
            {
                return <Table node={node as TableNode} />
            }
            else if (node.type === Anker.TYPE)
            {
                return <Anker node={node as AnkerNode} />
            }
            else if (node.type === AnkerButton.TYPE)
            {
                return <AnkerButton node={node as AnkerButtonNode} />
            }
            else if (node.type === VerticalContentSwitcher.TYPE)
            {
                return <VerticalContentSwitcher node={node as VerticalContentSwitcherNode} />
            }
            else if (node.type === HomePage.TYPE)
            {
                return <HomePage node={node as HomePageNode} />
            }
            else if (node.type === Form.TYPE)
            {
                return <Form node={node as FormNode} />
            }
            else if (node.type === ScrollbarContainer.TYPE)
            {
                return <ScrollbarContainer node={node as ScrollbarContainerNode} />
            }
            else if (node.type === Text.TYPE)
            {
                return <Text node={node as TextNode} />
            }
            else 
            {
                console.log("Invalid node " + node.type);
                console.log(node);
            }
        }
        return <></>;
    }
}

