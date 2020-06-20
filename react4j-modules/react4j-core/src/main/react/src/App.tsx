import React from 'react';
import './App.css';
import { Renderer, Node } from './renderer/Renderer';
import { Backend } from './backend/Backend';

class App extends React.Component<{}, { node: Node }>
{
  constructor()
  {
    super({});
    this.state = {
      node: {} as Node
    };
  }

  render(): JSX.Element
  {
    return (
      <div className="App">
        {Renderer.render(this.state.node)}
      </div>
    );
  }

  public componentDidMount()
  {
    Backend.getUI().then((response) =>
    {
      const homeNode = response.data.root;
      this.setState({
        node: homeNode
      });
    });
  }
}

export default App;
