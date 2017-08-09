package wbs.platform.status.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;

import com.google.common.base.Optional;
import com.google.gson.JsonObject;

import lombok.NonNull;

import wbs.console.async.ConsoleAsyncConnectionHandle;
import wbs.console.async.ConsoleAsyncEndpoint;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserSessionLogic;

@SingletonComponent ("statusKeepaliveAsyncEndpoint")
public
class StatusKeepaliveAsyncEndpoint
	implements ConsoleAsyncEndpoint <JsonObject> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// details

	@Override
	public
	String endpointPath () {
		return "/status/keepalive";
	}

	@Override
	public
	Class <JsonObject> requestClass () {
		return JsonObject.class;
	}

	// implementation

	@Override
	public
	Optional <JsonObject> message (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleAsyncConnectionHandle connectionHandle,
			@NonNull Long userId,
			@NonNull JsonObject jsonObject) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"message");

		) {

			taskLogger.debugFormat (
				"Keepalive from user: %s",
				integerToDecimalString (
					userId));

			// we do nothing further, since the console async manager will
			// already verify the user's identity and perform any action
			// necessary to keep their session alive

			return optionalAbsent ();

		}

	}

}
