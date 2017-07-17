package wbs.platform.core.console;

import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("coreLogoffAction")
public
class CoreLogoffAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserObjectHelper userHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <CoreLogoffResponder> coreLogoffResponderProvider;

	// details

	@Override
	public
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return coreLogoffResponderProvider.provide (
				taskLogger);

		}

	}

	// implementation

	@Override
	public
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			Optional <Long> userIdOptional =
				userConsoleLogic.userId ();

			if (
				optionalIsNotPresent (
					userIdOptional)
			) {
				return null;
			}

			Long userId =
				optionalGetRequired (
					userIdOptional);

			UserRec user =
				userHelper.findRequired (
					transaction,
					userId);

			userSessionLogic.userLogoff (
				transaction,
				user);

			transaction.commit ();

			return null;

		}

	}

}
