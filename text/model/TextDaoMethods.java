package wbs.platform.text.model;

import wbs.framework.database.Transaction;

public
interface TextDaoMethods {

	TextRec findByTextNoFlush (
			Transaction parentTransaction,
			String text);

}