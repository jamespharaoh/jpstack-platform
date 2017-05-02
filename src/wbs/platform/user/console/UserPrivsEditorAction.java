package wbs.platform.user.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.priv.console.PrivConsoleHelper;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.updatelog.logic.UpdateManager;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

import wbs.web.responder.Responder;

@PrototypeComponent ("userPrivsEditorAction")
public
class UserPrivsEditorAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	PrivConsoleHelper privHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UpdateManager updateManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserConsoleHelper userHelper;

	@SingletonDependency
	UserPrivConsoleHelper userPrivHelper;

	static
	Pattern privDataPattern =
		Pattern.compile ("(\\d+)-(can|cangrant)=(0|1)");

	@Override
	public
	Responder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		return responder (
			"userPrivsEditorResponder");

	}

	@Override
	public
	Responder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			UserRec user =
				userHelper.findFromContextRequired (
					transaction);

			Matcher matcher =
				privDataPattern.matcher (
					requestContext.parameterRequired (
						"privdata"));

			while (matcher.find ()) {

				Long privId =
					Long.parseLong (
						matcher.group (
							1));

				boolean grant =
					matcher.group (2).equals ("cangrant");

				boolean can =
					matcher.group (3).equals ("1");

				PrivRec priv =
					privHelper.findRequired (
						transaction,
						privId);

				UserPrivRec userPriv =
					userPrivHelper.find (
						transaction,
						user,
						priv);

				// check we have permission to update this priv

				if (
					! privChecker.canGrant (
						transaction,
						priv.getId ())
				) {
					continue;
				}

				boolean changed = false;

				if (userPriv == null) {

					// create new UserPriv

					if (can) {

						userPriv =
							userPrivHelper.createInstance ()

							.setPriv (
								priv)

							.setUser (
								user);

						if (grant) {

							userPriv

								.setCan (
									false)

								.setCanGrant (
									true);

						} else {

							userPriv

								.setCan (
									true)

								.setCanGrant (
									false);

						}

						userPrivHelper.insert (
							transaction,
							userPriv);

						changed = true;

					}

				} else {

					// update userpriv

					if (grant) {

						if (userPriv.getCanGrant () != can)
							changed = true;

						userPriv.setCanGrant (can);

					} else {

						if (userPriv.getCan () != can)
							changed = true;

						userPriv.setCan (can);

					}

					// remove if necessary

					if (! userPriv.getCan ()
							&& ! userPriv.getCanGrant ()) {

						userPrivHelper.remove (
							transaction,
							userPriv);

					}

				}

				// create event

				if (changed) {

					eventLogic.createEvent (
						transaction,
						can
							? (grant
								? "user_grant_grant"
								: "user_grant")
							: (grant
								? "user_revoke_grant"
								: "user_revoke"),
						userConsoleLogic.userRequired (
							transaction),
						priv,
						user);

				}

			}

			// signal the privs have been updated

			updateManager.signalUpdate (
				transaction,
				"user_privs",
				user.getId ());

			updateManager.signalUpdate (
				transaction,
				"privs",
				0l);

			transaction.commit ();

			requestContext.addNotice (
				"Updated user privileges");

			return null;

		}

	}

}
