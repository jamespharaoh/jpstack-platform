package wbs.platform.queue.console;

import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import wbs.console.helper.core.ConsoleHooks;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueItemSearch;
import wbs.platform.queue.model.QueueRec;

@SingletonComponent ("queueItemConsoleHooks")
public
class QueueItemConsoleHooks
	implements ConsoleHooks <QueueItemRec> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueConsoleHelper queueHelper;

	// implementation

	@Override
	public
	void applySearchFilter (
			@NonNull Transaction parentTransaction,
			@NonNull Object searchObject) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"applySearchFilter");

		) {

			QueueItemSearch search =
				genericCastUnchecked (
					searchObject);

			search

				.filter (
					true);

			// queues

			ImmutableList.Builder <Long> queuesBuilder =
				ImmutableList.builder ();

			for (
				QueueRec queue
					: queueHelper.findAll (
						transaction)
			) {

				Record <?> queueParent =
					objectManager.getParentRequired (
						transaction,
						queue);

				 if (
				 	! privChecker.canRecursive (
				 		transaction,
				 		queueParent,
				 		"manage")
				 ) {
				 	continue;
				 }

				queuesBuilder.add (
					queue.getId ());

			}

			search

				.filterQueueIds (
					queuesBuilder.build ());

		}

	}

}
