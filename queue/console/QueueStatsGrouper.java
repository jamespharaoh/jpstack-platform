package wbs.platform.queue.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.NonNull;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.reporting.StatsDataSet;
import wbs.console.reporting.StatsGrouper;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueRec;

import wbs.utils.string.FormatWriter;

@SingletonComponent ("queueStatsGrouper")
public
class QueueStatsGrouper
	implements StatsGrouper {

	// singleton dependencies

	@SingletonDependency
	ConsoleObjectManager consoleObjectManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueConsoleHelper queueHelper;

	// implementation

	@Override
	public
	Set <Object> getGroups (
			@NonNull StatsDataSet dataSet) {

		return new HashSet <Object> (
			dataSet.indexValues ().get ("queueId"));

	}

	@Override
	public
	void writeTdForGroup (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Object group) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeTdForGroup");

		) {

			QueueRec queue =
				queueHelper.findRequired (
					transaction,
					(Long)
					group);

			consoleObjectManager.writeTdForObjectMiniLink (
				transaction,
				formatWriter,
				privChecker,
				queue);

		}

	}

	@Override
	public
	List <Object> sortGroups (
			@NonNull Transaction parentTransaction,
			@NonNull Set <Object> groups) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"sortGroups");

		) {

			List <QueueRec> queues =
				new ArrayList<> (
					groups.size ());

			for (
				Object group
					: groups
			) {

				Long queueId =
					(Long)
					group;

				queues.add (
					queueHelper.findRequired (
						transaction,
						queueId));

			}

			Collections.sort (
				queues);

			ArrayList <Object> queueIds =
				new ArrayList<> (
					queues.size ());

			for (
				QueueRec queue
					: queues
			) {

				queueIds.add (
					queue.getId ());

			}

			return queueIds;

		}

	}

}
