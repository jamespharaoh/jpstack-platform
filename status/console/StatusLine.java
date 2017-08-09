package wbs.platform.status.console;

import java.util.concurrent.Future;

import com.google.gson.JsonObject;

import wbs.console.part.PagePart;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.database.Transaction;

public
interface StatusLine {

	String typeName ();

	PagePart createPagePart (
			Transaction parentTransaction);

	Future <JsonObject> getUpdateData (
			Transaction parentTransaction,
			UserPrivChecker privChecker);

}
