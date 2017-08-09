package wbs.platform.queue.console;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;

import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.reporting.StatsDataSet;
import wbs.console.reporting.StatsDatum;
import wbs.console.reporting.StatsProvider;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueItemObjectHelper;
import wbs.platform.queue.model.QueueItemStatsSearch;
import wbs.platform.queue.model.QueueItemUserStats;
import wbs.platform.scaffold.model.SliceObjectHelper;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.time.interval.TextualInterval;

@PrototypeComponent ("queueItemUserStatsProvider")
public
class QueueItemUserStatsProvider
	implements StatsProvider {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueItemObjectHelper queueItemHelper;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// state

	Set <Long> searchQueueIds;
	Set <Long> searchUserIds;

	Set <Long> filterQueueIds;
	Set <Long> filterUserIds;

	// public implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction,
			@NonNull Map <String, Set <String>> conditions) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			// get conditions

			searchQueueIds =
				queueConsoleLogic.getSupervisorSearchIds (
					transaction,
					conditions);

			searchUserIds =
				userConsoleLogic.getSupervisorSearchIds (
					transaction,
					conditions);

			// get filters

			filterQueueIds =
				queueConsoleLogic.getSupervisorFilterIds (
					transaction);

			filterUserIds =
				userConsoleLogic.getSupervisorFilterIds (
					transaction);

		}

	}

	@Override
	public
	StatsDataSet getStats (
			@NonNull Transaction parentTransaction,
			@NonNull Interval interval) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getStats");

		) {

			// fetch stats

			StatsDataSet statsDataSet =
				new StatsDataSet ();

			Set <Object> indexQueueIds =
				new HashSet<> ();

			Set <Object> indexUserIds =
				new HashSet<> ();

			List <QueueItemUserStats> queueItemUserStatsList =
				queueItemHelper.searchUserStats (
					transaction,
					new QueueItemStatsSearch ()

				.queueIds (
					searchQueueIds)

				.userIds (
					searchUserIds)

				.timestamp (
					TextualInterval.forInterval (
						DateTimeZone.UTC,
						interval))

				.filterQueueIds (
					filterQueueIds)

				.filterUserIds (
					filterUserIds)

			);

			for (
				QueueItemUserStats queueItemUserStats
					: queueItemUserStatsList
			) {

				statsDataSet.data ().add (
					new StatsDatum ()

					.startTime (
						interval.getStart ().toInstant ())

					.addIndex (
						"queueId",
						queueItemUserStats.getQueue ().getId ())

					.addIndex (
						"userId",
						queueItemUserStats.getUser ().getId ())

					.addValue (
						"numProcessed",
						queueItemUserStats.getNumProcessed ())

				);

				indexQueueIds.add (
					queueItemUserStats.getQueue ().getId ());

				indexUserIds.add (
					queueItemUserStats.getUser ().getId ());

			}

			statsDataSet.indexValues ().put (
				"queueId",
				indexQueueIds);

			statsDataSet.indexValues ().put (
				"userId",
				indexUserIds);

			return statsDataSet;

		}

	}

}
