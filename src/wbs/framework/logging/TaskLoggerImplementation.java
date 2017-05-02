package wbs.framework.logging;

import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.etc.NumberUtils.equalToZero;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.moreThanZero;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalDo;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalMapRequiredOrDefault;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringFormatArray;
import static wbs.utils.string.StringUtils.stringFormatLazy;
import static wbs.utils.string.StringUtils.stringFormatLazyArray;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import org.joda.time.Instant;

import wbs.utils.string.LazyString;

public
class TaskLoggerImplementation
	implements OwnedTaskLogger {

	// state

	private final
	Optional <TaskLoggerImplementation> parentOptional;

	private final
	LogTarget logTarget;

	private final
	CharSequence staticContext;

	private final
	CharSequence dynamicContext;

	private final
	long nesting;

	private final
	Boolean debugEnabled;

	private final
	Instant startTime;

	private
	Instant endTime;

	LogSeverity severity =
		LogSeverity.trace;

	long errorCount;
	long warningCount;
	long noticeCount;
	long logicCount;
	long debugCount;

	boolean addedToParent;

	CharSequence firstError;
	CharSequence lastError = "Aborting";

	List <String> errorMessages =
		new ArrayList<> ();

	List <TaskLogEvent> events =
		new ArrayList<> ();

	// constructors

	public
	TaskLoggerImplementation (
			@NonNull Optional <TaskLoggerImplementation> parentOptional,
			@NonNull LogTarget logTarget,
			@NonNull CharSequence staticContext,
			@NonNull CharSequence dynamicContext,
			@NonNull Optional <Boolean> debugEnabled) {

		this.parentOptional =
			parentOptional;

		this.nesting =
			optionalMapRequiredOrDefault (
				parent ->
					parent.nesting + 1l,
				parentOptional,
				0l);

		this.logTarget =
			logTarget;

		this.staticContext =
			staticContext;

		this.dynamicContext =
			dynamicContext;

		if (
			optionalIsPresent (
				debugEnabled)
		) {

			this.debugEnabled =
				optionalGetRequired (
					debugEnabled);

		} else {

			this.debugEnabled =
				optionalMapRequiredOrDefault (
					TaskLoggerImplementation::debugEnabled,
					parentOptional,
					true);

		}

		if (this.debugEnabled) {

			addToParent ();

		}

		this.startTime =
			Instant.now ();

	}

	// life cycle

	@Override
	public
	void close () {

		if (
			isNotNull (
				this.endTime)
		) {
			return;
		}

		this.endTime =
			Instant.now ();

	}

	// accessors

	@SuppressWarnings ("resource")
	@Override
	public
	TaskLoggerImplementation findRoot () {

		TaskLoggerImplementation currentTaskLogger =
			this;

		while (
			optionalIsPresent (
				currentTaskLogger.parentOptional)
		) {

			currentTaskLogger =
				optionalGetRequired (
					currentTaskLogger.parentOptional);

		}

		return currentTaskLogger;

	}

	@Override
	public
	long errorCount () {

		return errorCount;

	}

	@Override
	public
	boolean errors () {

		return moreThanZero (
			errorCount);

	}

	@Override
	public
	void firstErrorFormat (
			@NonNull CharSequence ... arguments) {

		firstError =
			stringFormatLazyArray (
				arguments);

	}

	public
	void lastErrorFormat (
			@NonNull String ... arguments) {

		lastError =
			stringFormatArray (
				arguments);

	}

	public
	void addChild (
			@NonNull OwnedTaskLogger child) {

		events.add (
			child);

	}

	// implementation

	public
	void logFormat (
			@NonNull LogSeverity severity,
			@NonNull String ... arguments) {

		switch (severity) {

		case fatal:

			throw fatalFormat (
				arguments);

		case error:

			errorFormat (
				arguments);

			break;

		case warning:

			warningFormat (
				arguments);

			break;

		case notice:

			noticeFormat (
				arguments);

			break;

		case logic:

			logicFormat (
				arguments);

			break;

		case trace:

			debugFormat (
				arguments);

			break;

		case debug:

			debugFormat (
				arguments);

			break;

		}

	}

	@Override
	public
	FatalErrorException fatalFormat (
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.fatal,
			message,
			optionalAbsent ());

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.fatal,
				message));

		throw new FatalErrorException (
			this,
			message.toString ());

	}

	@Override
	public
	FatalErrorException fatalFormatException (
			@NonNull Throwable throwable,
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.fatal,
			message,
			optionalOf (
				throwable));

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.fatal,
				message));

		throw new FatalErrorException (
			this,
			message.toString (),
			throwable);

	}

	@Override
	public
	void errorFormat (
			@NonNull CharSequence ... arguments) {

		writeFirstError ();

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.error,
			message,
			optionalAbsent ());

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.error,
				message));

		increaseErrorCount ();

	}

	@Override
	public
	void errorFormatException (
			@NonNull Throwable throwable,
			@NonNull CharSequence ... arguments) {

		writeFirstError ();

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.error,
			message,
			optionalOf (
				throwable));

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.error,
				message));

		increaseErrorCount ();

	}

	@Override
	public
	void warningFormat (
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.warning,
			message,
			optionalAbsent ());

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.warning,
				message));

		increaseWarningCount ();

	}

	@Override
	public
	void warningFormatException (
			@NonNull Throwable throwable,
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.warning,
			message,
			optionalOf (
				throwable));

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.warning,
				message));

		increaseWarningCount ();

	}

	@Override
	public
	void noticeFormat (
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.notice,
			message,
			optionalAbsent ());

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.notice,
				message));

		increaseNoticeCount ();

	}

	@Override
	public
	void noticeFormatException (
			@NonNull Throwable throwable,
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.notice,
			message,
			optionalOf (
				throwable));

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.notice,
				message));

		increaseNoticeCount ();

	}

	@Override
	public
	void logicFormat (
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.logic,
			message,
			optionalAbsent ());

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.logic,
				message));

		increaseLogicCount ();

	}

	@Override
	public
	void debugFormat (
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.debug,
			message,
			optionalAbsent ());

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.debug,
				message));

		increaseDebugCount ();

	}

	@Override
	public
	void debugFormatException (
			@NonNull Throwable throwable,
			@NonNull CharSequence ... arguments) {

		LazyString message =
			stringFormatLazyArray (
				arguments);

		logTarget.writeToLog (
			this,
			LogSeverity.debug,
			message,
			optionalOf (
				throwable));

		events.add (
			new TaskLogEntryEvent (
				LogSeverity.debug,
				message));

		increaseDebugCount ();

	}

	@Override
	public
	RuntimeException makeException (
			@NonNull Supplier <RuntimeException> exceptionSupplier) {

		if (errors ()) {

			LazyString message =
				stringFormatLazy (
					"%s due to %s errors",
					lastError,
					integerToDecimalString (
						errorCount));

			logTarget.writeToLog (
				this,
				LogSeverity.error,
				message,
				optionalAbsent ());

			events.add (
				new TaskLogEntryEvent (
					LogSeverity.error,
					message));

			throw exceptionSupplier.get ();

		} else {

			return new RuntimeException (
				"No errors");

		}

	}

	@Override
	public
	RuntimeException makeException () {

		if (errors ()) {

			LazyString message =
				stringFormatLazy (
					"%s due to %s errors",
					lastError,
					integerToDecimalString (
						errorCount));

			logTarget.writeToLog (
				this,
				LogSeverity.error,
				message,
				optionalAbsent ());

			throw new LoggedErrorsException (
				this,
				message.toString ());

		} else {

			return new RuntimeException (
				"No errors");

		}

	}

	@Override
	public <Type>
	Type wrap (
			@NonNull Function <TaskLogger, Type> function) {

		Type returnValue =
			function.apply (
				this);

		makeException ();

		return returnValue;

	}

	@Override
	public
	void wrap (
			@NonNull Consumer <TaskLogger> function) {

		function.accept (
			this);

		makeException ();

	}

	// private implementation

	private
	void writeFirstError () {

		// recurse up through parents

		if (
			optionalIsPresent (
				parentOptional)
		) {

			parentOptional.get ().writeFirstError ();

		}

		if (

			equalToZero (
				errorCount)

			&& isNotNull (
				firstError)

		) {

			// add to parent if we didn't already

			addToParent ();

			// write first error

			logTarget.writeToLog (
				this,
				LogSeverity.error,
				firstError,
				optionalAbsent ());

			events.add (
				new TaskLogEntryEvent (
					LogSeverity.error,
					firstError));

		}

	}

	private
	void increaseErrorCount () {

		errorCount ++;

		optionalDo (
			parentOptional,
			TaskLoggerImplementation::increaseErrorCount);

	}

	private
	void increaseWarningCount () {

		warningCount ++;

		optionalDo (
			parentOptional,
			TaskLoggerImplementation::increaseWarningCount);

	}

	private
	void increaseNoticeCount () {

		noticeCount ++;

		optionalDo (
			parentOptional,
			TaskLoggerImplementation::increaseNoticeCount);

	}

	private
	void increaseLogicCount () {

		logicCount ++;

		optionalDo (
			parentOptional,
			TaskLoggerImplementation::increaseLogicCount);

	}

	private
	void increaseDebugCount () {

		debugCount ++;

		optionalDo (
			parentOptional,
			TaskLoggerImplementation::increaseDebugCount);

	}

	private
	void addToParent () {

		if (addedToParent) {
			return;
		}

		if (
			optionalIsPresent (
				parentOptional)
		) {

			parentOptional.get ().addChild (
				this);
		}

		addedToParent = true;

	}

	// task logger implementation

	@Override
	public
	boolean debugEnabled () {

		return (
			logTarget.debugEnabled ()
			|| debugEnabled
		);

	}

	// task log event implementation

	@Override
	public
	LogSeverity eventSeverity () {

		return severity;

	}

	@Override
	public
	String eventText () {

		return stringFormat (
			"%s.%s",
			staticContext,
			dynamicContext);

	}

	@Override
	public
	Instant eventStartTime () {

		return startTime;

	}

	@Override
	public
	Instant eventEndTime () {

		return endTime;

	}

	@Override
	public
	List <TaskLogEvent> eventChildren () {

		return ImmutableList.copyOf (
			events);

	}

	@Override
	public
	TaskLoggerImplementation taskLoggerImplementation () {

		return this;

	}

}
