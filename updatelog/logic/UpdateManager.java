package wbs.platform.updatelog.logic;

import static wbs.utils.etc.NumberUtils.integerEqualSafe;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.keyEqualsDecimalInteger;
import static wbs.utils.string.StringUtils.keyEqualsString;
import static wbs.utils.time.TimeUtils.earlierThan;
import static wbs.utils.time.TimeUtils.instantSumDuration;
import static wbs.utils.time.TimeUtils.millisToInstant;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.updatelog.model.UpdateLogObjectHelper;
import wbs.platform.updatelog.model.UpdateLogRec;

/**
 * Provides an efficient signalling system which works using the database.
 * Signals are categorised according to a name, and a reference integer (eg a
 * hash code). To cause a signal use signalUpdate (...). To receive signals
 * create a Watcher using makeWatcher (...) then call isUpdated (...) to check
 * for changes. This always returns true the first time for a given ref.
 *
 * The idea is that you can cache large chunks of a table and, so long as all
 * updaters are using the signalling system, ensure that the cache is always
 * kept up to date.
 *
 * The underlying mechanism is efficient requiring (assuming no updates) only a
 * single database hit for a single row, every few seconds (this time period is
 * configurable). On update three rows must be looked up and updated/created,
 * then an extra hit is needed for each named category being watched and every
 * unique ref in the affected named category being watched.
 *
 * TODO is this really the best way to do this?
 */
@SingletonComponent ("updateManager")
public
class UpdateManager {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UpdateLogObjectHelper updateLogHelper;

	// properties

	@Getter @Setter
	Duration reloadFrequency =
		Duration.standardSeconds (
			1l);

	Instant reloadTime =
		millisToInstant (0);

	long masterVersion = -2;

	Map <String, UpdateStuff> secondaryVersions =
		new HashMap<> ();

	Map <String, Map <Long, UpdateStuff>> tertiaryVersions =
		new HashMap<> ();

