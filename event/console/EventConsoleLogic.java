package wbs.platform.event.console;

import wbs.console.part.PagePart;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.PermanentRecord;

import wbs.platform.event.model.EventRec;

import wbs.utils.string.FormatWriter;

public
interface EventConsoleLogic {

	PagePart makeEventsPart (
			Transaction parentTransaction,
			PermanentRecord <?> object);

	void writeEventHtml (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			EventRec event);

	void writeObjectAsHtml (
			Transaction parentTransaction,
			FormatWriter formatWriter,
			Object object);

	void writeEventsTable (
			Transaction parentTransaction,
			FormatWriter htmlWriter,
			Iterable <EventRec> events);

	void writeEventRow (
			Transaction parentTransaction,
			FormatWriter htmlWriter,
			EventRec event);

}