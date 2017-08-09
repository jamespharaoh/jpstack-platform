package wbs.platform.status.console;

import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.etc.Misc.doNothing;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.OptionalUtils.optionalMapRequired;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.string.StringUtils.emptyStringIfNull;
import static wbs.utils.string.StringUtils.joinWithSeparator;

import java.util.List;
import java.util.concurrent.Future;

import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import lombok.NonNull;

import org.apache.commons.lang3.tuple.Pair;

import wbs.console.async.ConsoleAsyncConnectionHandle;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.core.console.ConsoleAsyncSubscription;
import wbs.platform.deployment.logic.DeploymentLogic;
import wbs.platform.deployment.model.ConsoleDeploymentRec;
import wbs.platform.scaffold.console.RootConsoleHelper;
import wbs.platform.scaffold.model.RootRec;
import wbs.platform.user.model.UserRec;

import wbs.utils.time.core.DefaultTimeFormatter;

@SingletonComponent ("statusUpdateAsyncHelper")
public
class StatusUpdateAsyncHelper
	implements ConsoleAsyncSubscription.Helper <Object> {

	// singleton dependencies

	@SingletonDependency
	DeploymentLogic deploymentLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RootConsoleHelper rootHelper;

	@SingletonDependency
	StatusLineManager statusLineManager;

	@SingletonDependency
	DefaultTimeFormatter timeFormatter;

	@SingletonDependency
	WbsConfig wbsConfig;

	// details

	@Override
	public
	String endpointPath () {
		return "/status/update";
	}

	@Override
	public
	String endpointName () {
		return "Status update";
	}

	// implementation

	@Override
	public
	Object newSubscription (
			@NonNull TaskLogger parentTaskLogger) {

		return new Object ();

	}

	@Override
	public
	void prepareUpdate (
			@NonNull Transaction parentTransaction) {

		doNothing ();

	}

	@Override
	public
	void updateSubscriber (
			@NonNull Transaction parentTransaction,
			@NonNull Object state,
			@NonNull ConsoleAsyncConnectionHandle connectionHandle,
			@NonNull UserRec user,
			@NonNull UserPrivChecker privChecker) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"updateSubscriber");

		) {

			List <Pair <String, Future <JsonObject>>> futures =
				iterableMapToList (
					statusLineManager.getStatusLines (),
					statusLine ->
						Pair.of (
							statusLine.typeName (),
							statusLine.getUpdateData (
								transaction,
								privChecker)));

			JsonObject payload =
				new JsonObject ();

			JsonArray updates =
				new JsonArray ();

			addUpdate (
				updates,
				"core",
				buildUpdateCore (
					transaction,
					user));

			for (
				Pair <String, Future <JsonObject>> future
					: futures
			) {

				try {

					addUpdate (
						updates,
						future.getKey (),
						future.getValue ().get ());

				} catch (Exception exception) {

					transaction.errorFormatException (
						exception,
						"Error getting status update for %s",
						future.getKey ());

				}

			}

			payload.add (
				"updates",
				updates);

			connectionHandle.send (
				transaction,
				payload);

		}

	}

	private
	void addUpdate (
			@NonNull JsonArray updates,
			@NonNull String updateType,
			@NonNull JsonObject updateData) {

		JsonObject update =
			new JsonObject ();

		update.addProperty (
			"type",
			updateType);

		update.add (
			"data",
			updateData);

		updates.add (
			update);

	}

	private
	JsonObject buildUpdateCore (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"buildUpdateCore");

		) {

			RootRec root =
				rootHelper.findRequired (
					transaction,
					0l);

			Optional <ConsoleDeploymentRec> consoleDeploymentOptional =
				deploymentLogic.thisConsoleDeployment (
					transaction);

			JsonObject update =
				new JsonObject ();

			update.addProperty (
				"header",
				joinWithSeparator (
					" – ",
					presentInstances (
						optionalOf (
							"Status"),
						optionalMapRequired (
							consoleDeploymentOptional,
							consoleDeployment ->
								consoleDeployment.getStatusLabel ()),
						optionalOf (
							deploymentLogic.gitVersion ()))));

			update.addProperty (
				"timestamp",
				timeFormatter.timestampTimezoneSecondShortString (
					timeFormatter.timezoneParseRequired (
						ifNull (
							user.getDefaultTimezone (),
							user.getSlice ().getDefaultTimezone (),
							wbsConfig.defaultTimezone ())),
					transaction.now ()));

			update.addProperty (
				"notice",
				emptyStringIfNull (
					root.getNotice ()));

			return update;

		}

	}

}
