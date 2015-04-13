package wbs.platform.queue.console;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.List;

import javax.inject.Inject;

import lombok.Cleanup;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.console.action.ConsoleAction;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.queue.model.QueueItemClaimObjectHelper;
import wbs.platform.queue.model.QueueItemClaimRec;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

@PrototypeComponent ("queueUsersAction")
public
class QueueUsersAction
	extends ConsoleAction {

	// dependencies

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	QueueItemClaimObjectHelper queueItemClaimHelper;

	@Inject
	QueueConsoleLogic queueConsoleLogic;

	@Inject
	UserObjectHelper userHelper;

	// details

	@Override
	public
	Responder backupResponder () {
		return responder ("queueUsersResponder");
	}

	// implementation

	@Override
	public
	Responder goReal () {

		// get params

		int userId =
			Integer.parseInt (
				requestContext.parameter ("userId"));

		boolean reclaim;

		if (requestContext.parameter ("reclaim") != null) {
			reclaim = true;
		} else if (requestContext.parameter ("unclaim") != null) {
			reclaim = false;
		} else {
			throw new RuntimeException ();
		}

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		// load stuff

		UserRec theUser =
			userHelper.find (userId);

		UserRec myUser =
			userHelper.find (requestContext.userId ());

		// load items

		List<QueueItemClaimRec> queueItemClaims =
			queueItemClaimHelper.findClaimed (
				theUser);

		int numQueueItems =
			queueItemClaims.size ();

		// process items

		for (QueueItemClaimRec queueItemClaim
				: queueItemClaims) {

			QueueItemRec queueItem =
				queueItemClaim.getQueueItem ();

			if (reclaim) {

				queueConsoleLogic.reclaimQueueItem (
					queueItem,
					theUser,
					myUser);

			} else {

				queueConsoleLogic.unclaimQueueItem (
					queueItem,
					theUser);

			}

		}

		transaction.commit ();

		// add notice

		if (reclaim) {

			requestContext.addNotice (
				stringFormat (
					"Reclaimed %s queue items",
					numQueueItems));

		} else {

			requestContext.addNotice (
				stringFormat (
					"Unclaimed %s queue items",
					numQueueItems));

		}

		return null;

	}

}
