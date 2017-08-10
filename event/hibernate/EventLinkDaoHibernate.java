package wbs.platform.event.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.event.model.EventLinkDao;
import wbs.platform.event.model.EventLinkRec;

public
class EventLinkDaoHibernate
	extends HibernateDaoLegacy
	implements EventLinkDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <EventLinkRec> findByTypeAndRef (
			@NonNull Transaction parentTransaction,
			@NonNull Long typeId,
			@NonNull Long refId) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findByTypeAndRef");

		) {

			return findMany (
				transaction,
				EventLinkRec.class,

				createCriteria (
					transaction,
					EventLinkRec.class,
					"_eventLink")

				.add (
					Restrictions.eq (
						"_eventLink.typeId",
						typeId))

				.add (
					Restrictions.eq (
						"_eventLink.refId",
						refId))

			);

		}

	}

}
