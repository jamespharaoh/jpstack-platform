package wbs.platform.core.console;

import static wbs.utils.string.StringUtils.stringStartsWithSimple;
import static wbs.utils.string.StringUtils.substringFrom;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.deployment.console.ApiDeploymentConsoleHelper;
import wbs.platform.deployment.console.ConsoleDeploymentConsoleHelper;
import wbs.platform.deployment.console.DaemonDeploymentConsoleHelper;
import wbs.platform.deployment.model.ApiDeploymentRec;
import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.deployment.model.DaemonDeploymentRec;
import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("coreSystemRestartAction")
public
class CoreSystemRestartAction
	extends ConsoleAction {

	// singleton components

	@SingletonDependency
	ApiDeploymentConsoleHelper apiDeploymentHelper;

	@SingletonDependency
	ConsoleDeploymentConsoleHelper consoleDeploymentHelper;

	@SingletonDependency
	DaemonDeploymentConsoleHelper daemonDeploymentHelper;

	@SingletonDependency
	EventLogic eventLogic;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserPrivChecker userPrivChecker;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("coreSystemRestartResponder")
	ComponentProvider <WebResponder> restartResponderProvider;

	// details

	@Override
	protected
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return restartResponderProvider.provide (
				taskLogger);

		}

	}

	// implementation

	@Override
	protected
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			for (
				String parameterName
					: requestContext.parameterMap ().keySet ()
			) {

				if (
					stringStartsWithSimple (
						"api/",
						parameterName)
				) {

					restartApi (
						transaction,
						substringFrom (
							parameterName,
							"api/".length ()));

				}


				if (
					stringStartsWithSimple (
						"console/",
						parameterName)
				) {

					restartConsole (
						transaction,
						substringFrom (
							parameterName,
							"console/".length ()));

				}


				if (
					stringStartsWithSimple (
						"daemon/",
						parameterName)
				) {

					restartDaemon (
						transaction,
						substringFrom (
							parameterName,
							"daemon/".length ()));

				}

			}

			return null;

		}

	}

	private
	void restartApi (
			@NonNull Transaction parentTransaction,
			@NonNull String apiDeploymentCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"restartApi");

		) {

			ApiDeploymentRec apiDeployment =
				apiDeploymentHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					apiDeploymentCode);

			if (
				! userPrivChecker.canRecursive (
					transaction,
					apiDeployment,
					"restart")
			) {

				requestContext.addError (
					"Permission denied");

				return;

			}

			if (apiDeployment.getRestart ()) {

				requestContext.addWarning (
					"Already restarting");

				return;

			}

			apiDeployment

				.setRestart (
					true);

			eventLogic.createEvent (
				transaction,
				"api_deployment_restarted",
				userConsoleLogic.userRequired (
					transaction),
				apiDeployment);

			transaction.commit ();

			requestContext.addNotice (
				"Restart triggered");

		}

	}

	private
	void restartConsole (
			@NonNull Transaction parentTransaction,
			@NonNull String consoleDeploymentCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"restartConsole");

		) {

			ConsoleDeploymentRec consoleDeployment =
				consoleDeploymentHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					consoleDeploymentCode);

			if (
				! userPrivChecker.canRecursive (
					transaction,
					consoleDeployment,
					"restart")
			) {

				requestContext.addError (
					"Permission denied");

				return;

			}

			if (consoleDeployment.getRestart ()) {

				requestContext.addWarning (
					"Already restarting");

				return;

			}

			consoleDeployment

				.setRestart (
					true);

			eventLogic.createEvent (
				transaction,
				"console_deployment_restarted",
				userConsoleLogic.userRequired (
					transaction),
				consoleDeployment);

			transaction.commit ();

			requestContext.addNotice (
				"Restart triggered");

		}

	}

	private
	void restartDaemon (
			@NonNull Transaction parentTransaction,
			@NonNull String daemonDeploymentCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"restartDaemon");

		) {

			DaemonDeploymentRec daemonDeployment =
				daemonDeploymentHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					daemonDeploymentCode);

			if (
				! userPrivChecker.canRecursive (
					transaction,
					daemonDeployment,
					"restart")
			) {

				requestContext.addError (
					"Permission denied");

				return;

			}

			if (daemonDeployment.getRestart ()) {

				requestContext.addWarning (
					"Already restarting");

				return;

			}

			daemonDeployment

				.setRestart (
					true);

			eventLogic.createEvent (
				transaction,
				"daemon_deployment_restarted",
				userConsoleLogic.userRequired (
					transaction),
				daemonDeployment);

			transaction.commit ();

			requestContext.addNotice (
				"Restart triggered");

		}

	}

}