	private
	long getDatabaseVersion (
			@NonNull Transaction parentTransaction,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getDatabaseVersion");

		) {

			UpdateLogRec updateLog =
				updateLogHelper.findByTableAndRef (
					transaction,
					table,
					ref);

			long ret =
				updateLog != null
					? updateLog.getVersion ()
					: -1l;

			transaction.debugFormat (
				"getDatabaseVersion (\"%s\", %s) = %s",
				table,
				integerToDecimalString (
					ref),
				integerToDecimalString (
					ret));

			return ret;

		}

	}

	private
	void refreshMaster (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"refreshMaster");

		) {

			// check it is time

			if (
				earlierThan (
					transaction.now (),
					reloadTime)
			) {
				return;
			}

			reloadTime =
				instantSumDuration (
					transaction.now (),
					reloadFrequency);

			// hit the db

			long newMasterVersion =
				getDatabaseVersion (
					transaction,
					"master",
					0l);

			// if the version hasn't changed don't bother

			if (
				integerEqualSafe (
					masterVersion,
					newMasterVersion)
			) {
				return;
			}


			// remember the new version

			masterVersion =
				newMasterVersion;

			// markall secondary versions as dirty

			for (
				UpdateStuff updateStuff
					: secondaryVersions.values ()
			) {

				updateStuff.dirty = true;

			}

		}

	}

	private
	void refreshSecondary (
			@NonNull Transaction parentTransaction,
			@NonNull String table) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"refreshSecondary");

		) {

			// if it isn't marked dirty don't bother

			UpdateStuff stuff1 =
				secondaryVersions.get (table);

			if (stuff1 == null) {

				stuff1 =
					new UpdateStuff ();

				stuff1.version = -2;
				stuff1.dirty = true;

				secondaryVersions.put (
					table,
					stuff1);

			}

			if (! stuff1.dirty)
				return;

			stuff1.dirty = false;

			// hit the db

			long newSecondaryVersion =
				getDatabaseVersion (
					transaction,
					table,
					0l);

			// if the version hasn't changed don't bother

			if (
				integerEqualSafe (
					stuff1.version,
					newSecondaryVersion)
			) {
				return;
			}

			// remember the new version

			stuff1.version =
				newSecondaryVersion;

			// mark all this table's tertiary versions as dirty

			Map<Long,UpdateStuff> map =
				tertiaryVersions.get (
					table);

			if (map == null)
				return;

			for (UpdateStuff stuff2 : map.values ())
				stuff2.dirty = true;

		}

	}

	public
	long refreshTertiary (
			@NonNull Transaction parentTransaction,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"refreshTertiary");

		) {

			// if it's not dirty don't bother

			Map <Long, UpdateStuff> map =
				tertiaryVersions.get (
					table);

			if (map == null) {

				map =
					new HashMap<Long,UpdateStuff> ();

				tertiaryVersions.put (
					table,
					map);

			}

			UpdateStuff stuff =
				map.get (ref);

			if (stuff == null) {

				stuff =
					new UpdateStuff ();

				stuff.version = -2;
				stuff.dirty = true;

				map.put (
					ref,
					stuff);

			}

			if (! stuff.dirty)
				return stuff.version;

			stuff.dirty = false;

			// hit the db

			long newTertiaryVersion =
				getDatabaseVersion (
					transaction,
					table,
					ref);

			// if the version hasn't changed don't bother

			if (
				integerEqualSafe (
					stuff.version,
					newTertiaryVersion)
			) {
				return stuff.version;
			}

			// remember the new version

			return stuff.version =
				newTertiaryVersion;

		}

	}

	public
	long getVersionDb (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"UpdateManager.getVersionDb",
					keyEqualsString (
						"table",
						table),
					keyEqualsDecimalInteger (
						"ref",
						ref));

		) {

			refreshMaster (
				transaction);

			refreshSecondary (
				transaction,
				table);

			refreshTertiary (
				transaction,
				table,
				ref);

			Map <Long, UpdateStuff> map =
				tertiaryVersions.get (
					table);

			UpdateStuff stuff2 =
				map.get (ref);

			return stuff2.version;

		}

	}

	public synchronized
	long getVersion (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"getVersion");

		) {

			// check master

			Instant now =
				Instant.now ();

			if (
				earlierThan (
					reloadTime,
					now)
			) {

				return getVersionDb (
					taskLogger,
					table,
					ref);

			}

			// check secondary

			UpdateStuff stuff1 =
				secondaryVersions.get (
					table);

			if (stuff1 == null || stuff1.dirty) {

				return getVersionDb (
					taskLogger,
					table,
					ref);

			}

			// check tertiary

			Map <Long, UpdateStuff> map =
				tertiaryVersions.get (
					table);

			if (map == null) {

				return getVersionDb (
					taskLogger,
					table,
					ref);

			}

			UpdateStuff stuff2 =
				map.get (ref);

			if (stuff2 == null || stuff2.dirty) {

				return getVersionDb (
					taskLogger,
					table,
					ref);

			}

			return stuff2.version;

		}

	}

	private
	void realSignalUpdate (
			@NonNull Transaction parentTransaction,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"realSignalUpdate");

		) {

			UpdateLogRec updateLog =
				updateLogHelper.findByTableAndRef (
					transaction,
					table,
					ref);

			if (updateLog == null) {

				updateLog =
					updateLogHelper.insert (
						transaction,
						updateLogHelper.createInstance ()

					.setCode (
						table)

					.setRef (
						ref)

					.setVersion (
						0l)

				);

			}

			updateLog.setVersion (
				updateLog.getVersion () + 1);

		}

	}

	public
	void signalUpdate (
			@NonNull Transaction parentTransaction,
			@NonNull String table,
			@NonNull Long ref) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"signalUpdate");

		) {

			realSignalUpdate (
				transaction,
				table,
				ref);

			realSignalUpdate (
				transaction,
				table,
				0l);

			realSignalUpdate (
				transaction,
				"master",
				0l);

		}

	}

	public
	Watcher makeWatcher (
			@NonNull String table) {

		return new Watcher (
			table);

	}

	static
	class UpdateStuff {

		long version;
		boolean dirty;

	}

	public
	class Watcher {

		String table;

		Map<Long,Long> versions =
			new HashMap<> ();

		Watcher (
				String newTable) {

			table = newTable;

		}

		public
		boolean isUpdated (
				@NonNull TaskLogger parentTaskLogger,
				@NonNull Long ref) {

			try (

				OwnedTaskLogger taskLogger =
					logContext.nestTaskLogger (
						parentTaskLogger,
						"Watcher.isUpdated",
						keyEqualsString (
							"table",
							table),
						keyEqualsDecimalInteger (
							"ref",
							ref));

			) {

				long newVersion =
					getVersion (
						taskLogger,
						table,
						ref);

				if (versions.containsKey (ref)) {

					long oldVersion =
						versions.get (ref);

					if (oldVersion == newVersion)
						return false;

				}

				versions.put (
					ref,
					newVersion);

				return true;

			}

		}

	}

	public <Type>
	UpdateGetter <Type> makeUpdateGetterAdaptor (
			ComponentProvider <? extends Type> getter,
			long reloadTimeMs,
			String table,
			long ref) {

		return new UpdateGetterAdaptor <Type> (
			getter,
			reloadTimeMs,
			table,
			ref);

	}

	public static
	interface UpdateGetter <T>
		extends ComponentProvider <T> {

		void forceUpdate ();

	}

	private
	class UpdateGetterAdaptor <T>
		implements UpdateGetter <T> {

		ComponentProvider <? extends T> getter;
		long reloadTimeMs;
		String table;
		long ref;

		boolean forceUpdate = false;

		@Override
		public synchronized
		void forceUpdate () {
			forceUpdate = true;
		}

		public
		UpdateGetterAdaptor (
				ComponentProvider <? extends T> newGetter,
				long newReloadTimeMs,
				String newTable,
				long newRef) {

			getter = newGetter;
			reloadTimeMs = newReloadTimeMs;
			table = newTable;
			ref = newRef;

		}

		T value;
		long lastReload = 0;
		long oldVersion = -1;

		@Override
		public synchronized
		T provide (
				@NonNull TaskLogger parentTaskLogger) {

			try (

				OwnedTaskLogger taskLogger =
					logContext.nestTaskLogger (
						parentTaskLogger,
						"UpdateGetterAdaptor.provide");

			) {

				long now =
					System.currentTimeMillis ();

				// check for a forced update

				if (forceUpdate) {

					value =
						getter.provide (
							parentTaskLogger);

					lastReload = now;
					forceUpdate = false;

					return value;

				}

				// check for an update trigger

				long newVersion =
					getVersion (
						taskLogger,
						table,
						ref);

				if (oldVersion != newVersion) {

					value =
						getter.provide (
							parentTaskLogger);

					lastReload = now;
					oldVersion = newVersion;

					return value;

				}

				// if not check for a timed update

				if (lastReload + reloadTimeMs < now) {

					value =
						getter.provide (
							parentTaskLogger);

					lastReload = now;
					oldVersion = newVersion;

					return value;

				}

				// or just return the cached value

				return value;

			}

		}

	}

}
