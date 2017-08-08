package wbs.platform.event.console;

import static wbs.utils.etc.NumberUtils.integerEqualSafe;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.time.TimeUtils.instantToDateNullSafe;
import static wbs.utils.time.TimeUtils.millisToInstant;
import static wbs.utils.time.TimeUtils.millisecondsToDuration;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowSeparatorWrite;
import static wbs.web.utils.HtmlUtils.htmlLinkWriteHtml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lombok.NonNull;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.PagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.entity.record.PermanentRecord;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.event.model.EventLinkRec;
import wbs.platform.event.model.EventRec;
import wbs.platform.event.model.EventTypeRec;
import wbs.platform.media.console.MediaConsoleLogic;
import wbs.platform.media.model.MediaRec;
import wbs.platform.text.model.TextRec;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.string.FormatWriter;
import wbs.utils.string.LazyFormatWriter;

import wbs.web.utils.HtmlUtils;

@SingletonComponent ("eventConsoleLogic")
public
class EventConsoleLogicImplementation
	implements EventConsoleLogic {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaConsoleLogic mediaConsoleLogic;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ObjectEventsPart> objectEventsPartProvider;

	// implementation

	@Override
	public
	PagePart makeEventsPart (
			@NonNull Transaction parentTransaction,
			@NonNull PermanentRecord <?> object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"makeEventsPart");

		) {

			List <Record <?>> children =
				objectManager.getMinorChildren (
					transaction,
					object);

			List <GlobalId> objectGlobalIds =
				new ArrayList<> ();

			objectGlobalIds.add (
				objectManager.getGlobalId (
					transaction,
					object));

			for (
				Record<?> child
					: children
			) {

				objectGlobalIds.add (
					objectManager.getGlobalId (
						transaction,
						child));

			}

			return objectEventsPartProvider.provide (
				transaction,
				objectEventsPart ->
					objectEventsPart

				.dataObjectIds (
					objectGlobalIds)

			);

		}

	}

	@Override
	public
	void writeEventHtml (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull EventRec event) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeEventHtml");

		) {

			EventTypeRec eventType =
				event.getEventType ();

			String text =
				HtmlUtils.htmlEncode (
					eventType.getDescription ());

			// TODO this is not correct and will not handle text matching "%#" in
			// the previous replacements. it should be replaced with a single pass.

			for (
				EventLinkRec eventLink
					: event.getEventLinks ()
			) {

				if (
					integerEqualSafe (
						eventLink.getTypeId (),
						EventLogic.integerEventLinkType)
				) {

					// integer

					text =
						text.replaceAll (
							"%" + eventLink.getIndex (),
							HtmlUtils.htmlEncode (
								eventLink.getRefId ().toString ()));

				} else if (
					integerEqualSafe (
						eventLink.getTypeId (),
						EventLogic.booleanEventLinkType)
				) {

					// boolean

					text =
						text.replaceAll (
							"%" + eventLink.getIndex (),
							eventLink.getRefId () != 0 ? "yes" : "no");

				} else if (
					integerEqualSafe (
						eventLink.getTypeId (),
						EventLogic.instantEventLinkType)
				) {

					// instant

					text =
						text.replaceAll (
							"%" + eventLink.getIndex (),
							userConsoleLogic.timestampWithTimezoneString (
								transaction,
								millisToInstant (
									eventLink.getRefId ())));

				} else if (
					integerEqualSafe (
						eventLink.getTypeId (),
						EventLogic.durationEventLinkType)
				) {

					// duration

					text =
						text.replaceAll (
							"%" + eventLink.getIndex (),
							userConsoleLogic.prettyDuration (
								transaction,
								millisecondsToDuration (
									eventLink.getRefId ())));

				} else {

					// locate referenced object

					Record <?> object =
						objectManager.findObjectRequired (
							transaction,
							new GlobalId (
								eventLink.getTypeId (),
								eventLink.getRefId ()));

					// escape replacement text, what a mess ;-)

					try (

						LazyFormatWriter objectFormatWriter =
							new LazyFormatWriter ();

					) {

						writeObjectAsHtml (
							transaction,
							objectFormatWriter,
							object);

						String replacement =
							objectFormatWriter.toString ()

							.replaceAll (
								"\\\\",
								"\\\\\\\\")

							.replaceAll (
								"\\$",
								"\\\\\\$");

						// perform replacement

						text =
							text.replaceAll (
								"%" + eventLink.getIndex (),
								replacement);

					}

				}

			}

			formatWriter.writeString (
				text);

		}

	}

	@Override
	public
	void writeObjectAsHtml (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Object object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeObjectAsHtml");

		) {

			if (object instanceof Integer) {

				formatWriter.writeString (
					object.toString ());

			} else if (object instanceof TextRec) {

				TextRec text =
					(TextRec)
					object;

				formatWriter.writeFormat (
					"\"%s\"",
					text.getText ());

			} else if (object instanceof MediaRec) {

				MediaRec media =
					(MediaRec)
					object;

				htmlLinkWriteHtml (
					formatWriter,
					objectManager.localLink (
						transaction,
						media),
					() -> mediaConsoleLogic.writeMediaThumb32 (
						transaction,
						formatWriter,
						media));

			} else if (object instanceof Record) {

				Record <?> dataObject =
					(Record <?>)
					object;

				objectManager.writeHtmlForObject (
					transaction,
					formatWriter,
					dataObject,
					optionalAbsent (),
					false);

			} else {

				throw new IllegalArgumentException ();

			}

		}

	}

	@Override
	public
	void writeEventsTable (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull Iterable <EventRec> events) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeEventsTable");

		) {

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Time",
				"Details");

			int dayNumber = 0;

			for (
				EventRec event
					: events
			) {

				Calendar calendar =
					Calendar.getInstance ();

				calendar.setTime (
					instantToDateNullSafe (
						event.getTimestamp ()));

				int newDayNumber =
					(calendar.get (Calendar.YEAR) << 9)
					+ calendar.get (Calendar.DAY_OF_YEAR);

				if (newDayNumber != dayNumber) {

					htmlTableRowSeparatorWrite (
						formatWriter);

					htmlTableRowOpen (
						formatWriter,
						htmlStyleRuleEntry (
							"font-weight",
							"bold"));

					htmlTableCellWrite (
						formatWriter,
						userConsoleLogic.dateStringLong (
							transaction,
							event.getTimestamp ()),
						htmlColumnSpanAttribute (2l));

					htmlTableRowClose (
						formatWriter);

					dayNumber =
						newDayNumber;

				}

				writeEventRow (
					transaction,
					formatWriter,
					event);

			}

			htmlTableClose (
				formatWriter);

		}

	}

	@Override
	public
	void writeEventRow (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull EventRec event) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"writeEventRow");

		) {

			htmlTableRowOpen (
				formatWriter);

			htmlTableCellWrite (
				formatWriter,
				userConsoleLogic.timeString (
					transaction,
					event.getTimestamp ()));

			try (

				LazyFormatWriter eventFormatWriter =
					new LazyFormatWriter ();

			) {

				writeEventHtml (
					transaction,
					eventFormatWriter,
					event);

				htmlTableCellWriteHtml (
					formatWriter,
					eventFormatWriter.toString ());

			} catch (RuntimeException exception) {

				transaction.errorFormatException (
					exception,
					"Error displaying event %s",
					integerToDecimalString (
						event.getId ()));

				htmlTableCellWrite (
					formatWriter,
					"(error displaying this event)");

			}

			htmlTableRowClose (
				formatWriter);

		}

	}

}
