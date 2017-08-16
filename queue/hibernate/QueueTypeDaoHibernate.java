package wbs.platform.queue.hibernate;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueTypeDao;
import wbs.platform.queue.model.QueueTypeRec;

public
class QueueTypeDaoHibernate
	extends HibernateDaoLegacy
	implements QueueTypeDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <QueueTypeRec> findAll (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findAll");

		) {

			return findMany (
				transaction,
				QueueTypeRec.class,

				createCriteria (
					transaction,
					QueueTypeRec.class)

			);

		}

	}

	@Override
	public
	QueueTypeRec findRequired (
			@NonNull Transaction parentTransaction,
			@NonNull Long queueTypeId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findRequired");

		) {

			return get (
				transaction,
				QueueTypeRec.class,
				queueTypeId);

		}

	}

}
