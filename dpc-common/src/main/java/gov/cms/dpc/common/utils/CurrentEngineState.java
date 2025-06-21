package gov.cms.dpc.common.utils;

/**
 * Defines the current state of the aggregation engine.  Used to orchestrate a graceful shutdown of dpc-aggregation.
 */
public class CurrentEngineState {
	public enum States {
		/**
		 * The aggregation engine is running.  This is the default state when dpc-aggregation is started.
		 */
		RUNNING,

		/**
		 * Dpc-aggregation has received a SIGINT and been asked to stop, but is still processing its last batch.
		 */
		STOPPING,

		/**
		 * Dpc-aggregation has received a SIGINT and finished processing its last batch.
		 */
		STOPPED
	}

	private States currentState;

	public CurrentEngineState() {
		currentState = States.RUNNING;
	}

	/**
	 * Gets the current state of the dpc-aggregation aggregation engine.
	 * @return {@link CurrentEngineState.States}
	 */
	public synchronized States getState() { return currentState; }

	/**
	 * Sets the current state of the engine, and wakes up any threads that are waiting on a state change.
	 * @param state The new state of the thread.
	 */
	public synchronized void setState(States state) {
		currentState = state;
		notifyAll();
	}
}
