package wbs.platform.service.model;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

public
interface ServiceObjectHelperMethods {

	ServiceRec findOrCreate (
			Transaction parentTransaction,
			Record <?> parent,
			String typeCode,
			String code);

}