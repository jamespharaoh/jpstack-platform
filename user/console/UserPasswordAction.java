package wbs.platform.user.console;

import static wbs.utils.etc.LogicUtils.referenceNotEqualWithClass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.NonNull;

import org.apache.commons.codec.binary.Base64;

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
import wbs.platform.user.model.UserRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("userPasswordAction")
public
class UserPasswordAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserConsoleHelper userHelper;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("userPasswordResponder")
	ComponentProvider <WebResponder> passwordResponderProvider;

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

			return passwordResponderProvider.provide (
				taskLogger);

		}

	}

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

			String password1 =
				requestContext.parameterRequired (
					"password_1");

			String password2 =
				requestContext.parameterRequired (
					"password_2");

			// load user

			UserRec user =
				userHelper.findFromContextRequired (
					transaction);

			// check privs

			if (

				referenceNotEqualWithClass (
					UserRec.class,
					user,
					userConsoleLogic.userRequired (
						transaction))

				&& ! privChecker.canRecursive (
					transaction,
					user,
					"manage")

			) {

				requestContext.addError (
					"Access denied");

				return null;

			}

			// check for password

			if (password1.length () == 0
				&& password2.length () == 0) {

				requestContext.addError (
					"No password supplied");

				return null;

			}

			// check passwords match

			if (! password1.equals (password2)) {

				requestContext.addError (
					"Passwords do not match");

				return null;

			}

			// digest password

			String hash;

			try {

				MessageDigest messageDigest =
					MessageDigest.getInstance ("SHA-1");

				messageDigest.update (
					password1.getBytes ());

				hash =
					Base64.encodeBase64String (
						messageDigest.digest ());

			} catch (NoSuchAlgorithmException exception) {

				requestContext.addError (
					"Internal error");

				return null;

			}

			// update user

			user.setPassword (hash);

			// create an event

			eventLogic.createEvent (
				transaction,
				"user_password_reset",
				userConsoleLogic.userRequired (
					transaction),
				user);

			transaction.commit ();

			// return

			requestContext.addNotice (
				"Password updated");

			return null;

		}

	}

}
