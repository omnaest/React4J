import React from 'react';
import './App.css';
import { Renderer, Node } from './renderer/Renderer';
import { Backend } from './backend/Backend';
import { AxiosHelper } from './utils/AxiosHelper';
import 'bootstrap/dist/css/bootstrap.min.css';

class App extends React.Component<{}, { node: Node }> {
  constructor() {
    super({});
    this.state = {
      node: {} as Node
    };
  }

  render(): JSX.Element {
    return (
      <div className="App">
        {Renderer.render(this.state.node)}
      </div>
    );
  }

  public componentDidMount() {
    AxiosHelper.initializeAxios();
    Backend.getUI().then((response) => {
      const homeNode = response.data.root;
      this.setState({
        node: homeNode
      });
    });
  }
}

export default App;
