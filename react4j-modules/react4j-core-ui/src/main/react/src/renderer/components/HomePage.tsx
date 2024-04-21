import React from "react";
import { Node, Renderer } from "../Renderer";

export interface HomePageNode extends Node
{
    navigation: Node;
    body: Node;
}

export interface Props
{
    node: HomePageNode;
}


interface State
{
    menuCollapsed: boolean;
}

export class HomePage extends React.Component<Props, State>
{
    public static TYPE: string = "HOMEPAGE";

    public constructor(props: Props)
    {
        super(props);
        this.state = {
            menuCollapsed: true
        };
    }

    private toggleMenuCollapsedState(): void
    {
        this.setState({ menuCollapsed: !this.state.menuCollapsed });
    }


    public render(): JSX.Element
    {
        const menuIconComponentContent = (
            <nav className="navbar navbar-light bg-white">
                <button
                    className="navbar-toggler"
                    type="button"
                    data-toggle="collapse"
                    data-target="#navbarContent"
                    aria-controls="navbarContent"
                    aria-expanded={this.state.menuCollapsed ? "false" : "true"}
                    aria-label="Toggle navigation"
                    onClick={() => this.toggleMenuCollapsedState()}
                >
                    <span className="navbar-toggler-icon"></span>
                </button>
            </nav>
        );
        const menuIconComponentAnker = (
            <div className="navbar-menu-icon-container">
                {menuIconComponentContent}
            </div>
        );

        const navbar = (
            <div className="body-top">
                {menuIconComponentContent}
                {Renderer.render(this.props.node.navigation)}
            </div>
        );

        const bottomTopPadding = (
            <div className="body-bottom-top-padding" />
        );

        if (this.props.node.navigation)
        {
            return (
                <>
                    {this.state.menuCollapsed && menuIconComponentAnker}
                    {!this.state.menuCollapsed && navbar}
                    <div className={this.state.menuCollapsed ? "body-full" : "body-bottom"}>
                        {this.state.menuCollapsed && bottomTopPadding}
                        {Renderer.render(this.props.node.body)}
                    </div>
                </>
            );
        }
        else
        {
            return (
                <>
                    < div className="body-full" >
                        {Renderer.render(this.props.node.body)}
                    </div >
                </>
            );
        }
    }
}
