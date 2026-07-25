import React from 'react';
import './App.css';
import { Renderer, Node } from './renderer/Renderer';
import { Backend } from './backend/Backend';
import { AxiosHelper } from './utils/AxiosHelper';
import { InFlightTracker } from './backend/InFlightTracker';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';

interface State {
  node: Node;
  inFlightCount: number;
}

class App extends React.Component<{}, State> {
  private unsubscribeInFlightTracker?: () => void;

  constructor() {
    super({});
    this.state = {
      node: {} as Node,
      inFlightCount: InFlightTracker.getCount()
    };
  }

  render(): JSX.Element {
    return (
      // C2 testability hook (plan-74 Goal 3b): the settle signal surfaced on this single stable
      // root element -- data-inflight-count is the authoritative value, data-rerender-pending is
      // its boolean projection. A browser test can waitFor(count === 0) instead of sleeping.
      <div
        className="App"
        data-inflight-count={this.state.inFlightCount}
        data-rerender-pending={this.state.inFlightCount > 0}
      >
        {Renderer.render(this.state.node)}
      </div>
    );
  }

  public componentDidMount() {
    AxiosHelper.initializeAxios();
    this.unsubscribeInFlightTracker = InFlightTracker.subscribe((inFlightCount) => {
      this.setState({ inFlightCount });
    });
    Backend.getUI().then((response) => {
      const homeNode = response.data.root;
      this.setState({
        node: homeNode
      });
    });
  }

  public componentWillUnmount() {
    this.unsubscribeInFlightTracker?.();
  }
}

export default App;
