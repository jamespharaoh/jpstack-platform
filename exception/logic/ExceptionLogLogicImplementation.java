package wbs.platform.exception.logic;

import static wbs.utils.etc.EnumUtils.enumEqualSafe;

import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.exception.ExceptionUtilsImplementation;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;

import wbs.platform.exception.model.ConcreteExceptionResolution;
import wbs.platform.exception.model.ExceptionLogObjectHelper;
import wbs.platform.exception.model.ExceptionLogRec;
import wbs.platform.exception.model.ExceptionLogTypeObjectHelper;
import wbs.platform.exception.model.ExceptionLogTypeRec;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

@SingletonComponent ("exceptionLogLogic")
public
class ExceptionLogLogicImplementation
	implements ExceptionLogLogic {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogObjectHelper exceptionLogHelper;

	@SingletonDependency
	ExceptionLogTypeObjectHelper exceptionLogTypeHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserObjectHelper userHelper;

	// implementation

	@Override
	public
	ExceptionLogRec logException (
			@NonNull Transaction parentTransaction,
			@NonNull CharSequence typeCode,
			@NonNull CharSequence source,
			@NonNull CharSequence summary,
			@NonNull CharSequence dump,
			@NonNull Optional <Long> userId,
			@NonNull GenericExceptionResolution resolution) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"logException");

		) {

			// lookup type

			ExceptionLogTypeRec exceptionLogType =
				exceptionLogTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					typeCode);

			// lookup user

			UserRec user =
				userId.isPresent ()
					? userHelper.findRequired (
						transaction,
						userId.get ())
					: null;

			// create exception log

			ExceptionLogRec exceptionLog =
				exceptionLogHelper.insert (
					transaction,
					exceptionLogHelper.createInstance ()

				.setTimestamp (
					transaction.now ())

				.setType (
					exceptionLogType)

				.setSource (
					source.toString ())

				.setSummary (
					ExceptionUtilsImplementation.substituteNuls (
						summary))

				.setUser (
					user)

				.setDump (
					ExceptionUtilsImplementation.substituteNuls (
						dump))

				.setFatal (
					enumEqualSafe (
						resolution,
						GenericExceptionResolution.fatalError))

				.setResolution (
					conreteMap.get (
						resolution))

			);

			return exceptionLog;

		}

	}

	// data

	final static
	Map <GenericExceptionResolution,ConcreteExceptionResolution> conreteMap =
		ImmutableMap.<GenericExceptionResolution,ConcreteExceptionResolution>builder ()

		.put (
			GenericExceptionResolution.tryAgainNow,
			ConcreteExceptionResolution.tryAgainNow)

		.put (
			GenericExceptionResolution.tryAgainLater,
			ConcreteExceptionResolution.tryAgainLater)

		.put (
			GenericExceptionResolution.ignoreWithUserWarning,
			ConcreteExceptionResolution.ignoreWithUserWarning)

		.put (
			GenericExceptionResolution.ignoreWithThirdPartyWarning,
			ConcreteExceptionResolution.ignoreWithThirdPartyWarning)

		.put (
			GenericExceptionResolution.ignoreWithLoggedWarning,
			ConcreteExceptionResolution.ignoreWithLoggedWarning)

		.put (
			GenericExceptionResolution.ignoreWithNoWarning,
			ConcreteExceptionResolution.ignoreWithNoWarning)

		.put (
			GenericExceptionResolution.fatalError,
			ConcreteExceptionResolution.fatalError)

		.build ();

}