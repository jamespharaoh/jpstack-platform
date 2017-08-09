package wbs.platform.object.search;

import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalMapRequired;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringSplitComma;

import java.io.Serializable;
import java.util.List;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;
import wbs.platform.user.model.UserRec;

import wbs.utils.etc.NumberUtils;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectSearchGetAction")
public
class ObjectSearchGetAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// properties

	@Getter @Setter
	ComponentProvider <WebResponder> searchResponderProvider;

	@Getter @Setter
	ComponentProvider <WebResponder> resultsResponderProvider;

	@Getter @Setter
	String sessionKey;

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

			return searchResponderProvider.provide (
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

			UserRec user =
				userConsoleLogic.userRequired (
					transaction);

			Optional <Serializable> searchOptional =
				userSessionLogic.userDataObject (
					transaction,
					user,
					stringFormat (
						"object_search_%s_fields",
						sessionKey));

			Optional <List <Long>> objectIdsOptional =
				optionalMapRequired (
					userSessionLogic.userDataString (
						transaction,
						user,
						stringFormat (
							"object_search_%s_results",
							sessionKey)),
					objectIdStrings ->
						iterableMapToList (
							stringSplitComma (
								objectIdStrings),
							NumberUtils::parseIntegerRequired));

			if (

				optionalIsNotPresent (
					searchOptional)

				|| optionalIsNotPresent (
					objectIdsOptional)

			) {

				userSessionLogic.userDataRemove (
					transaction,
					user,
					stringFormat (
						"object_search_%s_results",
						sessionKey));

				transaction.commit ();

				return searchResponderProvider.provide (
					transaction);

			} else {

				transaction.commit ();

				return resultsResponderProvider.provide (
					transaction);

			}

		}

	}

}
