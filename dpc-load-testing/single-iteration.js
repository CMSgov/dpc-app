import { constants } from "./constants.js";
import { workflow, setup, teardown } from "./workflows.js";

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  scenarios: {
    workflow: {
      executor: 'per-vu-iterations',
      vus: constants.maxVUs,
      iterations: 1,
      exec: "workflow"
    }
  }
};

export { workflow, setup, teardown };
