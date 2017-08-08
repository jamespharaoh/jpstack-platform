package wbs.platform.event.logic;

import wbs.framework.database.Transaction;

import wbs.platform.event.model.EventRec;

public
interface EventLogic {

	EventRec createEvent (
			Transaction parentTransaction,
			String typeCode);

	EventRec createEvent (
			Transaction parentTransaction,
			String typeCode,
			Object ... objects);

	public final static
	long integerEventLinkType = -1l;

	public final static
	long booleanEventLinkType = -2l;

	public final static
	long instantEventLinkType = -3l;

	public final static
	long durationEventLinkType = -4l;

}