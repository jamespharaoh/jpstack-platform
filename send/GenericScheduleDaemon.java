package wbs.platform.send;

import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.List;

import lombok.NonNull;
import lombok.experimental.Accessors;

import org.joda.time.Instant;

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
class GenericScheduleDaemon <
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
			"%s schedule",
			helper ().name ());

	}

	@Override
	protected
	String backgroundProcessName () {

		return stringFormat (
			"%s.schedule",
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
				"Looking for scheduled broadcasts to send");

			List <Long> jobIds =
				findJobs (
					taskLogger);

			jobIds.forEach (
				jobId ->
					runJob (
						taskLogger,
						jobId));

		}

	}

	private
	List <Long> findJobs (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"runOnce");

		) {

			return iterableMapToList (
				helper ().findScheduledJobs (
					transaction,
					transaction.now ()),
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

			Job job =
				helper ().jobHelper ().findRequired (
					transaction,
					jobId);

			Service service =
				helper ().getService (
					transaction,
					job);

			// check state is still "scheduled"

			if (
				! helper ().jobScheduled (
					transaction,
					service,
					job)
			) {

				transaction.logicFormat (
					"Not sending %s because it is not scheduled",
					integerToDecimalString (
						jobId));

				return;

			}

			// check scheduled time is in the future

			Instant scheduledTime =
				helper ().getScheduledTime (
					transaction,
					service,
					job);

			if (
				scheduledTime.isAfter (
					transaction.now ())
			) {

				transaction.logicFormat (
					"Not sending %s because it is scheduled in the future",
					integerToDecimalString (
						jobId));

				return;

			}

			// move to sending state

			transaction.noticeFormat (
				"Sending %s",
				integerToDecimalString (
					jobId));

			helper ().sendStart (
				transaction,
				service,
				job);

			transaction.commit ();

		}

	}

}
