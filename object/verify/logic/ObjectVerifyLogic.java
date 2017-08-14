package wbs.platform.object.verify.logic;

import com.google.common.base.Optional;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;

import wbs.platform.object.verify.model.ObjectVerificationRec;
import wbs.platform.user.model.UserRec;

public
interface ObjectVerifyLogic {

	<Type extends Record <Type>>
	ObjectVerificationRec createOrUpdateObjectVerification (
			Transaction parentTransaction,
			Type object);

	void performVerification (
			Transaction parentTransaction,
			ObjectVerificationRec verification,
			Optional <UserRec> user);

}
