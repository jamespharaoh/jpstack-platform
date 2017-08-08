package wbs.platform.deployment.daemon;

import static wbs.utils.etc.NetworkUtils.runHostname;
import static wbs.utils.string.StringUtils.keyEqualsDecimalInteger;
import static wbs.utils.string.StringUtils.keyEqualsString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import java.util.List;

import lombok.NonNull;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.daemon.SleepingDaemonService;
import wbs.platform.deployment.model.ApiDeploymentObjectHelper;
import wbs.platform.deployment.model.ApiDeploymentRec;
import wbs.platform.deployment.model.ConsoleDeploymentObjectHelper;
import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.deployment.model.DaemonDeploymentObjectHelper;
import wbs.platform.deployment.model.DaemonDeploymentRec;
import wbs.platform.deployment.model.DeploymentState;

@SingletonComponent ("deploymentDaemon")
public
class DeploymentAgent
	extends SleepingDaemonService {

	// singleton components

	@SingletonDependency
	ApiDeploymentObjectHelper apiDeploymentHelper;

	@SingletonDependency
	ConsoleDeploymentObjectHelper consoleDeploymentHelper;

	@SingletonDependency
	DaemonDeploymentObjectHelper daemonDeploymentHelper;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	// details

	@Override
	protected
	String friendlyName () {
		return "Deployment agent";
	}

	@Override
	protected
	String backgroundProcessName () {

		return stringFormat (
			"root.deployment-agent-%s",
			hostname);

	}

	// state

	private
	String hostname =
		runHostname ();

	// implementation

	@Override
	protected
	void runOnce (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"runOnce");

		) {

			// find local api deployments

			List <ApiDeploymentRec> apiDeployments =
				apiDeploymentHelper.findByHostNotDeleted (
					transaction,
					hostname);

			apiDeployments.forEach (
				apiDeployment ->
					runApiDeployment (
						transaction,
						transaction,
						apiDeployment));

			// find local console deployments

			List <ConsoleDeploymentRec> consoleDeployments =
				consoleDeploymentHelper.findByHostNotDeleted (
					transaction,
					hostname);

			consoleDeployments.forEach (
				consoleDeployment ->
					runConsoleDeployment (
						transaction,
						transaction,
						consoleDeployment));

			// find local daemon deployments

			List <DaemonDeploymentRec> daemonDeployments =
				daemonDeploymentHelper.findByHostNotDeleted (
					transaction,
					hostname);

			daemonDeployments.forEach (
				daemonDeployment ->
					runDaemonDeployment (
						transaction,
						transaction,
						daemonDeployment));

			// commit transaction

			transaction.commit ();

		}

	}

	// private implementation

	private
	void runApiDeployment (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull OwnedTransaction transaction,
			@NonNull ApiDeploymentRec apiDeployment) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"runApiDeployment",
					keyEqualsDecimalInteger (
						"apiDeploymentId",
						apiDeployment.getId ()));

		) {

			// update state

			try {

				apiDeployment

					.setState (
						getServiceState (
							taskLogger,
							apiDeployment.getServiceName ()))

					.setStateTimestamp (
						transaction.now ());

			} catch (Exception exception) {

				apiDeployment

					.setState (
						DeploymentState.unknown)

					.setStateTimestamp (
						transaction.now ());

			}

			// perform restart

			if (apiDeployment.getRestart ()) {

				taskLogger.noticeFormat (
					"Restarting API deployment %s",
					apiDeployment.getServiceName ());

				restartService (
					apiDeployment.getServiceName ());

				apiDeployment

					.setRestart (
						false);

			}

		}

	}

	private
	void runConsoleDeployment (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull OwnedTransaction transaction,
			@NonNull ConsoleDeploymentRec consoleDeployment) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"runConsoleDeployment",
					keyEqualsDecimalInteger (
						"consoleDeploymentId",
						consoleDeployment.getId ()));

		) {

			// update state

			try {

				consoleDeployment

					.setState (
						getServiceState (
							taskLogger,
							consoleDeployment.getServiceName ()))

					.setStateTimestamp (
						transaction.now ());

			} catch (Exception exception) {

				consoleDeployment

					.setState (
						DeploymentState.unknown)

					.setStateTimestamp (
						transaction.now ());

			}

			// perform restart

			if (consoleDeployment.getRestart ()) {

				taskLogger.noticeFormat (
					"Restarting console deployment %s",
					consoleDeployment.getServiceName ());

				restartService (
					consoleDeployment.getServiceName ());

				consoleDeployment

					.setRestart (
						false);

			}

		}

	}

	private
	void runDaemonDeployment (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull OwnedTransaction transaction,
			@NonNull DaemonDeploymentRec daemonDeployment) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"runDaemonDeployment",
					keyEqualsDecimalInteger (
						"daemonDeploymentId",
						daemonDeployment.getId ()));

		) {

			// update state

			try {

				daemonDeployment

					.setState (
						getServiceState (
							taskLogger,
							daemonDeployment.getServiceName ()))

					.setStateTimestamp (
						transaction.now ());

			} catch (Exception exception) {

				daemonDeployment

					.setState (
						DeploymentState.unknown)

					.setStateTimestamp (
						transaction.now ());

			}

			// perform restart

			if (daemonDeployment.getRestart ()) {

				taskLogger.noticeFormat (
					"Restarting daemon deployment %s",
					daemonDeployment.getServiceName ());

				restartService (
					daemonDeployment.getServiceName ());

				daemonDeployment

					.setRestart (
						false);

			}

		}

	}

	private
	void restartService (
			@NonNull String serviceName) {

		try (

			DBusConnection dbus =
				DBusConnection.getConnection (
					DBusConnection.SYSTEM);

		) {

			SystemdManagerDbus systemd =
				SystemdManagerDbus.get (
					dbus);

			SystemdUnitDbus systemdUnit =
				systemd.getUnit (
					serviceName);

			systemdUnit.restart (
				"replace");

		} catch (DBusException dbusException) {

			throw new RuntimeException (
				dbusException);

		}

	}

	private
	DeploymentState getServiceState (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String serviceName) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getServiceState",
					keyEqualsString (
						"serviceName",
						serviceName));

			DBusConnection dbus =
				DBusConnection.getConnection (
					DBusConnection.SYSTEM);

		) {

			SystemdManagerDbus systemd =
				SystemdManagerDbus.get (
					dbus);

			SystemdUnitDbus systemdUnit =
				systemd.getUnit (
					serviceName);

			String unitLoadState =
				systemdUnit.loadState ();

			String unitActiveState =
				systemdUnit.activeState ();

			String unitSubState =
				systemdUnit.subState ();

			if (
				stringNotEqualSafe (
					unitLoadState,
					"loaded")
			) {

				return DeploymentState.stopped;

			}

			switch (unitActiveState) {

			case "active":

				if (
					stringEqualSafe (
						unitSubState,
						"running")
				) {
					return DeploymentState.running;
				} else {

					taskLogger.warningFormat (
						"Service %s ",
						serviceName,
						"state is active but sub-state is %s",
						unitSubState);

					return DeploymentState.error;

				}

			case "inactive":

				return DeploymentState.stopped;

			case "activating":

				return DeploymentState.starting;

			case "deactivating":

				return DeploymentState.stopping;

			case "failed":

				return DeploymentState.error;

			default:

				taskLogger.warningFormat (
					"Service %s ",
					serviceName,
					"is in unknown state %s",
					unitActiveState);

				return DeploymentState.error;

			}

		} catch (DBusException dbusException) {

			throw new RuntimeException (
				dbusException);

		}

	}

}
