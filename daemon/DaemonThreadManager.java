package wbs.platform.daemon;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.StrongPrototypeDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.utils.thread.ThreadManager;
import wbs.utils.thread.ThreadManagerImplementation;

@SingletonComponent ("threadManager")
public
class DaemonThreadManager
	implements ComponentFactory <ThreadManager> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@StrongPrototypeDependency
	ComponentProvider <ThreadManagerImplementation> threadManagerProvider;

	// components

	@Override
	public
	ThreadManager makeComponent (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeComponent");

		) {

			return threadManagerProvider.provide (
				taskLogger,
				threadManager ->
					threadManager

				.exceptionTypeCode (
					"daemon")

			);

		}

	}

}
