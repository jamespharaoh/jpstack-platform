package wbs.platform.event.hibernate;

import static wbs.utils.etc.NullUtils.isNotNull;

import java.util.List;

import lombok.NonNull;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDaoLegacy;
import wbs.framework.logging.LogContext;

import wbs.platform.event.model.EventDao;
import wbs.platform.event.model.EventRec;
import wbs.platform.event.model.EventSearch;

public
class EventDaoHibernate
	extends HibernateDaoLegacy
	implements EventDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	List <Long> searchIds (
			@NonNull Transaction parentTransaction,
			@NonNull EventSearch eventSearch) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"searchIds");

		) {

			Criteria criteria =
				createCriteria (
					transaction,
					EventRec.class,
					"_event")

				.createAlias (
					"_event.eventType",
					"_eventType");

			// add search criteria

			if (
				isNotNull (
					eventSearch.timestampAfter ())
			) {

				criteria.add (
					Restrictions.ge (
						"_event.timestamp",
						eventSearch.timestampAfter ()));

			}

			if (
				isNotNull (
					eventSearch.timestampBefore ())
			) {

				criteria.add (
					Restrictions.lt (
						"_event.timestamp",
						eventSearch.timestampBefore ()));

			}

			if (
				isNotNull (
					eventSearch.admin ())
			) {

				criteria.add (
					Restrictions.eq (
						"_eventType.admin",
						eventSearch.admin ()));

			}

			// add default order

			criteria

				.addOrder (
					Order.desc (
						"_event.timestamp"));

			// set to return ids only

			criteria

				.setProjection (
					Projections.id ());

			// perform and return

			return findMany (
				transaction,
				Long.class,
				criteria);

		}

	}

}
