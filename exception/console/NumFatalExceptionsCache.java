package wbs.platform.exception.console;

import lombok.NonNull;

import org.joda.time.Duration;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.DatabaseCachedGetter;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.exception.model.ExceptionLogObjectHelper;

import wbs.utils.cache.CachedGetter;

@SingletonComponent ("numFatalExceptionsCache")
public
class NumFatalExceptionsCache
	implements CachedGetter <Transaction, Long> {

	// singleton dependencies

	@SingletonDependency
	ExceptionLogObjectHelper exceptionLogHelper;;

	@ClassSingletonDependency
	LogContext logContext;

	// state

	private
	CachedGetter <Transaction, Long> delegate;

	// life cycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			delegate =
				new DatabaseCachedGetter<> (
					logContext,
					this::refresh,
					Duration.standardSeconds (
						2l));

		}

	}

	// public implementation

	@Override
	public
	Long get (
			@NonNull Transaction context) {

		return delegate.get (
			context);

	}

	// private implementation

	private
	Long refresh (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"refresh");

		) {

			return exceptionLogHelper.countWithAlertAndFatal (
				transaction);

		}

	}

}
