import React from "react";
import { Node, Renderer } from "../Renderer";
import { Backend, DataPage } from "../../backend/Backend";

export interface ListViewNode extends Node {
    element: Node;
}

export interface Props {
    node: ListViewNode;
}

interface State {
    data: DataPage[];
}


export class ListView extends React.Component<Props, State> {
    public static TYPE: string = "LISTVIEW";


    public componentDidMount(): void {
        const target = this.props.node.target;
        Backend.fetchData(target, 0).then((page) => {
            const data = this.state.data;
            data.push(page);
            return this.setState({
                data: data
            });
        });
    }

    constructor(props: Props) {
        super(props);
        this.state = {
            data: []
        }
    }

    public render(): JSX.Element {
        const elements = this.state.data?.flatMap((page) => page.elements);
        return (
            <>
                <div >
                    {elements?.map((element, index) => (
                        <span key={index}>{Renderer.render(this.props.node?.element)}</span>
                    ))}
                </div>
            </>
        );
    }
}
