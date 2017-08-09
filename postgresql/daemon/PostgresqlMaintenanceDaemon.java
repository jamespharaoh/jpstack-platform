package wbs.platform.postgresql.daemon;

import static wbs.utils.etc.LogicUtils.ifThenElse;
import static wbs.utils.etc.NumberUtils.integerEqualSafe;
import static wbs.utils.etc.NumberUtils.notLessThan;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.string.StringUtils.keyEqualsString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.time.TimeUtils.earlierThan;
import static wbs.utils.time.TimeUtils.localTime;
import static wbs.utils.time.TimeUtils.notEarlierThan;
import static wbs.utils.time.TimeUtils.sleepUntil;
import static wbs.utils.time.TimeUtils.toInstant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.LoggingDataSource;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;

import wbs.platform.daemon.AbstractDaemonService;

import wbs.utils.thread.ThreadManager;

@SingletonComponent ("pgMaint")
public
class PostgresqlMaintenanceDaemon
	extends AbstractDaemonService {

	// singleton dependencies

	@SingletonDependency
	LoggingDataSource dataSource;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ThreadManager threadManager;

	// details

	final static
	long monthlyDay = 15;

	final static
	long weeklyDayOfWeek =
		DateTimeConstants.WEDNESDAY;

	final static
	long dailyHour = 5;

	final static
	long hourlyMinute = 30;

	@Override
	protected
	String friendlyName () {
		return "PostgreSQL maintenance";
	}

	@Override
	protected
	String getThreadName () {
		return "PgMaint";
	}

	private
	String frequencyName (
			String frequency) {

		if (frequency.equals ("m"))
			return "monthly";

		if (frequency.equals ("w"))
			return "weekly";

		if (frequency.equals ("d"))
			return "daily";

		if (frequency.equals ("h"))
			return "hourly";

		if (frequency.equals ("5"))
			return "five-minute";

		if (frequency.equals ("1"))
			return "one-minute";

		return frequency;

	}

	private
	void doTasks (
			@NonNull String frequency) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.createTaskLogger (
					"doTasks",
					keyEqualsString (
						"frequency",
						frequency));

		) {

			String frequencyName =
				frequencyName (
					frequency);

			taskLogger.noticeFormat (
				"%s maintenance starting",
				frequencyName);

			try (

				Connection connnection =
					dataSource.getConnection (
						taskLogger);

			) {

				connnection.setTransactionIsolation (
					Connection.TRANSACTION_SERIALIZABLE);

				connnection.setAutoCommit (
					true);

				// get list of commands

				List <Command> commands =
					fetchCommands (
						connnection,
						frequency);

				// prepare the update

				String updateQuery =
					"UPDATE postgresql_maintenance " +
					"SET last_run = ?, " +
						"last_duration = ?, " +
						"last_output = ? " +
					"WHERE id = ?";

				try (

					PreparedStatement updateStatement =
						connnection.prepareStatement (
							updateQuery);

					Statement statement =
						connnection.createStatement ();

				) {

					// for each command...

					for (
						Command command
							: commands
					) {

						// output a pretty message

						taskLogger.debugFormat (
							"executing \"%s\"",
							command.command);

						try {

							// perform (and time) the command

							Instant time1 =
								Instant.now ();

							statement.execute (
								command.command);

							Instant time2 =
								Instant.now ();

							// collect output

							StringBuilder stringBuilder =
								new StringBuilder ();

							SQLWarning warning =
								statement.getWarnings ();

							while (warning != null) {

								if (warning.toString () != null) {

									if (stringBuilder.length () > 0)
										stringBuilder.append ("\n");

									stringBuilder.append (
										warning.toString ());

								}

								warning =
									warning.getNextWarning ();

							}

							statement.clearWarnings ();

							// and update the db

							updateStatement.setTimestamp (
								1,
								new Timestamp (
									time1.getMillis ()));

							updateStatement.setLong (
								2,
								time2.getMillis () - time1.getMillis ());

							updateStatement.setString (
								3,
								stringBuilder.toString ());

							updateStatement.setInt (
								4,
								command.id);

							updateStatement.execute ();

						} catch (Exception exception) {

							exceptionLogger.logThrowable (
								taskLogger,
								"daemon",
								getClass ().getSimpleName (),
								exception,
								optionalAbsent (),
								GenericExceptionResolution.tryAgainLater);

						}

					}

				}

			} catch (Exception exception) {

				exceptionLogger.logThrowable (
					taskLogger,
					"daemon",
					getClass ().getSimpleName (),
					exception,
					optionalAbsent (),
					GenericExceptionResolution.tryAgainLater);

			}

			taskLogger.noticeFormat (
				"%s maintenance complete",
				frequencyName);

		}

	}

	List <Command> fetchCommands (
			@NonNull Connection connection,
			@NonNull String frequency)
		throws SQLException {

		List <Command> commands =
			new ArrayList<> ();

		try (

			PreparedStatement preparedStatement =
				connection.prepareStatement (
					stringFormat (
						"SELECT id, command ",
						"FROM postgresql_maintenance ",
						"WHERE frequency = ? ",
						"ORDER BY sequence, command"));

		) {

			preparedStatement.setString (
				1,
				frequency);

			try (

				ResultSet resultSet =
					preparedStatement.executeQuery ();

			) {

				while (resultSet.next ()) {

					Command command =
						new Command ();

					command.id =
						resultSet.getInt (1);

					command.command =
						resultSet.getString (2);

					commands.add (
						command);

				}

				return commands;

			}

		}

	}

	Instant nextDaily;
	Instant nextHourly;
	Instant nextFiveMinutes;
	Instant nextOneMinute;

	@Override
	public
	void runService () {

		calculateTimes ();

		while (true) {

			try {
				waitNext ();
			} catch (InterruptedException exception) {
				return;
			}

			doStuff ();

		}

	}

	void doStuff () {

		DateTime now =
			DateTime.now ();

		if (
			notEarlierThan (
				now,
				nextDaily)
		) {

			if (
				integerEqualSafe (
					now.getDayOfMonth (),
					monthlyDay)
			) {

				doTasks ("m");

			} else if (
				integerEqualSafe (
					now.getDayOfWeek (),
					weeklyDayOfWeek)
			) {

				doTasks ("w");

			} else {

				doTasks ("d");

			}

			calcDaily (
				now);

		}

		if (
			notEarlierThan (
				now,
				nextHourly)
		) {

			doTasks ("h");

			calcHourly (
				now);

		}

		if (
			notEarlierThan (
				now,
				nextFiveMinutes)
		) {

			doTasks ("5");

			calcFiveMins (
				now);

		}

		if (
			notEarlierThan (
				now,
				nextOneMinute)
		) {

			doTasks ("1");

			calcOneMin (
				now);

		}

	}

	void waitNext ()
		throws InterruptedException {

		Instant nextTime =
			nextDaily;

		if (
			earlierThan (
				nextHourly,
				nextTime)
		) {

			nextTime =
				nextHourly;

		}

		if (
			earlierThan (
				nextFiveMinutes,
				nextTime)
		) {

			nextTime =
				nextFiveMinutes;

		}

		if (
			earlierThan (
				nextOneMinute,
				nextTime)
		) {

			nextTime =
				nextOneMinute;

		}

		Instant now =
			Instant.now ();

		if (
			earlierThan (
				now,
				nextTime)
		) {

			sleepUntil (
				nextTime);

		}

	}

	void calculateTimes () {

		DateTime now =
			DateTime.now ();

		calcDaily (
			now);

		calcHourly (
			now);

		calcFiveMins (
			now);

		calcOneMin (
			now);

	}

	void calcDaily (
			@NonNull DateTime originalDailyTime) {

		DateTime actualDailyTime =
			ifThenElse (
				notLessThan (
					originalDailyTime.getHourOfDay (),
					dailyHour),
				() -> originalDailyTime.plusDays (1),
				() -> originalDailyTime);

		nextDaily =
			toInstant (
				actualDailyTime.withTime (
					localTime (
						dailyHour,
						0,
						0,
						0)));

	}

	void calcHourly (
			@NonNull DateTime originalHourlyTime) {

		DateTime actualHourlyTime =
			ifThenElse (
				notLessThan (
					originalHourlyTime.getMinuteOfHour (),
					hourlyMinute),
				() -> originalHourlyTime.plusHours (1),
				() -> originalHourlyTime);

		nextHourly =
			toInstant (
				actualHourlyTime.withTime (
					localTime (
						actualHourlyTime.getHourOfDay (),
						hourlyMinute,
						0,
						0)));

	}

	void calcFiveMins (
			@NonNull DateTime now) {

		DateTime nextFiveMinsTemp =
			now.plusMinutes (
				5);

		nextFiveMinutes =
			toInstant (
				nextFiveMinsTemp

			.minusMinutes (
				nextFiveMinsTemp.getMinuteOfHour () % 5)

			.withSecondOfMinute (
				0)

			.withMillisOfSecond (
				0)

		);

	}

	void calcOneMin (
			@NonNull DateTime now) {

		nextOneMinute =
			toInstant (
				now

			.plusMinutes (
				1)

			.withSecondOfMinute (
				0)

			.withMillisOfSecond (
				0)

		);

	}

	static
	class Command {
		int id;
		String command;
	}

}