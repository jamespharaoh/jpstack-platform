package wbs.platform.object.verify.hibernate;

import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.object.verify.model.ObjectVerificationDaoMethods;
import wbs.platform.object.verify.model.ObjectVerificationRec;

public
class ObjectVerificationDaoHibernate
	extends HibernateDaoLegacy
	implements ObjectVerificationDaoMethods {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// public implementation

	@Override
	public
	List <Long> findIdsPendingLimit (
			@NonNull Transaction parentTransaction,
			@NonNull Instant timestamp,
			@NonNull Long maxItems) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findIdsPendingLimit");

		) {

			return findMany (
				transaction,
				Long.class,

				createCriteria (
					transaction,
					ObjectVerificationRec.class,
					"_objectVerification")

				.add (
					Restrictions.le (
						"_objectVerification.nextRun",
						timestamp))

				.addOrder (
					Order.asc (
						"_objectVerification.nextRun"))

				.setProjection (
					Projections.id ())

				.setMaxResults (
					toJavaIntegerRequired (
						maxItems))

			);


		}

	}

}
