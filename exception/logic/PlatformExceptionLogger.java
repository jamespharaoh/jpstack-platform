package wbs.platform.exception.logic;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.function.Function;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.ExceptionUtils;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.exception.model.ExceptionLogRec;

@SingletonComponent ("platformExceptionLogger")
public
class PlatformExceptionLogger
	implements ExceptionLogger {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogLogic exceptionLogLogic;

	@SingletonDependency
	ExceptionUtils exceptionLogic;

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	ExceptionLogRec logSimple (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull CharSequence typeCode,
			@NonNull CharSequence source,
			@NonNull CharSequence summary,
			@NonNull CharSequence dump,
			@NonNull Optional <Long> userId,
			@NonNull GenericExceptionResolution resolution) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"logSimple");

		) {

			return logExceptionWrapped (
				taskLogger,
				typeCode,
				source,
				userId,
				nestedTaskLogger -> realLogException (
					nestedTaskLogger,
					typeCode,
					source,
					summary,
					dump,
					userId,
					resolution));

		}

	}

	@Override
	public
	ExceptionLogRec logThrowable (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull CharSequence typeCode,
			@NonNull CharSequence source,
			@NonNull Throwable throwable,
			@NonNull Optional <Long> userId,
			@NonNull GenericExceptionResolution resolution) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"logThrowable");

		) {

			return logExceptionWrapped (
				taskLogger,
				typeCode,
				source,
				userId,
				nestedTaskLogger -> realLogException (
					nestedTaskLogger,
					typeCode,
					source,
					exceptionLogic.throwableSummary (
						taskLogger,
						throwable),
					exceptionLogic.throwableDump (
						taskLogger,
						throwable),
					userId,
					resolution));

		}

	}

	@Override
	public
	ExceptionLogRec logThrowableWithSummary (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull CharSequence typeCode,
			@NonNull CharSequence source,
			@NonNull CharSequence summary,
			@NonNull Throwable throwable,
			@NonNull Optional <Long> userId,
			@NonNull GenericExceptionResolution resolution) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"logThrowableWithSummary");

		) {

			return logExceptionWrapped (
				taskLogger,
				typeCode,
				source,
				userId,
				nestedTaskLogger -> realLogException (
					nestedTaskLogger,
					typeCode,
					source,
					stringFormat (
						"%s\n%s",
						summary,
						exceptionLogic.throwableSummary (
							taskLogger,
							throwable)),
					exceptionLogic.throwableDump (
						taskLogger,
						throwable),
					userId,
					resolution));

		}

	}

	ExceptionLogRec logExceptionWrapped (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull CharSequence typeCode,
			@NonNull CharSequence source,
			@NonNull Optional <Long> userId,
			@NonNull Function <TaskLogger, ExceptionLogRec> target) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"logExceptionWrapped");

		) {

			try {

				return target.apply (
					taskLogger);

			} catch (Exception furtherException) {

				taskLogger.fatalFormatException (
					furtherException,
					"Error logging exception");

				String furtherSummary =
					stringFormat (
						"Threw %s while logging exception from %s",
						furtherException.getClass ().getSimpleName (),
						source);

				try {

					realLogException (
						taskLogger,
						typeCode,
						"exception log",
						stringFormat (
							"%s\n%s",
							furtherSummary,
							exceptionLogic.throwableSummary (
								taskLogger,
								furtherException)),
						exceptionLogic.throwableDump (
							taskLogger,
							furtherException),
						optionalAbsent (),
						GenericExceptionResolution.fatalError);

				} catch (Exception yetAnotherException) {

					taskLogger.fatalFormatException (
						yetAnotherException,
						"Error logging error logging exception");

				}

				return null;

			}

		}

	}

	private
	ExceptionLogRec realLogException (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull CharSequence typeCode,
			@NonNull CharSequence source,
			@NonNull CharSequence summary,
			@NonNull CharSequence dump,
			@NonNull Optional <Long> userId,
			@NonNull GenericExceptionResolution resolution) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"realLogException");

		) {

			ExceptionLogRec exceptionLog =
				exceptionLogLogic.logException (
					transaction,
					typeCode,
					source,
					summary,
					dump,
					userId,
					resolution);

			transaction.commit ();

			return exceptionLog;

		}

	}

}
