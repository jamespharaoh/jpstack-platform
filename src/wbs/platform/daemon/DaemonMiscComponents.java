package wbs.platform.daemon;

import javax.inject.Provider;

import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.UninitializedDependency;
import wbs.framework.utils.ThreadManager;
import wbs.framework.utils.ThreadManagerImplementation;

@SingletonComponent ("daemonMiscComponents")
public
class DaemonMiscComponents {

	// unitialized dependencies

	@UninitializedDependency
	Provider <ThreadManagerImplementation> threadManagerImplementationProvider;

	// components

	@SingletonComponent ("threadManager")
	public
	ThreadManager threadManager () {

		return threadManagerImplementationProvider.get ()

			.exceptionTypeCode (
				"api");

	}

}
