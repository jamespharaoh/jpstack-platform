package wbs.platform.core.console;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.lookup.BooleanLookup;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("coreAuthAction")
public
class CoreAuthAction
	extends ConsoleAction {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	BooleanLookup lookup;

	@Getter @Setter
	ComponentProvider <WebResponder> normalResponderProvider;

	@Getter @Setter
	ComponentProvider <WebResponder> deniedResponderProvider;

	@Override
	public
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"goReal");

		) {

			if (
				! lookup.lookup (
					requestContext.consoleContextStuffRequired ())
			) {

				requestContext.addError (
					"Access denied");

				return deniedResponderProvider.provide (
					taskLogger);

			}

			return normalResponderProvider.provide (
				taskLogger);

		}

	}

}
