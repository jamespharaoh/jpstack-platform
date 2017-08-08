package wbs.platform.lock.logic;

import static wbs.utils.etc.NumberUtils.fromJavaInteger;

import java.util.Arrays;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.lock.model.LockObjectHelper;
import wbs.platform.lock.model.LockRec;

@SingletonComponent ("lockLogic")
public
class LockLogicImplementation
	implements LockLogic {

	// singleton dependencies

	@SingletonDependency
	LockObjectHelper lockHelper;

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	void magicLock (
			@NonNull Transaction parentTransaction,
			@NonNull Object ... objects) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"magicLock");

		) {

			Long lockId =
				fromJavaInteger (
					Arrays.hashCode (
						objects)
					& 0x3fff);

			LockRec lock =
				lockHelper.findRequired (
					transaction,
					lockId);

			lockHelper.lock (
				transaction,
				lock);

		}

	}

}
