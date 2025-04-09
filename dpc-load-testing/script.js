import { workflowA, workflowB, setup, teardown } from "./workflows.js";

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  scenarios: {
    workflow_a: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflowA"
    },
    workflow_b: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      exec: "workflowB"
    }
  }
};

export { workflowA, workflowB, setup, teardown };
