package wbs.platform.send;

import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.List;

import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.daemon.SleepingDaemonService;

@Accessors (fluent = true)
public abstract
class GenericSendDaemon <
	Service extends Record <Service>,
	Job extends Record <Job>,
	Item extends Record <Item>
>
	extends SleepingDaemonService {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	// hooks

	protected abstract
	GenericSendHelper <Service, Job, Item> helper ();

	// details

	@Override
	protected
	String friendlyName () {

		return stringFormat (
			"%s send",
			capitalise (
				helper ().name ()));

	}

	@Override
	protected
	String backgroundProcessName () {

		return stringFormat (
			"%s.send",
			helper ().parentTypeName ());

	}

	// implementation

	@Override
	protected
	void runOnce (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"runOnce");

		) {

			taskLogger.debugFormat (
				"Looking for jobs in sending state");

			List <Long> jobIds =
				getJobIds (
					taskLogger);

			jobIds.forEach (
				jobId ->
					runJob (
						taskLogger,
						jobId));

		}

	}

	private
	List <Long> getJobIds (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"getJobIds");

		) {

			return iterableMapToList (
				helper ().findSendingJobs (
					transaction),
				Job::getId);

		}

	}

	void runJob (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long jobId) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"runJob");

		) {

			transaction.debugFormat (
				"Performing send for %s",
				integerToDecimalString (
					jobId));

			Job job =
				helper ().jobHelper ().findRequired (
					transaction,
					jobId);

			Service service =
				helper ().getService (
					transaction,
					job);

			if (
				! helper ().jobSending (
					transaction,
					service,
					job)
			) {

				transaction.debugFormat (
					"Not sending job %s",
					integerToDecimalString (
						jobId));

				return;

			}

			if (
				! helper ().jobConfigured (
					transaction,
					service,
					job)
			) {

				transaction.warningFormat (
					"Not configured job %s",
					integerToDecimalString (
						jobId));

				return;

			}

			List <Item> items =
				helper ().findItemsLimit (
					transaction,
					service,
					job,
					100l);

			if (
				items.isEmpty ()
			) {

				// handle completion

				transaction.debugFormat (
					"Triggering completion for job %s",
					integerToDecimalString (
						jobId));

				helper ().sendComplete (
					transaction,
					service,
					job);

			} else {

				// perform send

				for (
					Item item
						: items
				) {

					// verify the item

					boolean itemVerified =
						helper ().verifyItem (
							transaction,
							service,
							job,
							item);

					if (itemVerified) {

						transaction.debugFormat (
							"Sending item %s",
							integerToDecimalString (
								item.getId ()));

						// send item

						helper ().sendItem (
							transaction,
							service,
							job,
							item);

					} else {

						transaction.debugFormat (
							"Rejecting item %s",
							integerToDecimalString (
								item.getId ()));

						// reject it

						helper ().rejectItem (
							transaction,
							service,
							job,
							item);

					}

				}

			}

			// commit transaction

			transaction.commit ();

			transaction.debugFormat (
				"Finished send for job %s",
				integerToDecimalString (
					jobId));

		}

	}

}
