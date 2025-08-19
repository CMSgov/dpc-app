import { workflow, setup, teardown } from "./workflows.js";
import { constants } from "./constants.js";

// See https://grafana.com/docs/k6/latest/using-k6/k6-options/reference for
// details on this configuration object.

// Before running this, ensure the org provider cap is removed in your environment.  If you don't, the workflows
// will start failing since they create a new provider at the start of every iteration.  Your response times will
// look very fast all of the sudden and your results will be very, very skewed.
export const options = {
  scenarios: {
    workflow: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1h',
      preAllocatedVUs: constants.preAllocatedVUs,
      maxVUs: constants.maxVUs,
      stages: [
        { target: 50, duration: '30m' },    // Average load for 30 minutes
        { target: 1500, duration: '30m' },  // Ramp up to 30x load over 30 minutes
        { target: 1500, duration: '60m' }   // Stay at 30x load for 60 minutes
      ],
      exec: "workflow"
    }
  }
};

export { workflow, setup, teardown };
