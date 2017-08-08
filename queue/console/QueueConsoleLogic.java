package wbs.platform.queue.console;

import java.util.Map;
import java.util.Set;

import wbs.framework.database.Transaction;

import wbs.platform.queue.metamodel.QueueTypeSpec;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueTypeRec;
import wbs.platform.user.model.UserRec;

public
interface QueueConsoleLogic {

	QueueTypeSpec queueTypeSpec (
			Transaction parentTransaction,
			QueueTypeRec queueType);

	QueueItemRec claimQueueItem (
			Transaction parentTransaction,
			QueueRec queue,
			UserRec user);

	void unclaimQueueItem (
			Transaction parentTransaction,
			QueueItemRec queueItem,
			UserRec user);

	void reclaimQueueItem (
			Transaction parentTransaction,
			QueueItemRec queueItem,
			UserRec oldUser,
			UserRec newUser);

	boolean canSupervise (
			Transaction parentTransaction,
			QueueRec queue);

	Set <Long> getSupervisorSearchIds (
			Transaction parentTransaction,
			Map <String, Set <String>> conditions);

	Set <Long> getSupervisorFilterIds (
			Transaction parentTransaction);

}
