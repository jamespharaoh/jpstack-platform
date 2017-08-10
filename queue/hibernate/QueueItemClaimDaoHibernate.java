package wbs.platform.queue.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueItemClaimDaoMethods;
import wbs.platform.queue.model.QueueItemClaimRec;
import wbs.platform.queue.model.QueueItemClaimStatus;
import wbs.platform.user.model.UserRec;

public
class QueueItemClaimDaoHibernate
	extends HibernateDaoLegacy
	implements QueueItemClaimDaoMethods {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <QueueItemClaimRec> findClaimed (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findClaimed");

		) {

			return findMany (
				transaction,
				QueueItemClaimRec.class,

				createCriteria (
					transaction,
					QueueItemClaimRec.class)

				.add (
					Restrictions.eq (
						"status",
						QueueItemClaimStatus.claimed))

			);

		}

	}

	@Override
	public
	List <QueueItemClaimRec> findClaimed (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findClaimed");

		) {

			return findMany (
				transaction,
				QueueItemClaimRec.class,

				createCriteria (
					transaction,
					QueueItemClaimRec.class)

				.add (
					Restrictions.eq (
						"user",
						user))

				.add (
					Restrictions.eq (
						"status",
						QueueItemClaimStatus.claimed))

			);

		}

	}

}
