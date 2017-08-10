package wbs.platform.queue.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectDaoMethods;
import wbs.platform.queue.model.QueueSubjectRec;

public
class QueueSubjectDaoHibernate
	extends HibernateDaoLegacy
	implements QueueSubjectDaoMethods {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <QueueSubjectRec> findActive (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findActive");

		) {

			return findMany (
				transaction,
				QueueSubjectRec.class,

				createCriteria (
					transaction,
					QueueSubjectRec.class,
					"_queueSubject")

				.add (
					Restrictions.gt (
						"_queueSubject.activeItems",
						0l))

				.add (
					Restrictions.eq (
						"_queueSubject.queue",
						queue))

			);

		}

	}

	@Override
	public
	QueueSubjectRec find (
			@NonNull Transaction parentTransaction,
			@NonNull QueueRec queue,
			@NonNull Record <?> object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"find");

		) {

			return findOneOrNull (
				transaction,
				QueueSubjectRec.class,

				createCriteria (
					transaction,
					QueueSubjectRec.class,
					"_queueSubject")

				.add (
					Restrictions.eq (
						"_queueSubject.queue",
						queue))

				.add (
					Restrictions.eq (
						"_queueSubject.objectId",
						object.getId ()))

			);

		}

	}

	@Override
	public
	List <QueueSubjectRec> findActive (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findActive");

		) {

			return findMany (
				transaction,
				QueueSubjectRec.class,

				createCriteria (
					transaction,
					QueueSubjectRec.class,
					"_queueSubject")

				.add (
					Restrictions.gt (
						"_queueSubject.activeItems",
						0l))

			);

		}

	}

}
