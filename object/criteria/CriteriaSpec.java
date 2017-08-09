package wbs.platform.object.criteria;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.entity.record.Record;
import wbs.framework.logging.TaskLogger;

public
interface CriteriaSpec {

	boolean evaluate (
			TaskLogger parentTaskLogger,
			ConsoleRequestContext requestContext,
			UserPrivChecker privChecker,
			ConsoleHelper <?> objectHelper,
			Record <?> object);

}
