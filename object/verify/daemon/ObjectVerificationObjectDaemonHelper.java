package wbs.platform.object.verify.daemon;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;

import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.daemon.ObjectDaemon;
import wbs.platform.object.verify.logic.ObjectVerifyLogic;
import wbs.platform.object.verify.model.ObjectVerificationObjectHelper;
import wbs.platform.object.verify.model.ObjectVerificationRec;

@SingletonComponent ("shnProductVerifyObjectDaemonHelper")
public
class ObjectVerificationObjectDaemonHelper
	implements ObjectDaemon <Long> {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectVerificationObjectHelper objectVerificationHelper;

	@SingletonDependency
	ObjectVerifyLogic objectVerifyLogic;

	// details

	@Override
	public
	String backgroundProcessName () {
		return "object-verification.update";
	}

	@Override
	public
	String itemNameSingular () {
		return "object verification";
	}

	@Override
	public
	String itemNamePlural () {
		return "object verifications";
	}

	@Override
	public
	LogContext logContext () {
		return logContext;
	}

	// public implementation

	@Override
	public
	List <Long> findObjectIds (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"findObjectIds");

		) {

			return objectVerificationHelper.findIdsPendingLimit (
				transaction,
				transaction.now (),
				16384l);

		}

	}

	@Override
	public
	void processObject (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long objectVerificationId) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"processObject");

		) {

			ObjectVerificationRec verification =
				objectVerificationHelper.findRequired (
					transaction,
					objectVerificationId);

			objectVerifyLogic.performVerification (
				transaction,
				verification,
				optionalAbsent ());

			transaction.commit ();

		}

	}

}
