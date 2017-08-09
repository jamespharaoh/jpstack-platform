package wbs.platform.event.logic;

import static wbs.utils.etc.NumberUtils.fromJavaInteger;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.TypeUtils.classNameSimple;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.entity.record.PermanentRecord;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.event.model.EventLinkObjectHelper;
import wbs.platform.event.model.EventLinkRec;
import wbs.platform.event.model.EventObjectHelper;
import wbs.platform.event.model.EventRec;
import wbs.platform.event.model.EventTypeObjectHelper;
import wbs.platform.event.model.EventTypeRec;
import wbs.platform.text.model.TextObjectHelper;

@SingletonComponent ("eventLogic")
public
class EventLogicImplementation
	implements EventLogic {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventObjectHelper eventHelper;

	@SingletonDependency
	EventLinkObjectHelper eventLinkHelper;

	@SingletonDependency
	EventTypeObjectHelper eventTypeHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	TextObjectHelper textHelper;

	// implementation

	@Override
	public
	EventRec createEvent (
			@NonNull Transaction parentTransaction,
			@NonNull String typeCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createEvent");

		) {

			// lookup type

			EventTypeRec eventType =
				eventTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					typeCode);

			// create event

			EventRec event =
				eventHelper.insert (
					transaction,
					eventHelper.createInstance ()

				.setEventType (
					eventType)

				.setTimestamp (
					transaction.now ())

			);

			// return

			return event;

		}

	}

	@Override
	public
	EventRec createEvent (
			@NonNull Transaction parentTransaction,
			@NonNull String typeCode,
			@NonNull Object ... linkObjects) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createEvent");

		) {

			// create event

			EventRec event =
				createEvent (
					transaction,
					typeCode);

			// create event links

			int index = 0;

			for (
				Object linkObject
					: linkObjects
			) {

				EventLinkRec eventLink =
					createEventLink (
						transaction,
						event,
						linkObject,
						fromJavaInteger (
							index));

				eventLinkHelper.insert (
					transaction,
					eventLink);

				event.getEventLinks ().add (
					eventLink);

				index ++;

			}

			// return

			return event;

		}

	}

	private
	EventLinkRec createEventLink (
			@NonNull Transaction parentTransaction,
			@NonNull EventRec event,
			@NonNull Object originalLinkObject,
			@NonNull Long index) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createEventLink");

		) {

			Object linkObject =
				normaliseLinkObject (
					transaction,
					originalLinkObject,
					index);

			// handle permanent records as object ids

			if (linkObject instanceof PermanentRecord <?>) {

				PermanentRecord <?> dataObject =
					(PermanentRecord <?>) linkObject;

				if (dataObject.getId () == null)
					throw new IllegalArgumentException ();

				return eventLinkHelper.createInstance ()

					.setEvent (
						event)

					.setIndex (
						index)

					.setTypeId (
						objectManager.getObjectTypeId (
							transaction,
							dataObject))

					.setRefId (
						dataObject.getId ());

			}

			// store longs directly

			if (linkObject instanceof Long) {

				return eventLinkHelper.createInstance ()

					.setEvent (
						event)

					.setIndex (
						index)

					.setTypeId (
						EventLogic.integerEventLinkType)

					.setRefId (
						(Long) linkObject);

			}

			// store booleans directly

			if (linkObject instanceof Boolean) {

				return eventLinkHelper.createInstance ()

					.setEvent (
						event)

					.setIndex (
						index)

					.setTypeId (
						EventLogic.booleanEventLinkType)

					.setRefId (
						(Boolean) linkObject
							? 1L
							: 0L);

			}

			// store instants directly

			if (linkObject instanceof Instant) {

				Instant instant =
					(Instant)
					linkObject;

				return eventLinkHelper.createInstance ()

					.setEvent (
						event)

					.setIndex (
						index)

					.setTypeId (
						EventLogic.instantEventLinkType)

					.setRefId (
						instant.getMillis ());

			}

			// store durations directly

			if (linkObject instanceof Duration) {

				Duration duration =
					genericCastUnchecked (
						linkObject);

				return eventLinkHelper.createInstance ()

					.setEvent (
						event)

					.setIndex (
						index)

					.setTypeId (
						EventLogic.durationEventLinkType)

					.setRefId (
						duration.getMillis ());

			}

			// error otherwise

			throw new RuntimeException (
				stringFormat (
					"Unlinkable type %s passed as link parameter %s",
					classNameSimple (
						linkObject.getClass ()),
					integerToDecimalString (
						index)));

		}

	}

	private
	Object normaliseLinkObject (
			@NonNull Transaction parentTransaction,
			@NonNull Object originalLinkObject,
			@NonNull Long index) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"normaliseLinkObject");

		) {

			Object currentLinkObject =
				originalLinkObject;

			// convert to string, continue processing

			if (
				currentLinkObject instanceof Float
				|| currentLinkObject instanceof Double
				|| currentLinkObject instanceof Enum <?>
				|| currentLinkObject instanceof LocalDate
			) {

				currentLinkObject =
					currentLinkObject.toString ();

			}

			// convert to long, continue processing

			if (currentLinkObject instanceof Integer) {

				currentLinkObject =
					fromJavaInteger (
						(Integer)
						currentLinkObject);

			}

			// handle strings as text objects

			if (currentLinkObject instanceof String) {

				return textHelper.findOrCreate (
					transaction,
					(String)
					currentLinkObject);

			}

			// return object unchanged

			return currentLinkObject;

		}

	}

}
