package wbs.platform.object.verify.model;

import java.util.List;

import com.google.common.base.Optional;

import org.joda.time.Instant;

import wbs.framework.database.Transaction;

import wbs.platform.object.core.model.ObjectTypeRec;

public
interface ObjectVerificationDaoMethods {

	List <Long> findIdsPendingLimit (
			Transaction parentTransaction,
			Instant timestamp,
			Long maxItems);

	Optional <ObjectVerificationRec> findByParent (
			Transaction parentTransaction,
			ObjectTypeRec parentType,
			Long parentId);

}
