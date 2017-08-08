package wbs.platform.daemon;

import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Instant;

import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@Accessors (fluent = true)
public
class RemovalDaemonWrapper <Type extends Record <Type>>
	extends SleepingDaemonService {

	// singleton dependencies

	@SingletonDependency
	Database database;

	// properties

	@Getter @Setter
	RemovalDaemon <Type> removalDaemon;

	// details

	@Override
	protected
	String friendlyName () {
		return removalDaemon.serviceName ();
	}

	@Override
	protected
	String backgroundProcessName () {
		return removalDaemon.backgroundProcessName ();
	}

	// public implementation

	@Override
	protected
	void runOnce (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				removalDaemon.logContext ().nestTaskLogger (
					parentTaskLogger,
					"runOnce");

		) {

			while (
				runBatch (
					taskLogger)
			) {

				try {

					Thread.sleep (
						removalDaemon.sleepTime ().getMillis ());

				} catch (InterruptedException interruptedException) {

					taskLogger.warningFormat (
						"Aborting due to interrupt");

					Thread.currentThread ().interrupt ();

					return;

				}

			}

		}

	}

	// private implementation

	private
	boolean runBatch (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					removalDaemon.logContext (),
					parentTaskLogger,
					"runBatch");

		) {

			Instant timestamp =
				transaction.now ().minus (
					removalDaemon.removalAge ());

			List <Type> items =
				removalDaemon.findItemsForRemoval (
					transaction,
					timestamp,
					removalDaemon.itemsPerBatch ());

			if (
				collectionIsEmpty (
					items)
			) {
				return false;
			}

			transaction.noticeFormat (
				"Removing %s old items",
				integerToDecimalString (
					collectionSize (
						items)));

			items.forEach (
				item ->
					removalDaemon.removeItem (
						transaction,
						item));

			transaction.commit ();

			return true;

		}

	}

}
