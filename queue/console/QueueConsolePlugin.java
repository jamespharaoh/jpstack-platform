package wbs.platform.queue.console;

import java.util.List;

import wbs.framework.logging.TaskLogger;

import wbs.platform.queue.model.QueueItemRec;

import wbs.web.responder.WebResponder;

public
interface QueueConsolePlugin {

	List <String> queueTypeCodes (
			TaskLogger parentTaskLogger);

	WebResponder makeResponder (
			TaskLogger taskLogger,
			QueueItemRec queueItem);

}
