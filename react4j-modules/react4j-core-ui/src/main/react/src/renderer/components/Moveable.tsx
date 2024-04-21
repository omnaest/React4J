
import React from "react";
import { fromEvent } from "baconjs"
import ReactDOM from "react-dom";

export interface Props
{
}

export interface State
{
    x: number;
    y: number;
}

export class Moveable extends React.Component<Props, State>
{
    public static TYPE: string = "MOVEABLE";

    constructor(props: Props)
    {
        super(props);
        this.state = {
            x: 100,
            y: 100
        };
    }

    private setXandY(event: any)
    {
        this.setState({
            x: event.x,
            y: event.y
        });
    }

    public componentDidMount(): void
    {
        var node = ReactDOM.findDOMNode(this);
        fromEvent(node, "ondrag").onValue(this.setXandY.bind(this));
    }

    public render(): JSX.Element
    {
        return (
            <img
                draggable
                onDrag={(e) =>
                {
                    console.log(e);
                    console.log(e.clientX);
                    console.log(e.clientY);
                }}
                style={{
                    position: "absolute",
                    left: this.state.x,
                    top: this.state.y,
                    width: "50px",
                    height: "50px",
                    zIndex: 1000000
                }}
                alt="title"
                src={"/images/logo.png"}
                width="100%"
            />
        );
    }
}
