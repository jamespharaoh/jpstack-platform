package wbs.platform.object.verify.logic;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

import wbs.platform.object.verify.model.ObjectVerificationRec;

public
interface ObjectVerifyLogic {

	<Type extends Record <Type>>
	ObjectVerificationRec createOrUpdateObjectVerification (
			Transaction parentTransaction,
			Type object);

}
