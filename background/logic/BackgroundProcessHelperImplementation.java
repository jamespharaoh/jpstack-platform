package wbs.platform.background.logic;

import static wbs.utils.etc.EnumUtils.enumName;
import static wbs.utils.etc.LogicUtils.booleanNotEqual;
import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.string.FormatWriterUtils.formatWriterConsumerToString;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.joinWithoutSeparator;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;
import static wbs.utils.time.TimeUtils.earlierThan;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Duration;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogEvent;
import wbs.framework.logging.TaskLogger;

import wbs.platform.background.model.BackgroundProcessObjectHelper;
import wbs.platform.background.model.BackgroundProcessRec;

import wbs.utils.random.RandomLogic;
import wbs.utils.string.FormatWriter;

@PrototypeComponent ("backgroundProcessHelper")
@Accessors (fluent = true)
public
class BackgroundProcessHelperImplementation
	implements BackgroundProcessHelper {

	// singleton dependencies

	@SingletonDependency
	BackgroundProcessObjectHelper backgroundProcessHelper;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RandomLogic randomLogic;

	// properties

	@Getter @Setter
	String parentTypeCode;

	@Getter @Setter
	String backgroundProcessCode;

	@Getter @Setter
	Long backgroundProcessId;

	@Getter @Setter
	Duration backgroundProcessFrequency;

	@Getter @Setter
	Duration backgroundProcessFrequencyVariance;

	@Getter @Setter
	Boolean backgroundProcessDebugEnabled;

	// state

	String runningToken;

	// accessors

	@Override
	public
	Boolean debugEnabled () {
		return backgroundProcessDebugEnabled;
	}

	@Override
	public
	Duration frequency () {
		return backgroundProcessFrequency;
	}

	@Override
	public
	Duration frequencyVariance () {
		return backgroundProcessFrequencyVariance;
	}

	// implementation

	@Override
	public
	String threadName () {

		return joinWithoutSeparator (
			hyphenToCamel (
				parentTypeCode),
			capitalise (
				hyphenToCamel (
					backgroundProcessCode)));

	}

	@Override
	public
	boolean setBackgroundProcessStart (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"setBackgroundProcessStart");

		) {

			BackgroundProcessRec backgroundProcess =
				backgroundProcessHelper.findRequired (
					transaction,
					backgroundProcessId);

			if (
				booleanNotEqual (
					backgroundProcess.getDebug (),
					backgroundProcessDebugEnabled)
			) {

				backgroundProcessDebugEnabled =
					backgroundProcess.getDebug ();

				return false;

			}

			if (

				backgroundProcess.getRunning ()

				&& earlierThan (
					transaction.now ().minus (
						Duration.standardMinutes (10)),
					backgroundProcess.getRunningWatchdogTime ())

			) {
				return false;
			}

			runningToken =
				randomLogic.generateLowercase (10);

			backgroundProcess

				.setRunning (
					true)

				.setRunningToken (
					runningToken)

				.setRunningStartTime (
					transaction.now ())

				.setRunningWatchdogTime (
					transaction.now ())

			;

			transaction.commit ();

			return true;

		}

	}

	@Override
	public
	void setBackgroundProcessStop (
			@NonNull TaskLogger parentTaskLogger) {

		if (
			isNull (
				runningToken)
		) {
			return;
		}

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"setBackgroundProcessStop");

		) {

			BackgroundProcessRec backgroundProcess =
				backgroundProcessHelper.findRequired (
					transaction,
					backgroundProcessId);

			if (

				! backgroundProcess.getRunning ()

				|| stringNotEqualSafe (
					backgroundProcess.getRunningToken (),
					runningToken)

			) {
				return;
			}

			backgroundProcessFrequency =
				backgroundProcess.getFrequency ();

			backgroundProcessFrequencyVariance =
				backgroundProcess.getFrequency ().dividedBy (4l);

			backgroundProcess

				.setNumConsecutiveFailures (
					ifThenElse (
						transaction.getRoot ().errors (),
						() -> 1l +
							backgroundProcess.getNumConsecutiveFailures (),
						() -> 0l))

				.setLastRunTime (
					backgroundProcess.getRunningStartTime ())

				.setLastRunSuccess (
					! transaction.getRoot ().errors ())

				.setLastRunDuration (
					new Duration (
						backgroundProcess.getRunningStartTime (),
						transaction.now ()))

				.setLastTaskLog (
					taskLog (
						transaction.getRoot ()))

				.setRunning (
					false)

				.setRunningToken (
					null)

				.setRunningStartTime (
					null)

				.setRunningWatchdogTime (
					null)

			;

			transaction.commit ();

		} finally {

			runningToken = null;

		}

	}

	@Override
	public
	Duration calculateFirstDelay () {

		return randomLogic.randomDuration (
			backgroundProcessFrequency);

	}

	@Override
	public
	Duration calculateSubsequentDelay () {

		return randomLogic.randomDuration (
			backgroundProcessFrequency,
			backgroundProcessFrequencyVariance);

	}

	// private implementation

	private
	String taskLog (
			@NonNull TaskLogEvent taskLogEvent) {

		return formatWriterConsumerToString (
			"  ",
			formatWriter ->
				writeTaskLog (
					formatWriter,
					taskLogEvent));

	}

	private
	void writeTaskLog (
			@NonNull FormatWriter formatWriter,
			@NonNull TaskLogEvent taskLogEvent) {

		formatWriter.writeLineFormatIncreaseIndent (
			"%s %s",
			enumName (
				taskLogEvent.eventSeverity ()),
			taskLogEvent.eventText ());

		taskLogEvent.eventChildren ().forEach (
			childEvent ->
				writeTaskLog (
					formatWriter,
					childEvent));

		formatWriter.decreaseIndent ();

	}

}
