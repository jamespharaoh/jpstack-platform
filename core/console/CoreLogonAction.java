package wbs.platform.core.console;

import static wbs.utils.collection.CollectionUtils.collectionHasTwoItems;
import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.collection.CollectionUtils.listSecondElementRequired;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringInSafe;
import static wbs.utils.string.StringUtils.stringSplitFullStop;

import java.util.List;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;
import wbs.platform.user.model.UserRec;
import wbs.platform.user.model.UserSessionRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("coreLogonAction")
public
class CoreLogonAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	ServiceObjectHelper serviceHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	@SingletonDependency
	WbsConfig wbsConfig;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("coreLogonResponder")
	ComponentProvider <WebResponder> logonResponderProvider;

	@PrototypeDependency
	@NamedDependency ("coreRedirectResponder")
	ComponentProvider <WebResponder> redirectResponderProvider;

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

			return logonResponderProvider.provide (
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

			// get params

			@NonNull
			String slice =
				requestContext.parameterRequired (
					"slice");

			@NonNull
			String username =
				requestContext.parameterRequired (
					"username");

			@NonNull
			String password =
				requestContext.parameterRequired (
					"password");

			// extract slice from username if present

			if (
				optionalIsPresent (
					requestContext.header (
						"x-wbs-slice"))
			) {

				List <String> usernameParts =
					stringSplitFullStop (
						username);

				if (
					collectionHasTwoItems (
						usernameParts)
				) {

					slice =
						listFirstElementRequired (
							usernameParts);

					username =
						listSecondElementRequired (
							usernameParts);

				}

			}

			// check we got the right params

			if (
				stringInSafe (
					"",
					slice,
					username,
					password)
			) {
				return null;
			}

			// attempt logon

			transaction.debugFormat (
				"Attempting logon");

			Optional <UserSessionRec> userSessionOptional =
				userSessionLogic.userLogonTry (
					transaction,
					requestContext,
					slice.toLowerCase (),
					username.toLowerCase (),
					password,
					requestContext.header (
						"User-Agent"),
					requestContext.cookie (
						"txt2_console"));

			Optional <Long> userIdOptional;

			if (
				optionalIsPresent (
					userSessionOptional)
			) {

				transaction.debugFormat (
					"Logon success");

				UserSessionRec userSession =
					optionalGetRequired (
						userSessionOptional);

				UserRec user =
					userSession.getUser ();

				userIdOptional =
					optionalOf (
						user.getId ());

			} else {

				transaction.debugFormat (
					"Logon failure");

				userIdOptional =
					optionalAbsent ();

			}

			transaction.commit ();

			// if it failed show the logon page again

			if (
				optionalIsNotPresent (
					userIdOptional)
			) {

				requestContext.addWarningFormat (
					"Sorry, the details you entered did not match. Please try ",
					"again, or contact an appropriate person for help.");

				transaction.warningFormat (
					"Failed logon attempt for %s.%s",
					slice,
					username);

				return null;

			}

			Long userId =
				optionalGetRequired (
					userIdOptional);

			// save the userid

			transaction.noticeFormat (
				"Successful logon for %s.%s (%s)",
				slice,
				username,
				integerToDecimalString (
					userId));

			// and redirect to the console proper

			return redirectResponderProvider.provide (
				transaction);

		}

	}

}
