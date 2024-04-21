import { ActionType } from "typesafe-actions";
import * as actions from './Action';

export type Actions = ActionType<typeof actions>;