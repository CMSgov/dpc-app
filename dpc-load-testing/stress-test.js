import { workflow, setup, teardown } from "./workflows.js";

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.
export const options = {
  scenarios: {
    workflow: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1h',
      preAllocatedVUs: 10,
      stages: [
        { target: 50, duration: '30m' },
        { target: 100, duration: '30m' },
        { target: 150, duration: '30m' },
        { target: 200, duration: '30m' },
      ],
      exec: "workflow"
    }
  }
};

export { workflow, setup, teardown };
