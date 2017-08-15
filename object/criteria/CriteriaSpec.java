package wbs.platform.object.criteria;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

public
interface CriteriaSpec <RecordType extends Record <RecordType>> {

	boolean evaluate (
			Transaction parentTransaction,
			ConsoleRequestContext requestContext,
			UserPrivChecker privChecker,
			ConsoleHelper <RecordType> consoleHelper,
			RecordType object);

}
