package wbs.platform.object.verify.logic;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectManager;

import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.object.verify.model.ObjectVerificationObjectHelper;
import wbs.platform.object.verify.model.ObjectVerificationRec;

@SingletonComponent ("objectVerifyLogic")
public
class ObjectVerifyLogicImplementation
	implements ObjectVerifyLogic {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	ObjectVerificationObjectHelper objectVerificationHelper;

	// public implementation

	@Override
	public <Type extends Record <Type>>
	ObjectVerificationRec createObjectVerification (
			@NonNull Transaction parentTransaction,
			@NonNull Type object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createObjectVerification");

		) {

			ObjectHelper <Type> objectHelper =
				objectManager.objectHelperForObjectRequired (
					object);

			ObjectTypeRec objectType =
				objectTypeHelper.findRequired (
					transaction,
					objectHelper.objectTypeId ());

			return objectVerificationHelper.insert (
				transaction,
				objectVerificationHelper.createInstance ()

				.setParentType (
					objectType)

				.setParentId (
					object.getId ())

				.setNextRun (
					transaction.now ())

			);

		}

	}

}
