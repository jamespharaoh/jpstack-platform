package wbs.platform.status.console;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.StrongPrototypeDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.tools.ComponentFactory;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.core.console.ConsoleAsyncSubscription;

@SingletonComponent ("statusUpdateAsyncEndpoint")
public
class StatusUpdateAsyncEndpoint
	implements ComponentFactory <ConsoleAsyncSubscription <Object>> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	StatusUpdateAsyncHelper statusUpdateAsyncHelper;

	// prototype dependencies

	@StrongPrototypeDependency
	ComponentProvider <ConsoleAsyncSubscription <Object>>
		consoleAsyncSubscriptionProvider;

	// implementation

	@Override
	public
	ConsoleAsyncSubscription <Object> makeComponent (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"statusUpdateAsyncEndpoint");

		) {

			return consoleAsyncSubscriptionProvider.provide (
				taskLogger,
				consoleAsyncSubscription ->
					consoleAsyncSubscription

				.helper (
					statusUpdateAsyncHelper)

			);

		}

	}

}
