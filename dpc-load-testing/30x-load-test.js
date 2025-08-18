import { workflow, setup, teardown } from "./workflows.js";
import { constants } from "./constants.js";

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  scenarios: {
    workflow: {
      executor: 'constant-arrival-rate',
      duration: '1h',
      rate: 1500,
      timeUnit: '1h',
      preAllocatedVUs: constants.preAllocatedVUs,
      exec: "workflow",
      maxVUs: constants.maxVUs,
    }
  }
};

export { workflow, setup, teardown };
