import { rootReducer } from "./Reducer";
import { NodeStates, RootReducerState, UIContextStates } from "./StoreStates";
import { createStore, combineReducers } from "redux"


const store = createStore<RootReducerState, any, any, any>(rootReducer);
export default store;