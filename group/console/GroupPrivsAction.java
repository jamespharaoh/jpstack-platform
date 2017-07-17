package wbs.platform.group.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.group.model.GroupRec;
import wbs.platform.priv.console.PrivConsoleHelper;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.updatelog.logic.UpdateManager;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("groupPrivsAction")
public
class GroupPrivsAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@SingletonDependency
	GroupConsoleHelper groupHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	PrivConsoleHelper privHelper;

	@SingletonDependency
	UpdateManager updateManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserObjectHelper userHelper;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("groupPrivsResponder")
	ComponentProvider <WebResponder> privsResponderProvider;

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

			return privsResponderProvider.provide (
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

			long numRevoked = 0;
			long numGranted = 0;

			GroupRec group =
				groupHelper.findFromContextRequired (
					transaction);

			Matcher matcher =
				privDataPattern.matcher (
					requestContext.parameterRequired (
						"privdata"));

			while (matcher.find ()) {

				Long privId =
					Long.parseLong (
						matcher.group (1));

				boolean newCan =
					matcher.group (2).equals ("1");

				PrivRec priv =
					privHelper.findRequired (
						transaction,
						privId);

				boolean oldCan =
					group.getPrivs ().contains (priv);

				// check we have permission to update this priv

				if (
					! privChecker.canGrant (
						transaction,
						priv.getId ())
				) {
					continue;
				}

				if (! oldCan && newCan) {

					group.getPrivs ().add (
						priv);

					eventLogic.createEvent (
						transaction,
						"group_grant",
						userConsoleLogic.userRequired (
							transaction),
						priv,
						group);

					numGranted ++;

				} else if (oldCan && ! newCan) {

					group.getPrivs ().remove (
						priv);

					eventLogic.createEvent (
						transaction,
						"group_revoke",
						userConsoleLogic.userRequired (
							transaction),
						priv,
						group);

					numRevoked++;

				}

			}

			for (
				UserRec user
					: group.getUsers ()
			) {

				updateManager.signalUpdate (
					transaction,
					"user_privs",
					user.getId ());

			}

			transaction.commit ();

			if (numGranted > 0) {

				requestContext.addNotice (
					"" + numGranted + " privileges granted");

			}

			if (numRevoked > 0) {

				requestContext.addNotice (
					"" + numRevoked + " privileges revoked");

			}

			return null;

		}

	}

	private final static
	Pattern privDataPattern =
		Pattern.compile ("(\\d+)-can=(0|1)");

}
