package wbs.platform.user.console;

import static wbs.utils.collection.MapUtils.mapItemForKey;
import static wbs.utils.collection.MapUtils.mapItemForKeyRequired;
import static wbs.utils.etc.Misc.hashSha1Base64;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.parseInteger;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalMapRequired;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.optionalValueNotEqualWithClass;
import static wbs.utils.string.StringUtils.joinWithFullStop;
import static wbs.utils.string.StringUtils.keyEqualsDecimalInteger;
import static wbs.utils.string.StringUtils.keyEqualsString;
import static wbs.utils.string.StringUtils.keyEqualsYesNo;
import static wbs.utils.string.StringUtils.objectToString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;
import static wbs.utils.time.TimeUtils.earlierThan;
import static wbs.utils.time.TimeUtils.millisToInstant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import org.joda.time.Duration;
import org.joda.time.Instant;

import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.text.console.TextConsoleHelper;
import wbs.platform.user.model.UserDataRec;
import wbs.platform.user.model.UserOnlineRec;
import wbs.platform.user.model.UserRec;
import wbs.platform.user.model.UserSessionRec;

import wbs.utils.random.RandomLogic;

@SingletonComponent ("userSessionLogic")
public
class UserSessionLogicImplementation
	implements UserSessionLogic {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	RandomLogic randomLogic;

	@SingletonDependency
	TextConsoleHelper textHelper;

	@SingletonDependency
	UserDataConsoleHelper userDataHelper;

	@SingletonDependency
	UserConsoleHelper userHelper;

	@SingletonDependency
	UserOnlineConsoleHelper userOnlineHelper;

	@SingletonDependency
	UserSessionConsoleHelper userSessionHelper;

	@SingletonDependency
	WbsConfig wbsConfig;

	// state

	Instant nextReload =
		millisToInstant (0);

	Map <Long, String> onlineSessionIdsByUserId =
		new HashMap<> ();

	Map <String, Instant> activeSessions =
		new HashMap<> ();

	// implementation

	@Override
	public
	UserSessionRec userLogon (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull UserRec user,
			@NonNull Optional <String> userAgent,
			@NonNull Optional <String> consoleDeploymentCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userLogon");

		) {

			// end any existing session

			userLogoff (
				transaction,
				user);

			// start the session log

			UserSessionRec session =
				userSessionHelper.insert (
					transaction,
					userSessionHelper.createInstance ()

				.setUser (
					user)

				.setStartTime (
					transaction.now ())

				.setUserAgent (
					textHelper.findOrCreate (
						transaction,
						userAgent.orNull ()))

			);

			// go online

			UserOnlineRec userOnline =
				userOnlineHelper.insert (
					transaction,
					userOnlineHelper.createInstance ()

				.setUser (
					user)

				.setSessionId (
					randomLogic.generateLowercase (
						20))

				.setTimestamp (
					transaction.now ())

				.setUserSession (
					session)

			);

			// create cookies

			requestContext.cookieSet (
				sessionIdCookieName,
				userOnline.getSessionId ());

			requestContext.cookieSet (
				userIdCookieName,
				integerToDecimalString (
					user.getId ()));

			// return

			return session;

		}

	}

	@Override
	public
	void userLogoff (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userLogoff");

		) {

			Optional <UserOnlineRec> userOnlineOptional =
				userOnlineHelper.find (
					transaction,
					user.getId ());

			if (
				optionalIsNotPresent (
					userOnlineOptional)
			) {
				return;
			}

			UserOnlineRec userOnline =
				userOnlineOptional.get ();

			// end the session log

			UserSessionRec userSession =
				userOnline.getUserSession ();

			if (userSession != null) {

				userSession.setEndTime (
					userOnline.getTimestamp ());

			}

			// go offline

			userOnlineHelper.remove (
				transaction,
				userOnline);

		}

	}

	@Override
	public
	Optional <UserSessionRec> userLogonTry (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull String sliceCode,
			@NonNull String username,
			@NonNull String password,
			@NonNull Optional <String> userAgent,
			@NonNull Optional <String> consoleDeploymentCode) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userLogonTry");

		) {

			// lookup the user

			Optional <UserRec> userOptional =
				userHelper.findByCode (
					transaction,
					GlobalId.root,
					sliceCode,
					username);

			if (
				optionalIsNotPresent (
					userOptional)
			) {
				return optionalAbsent ();
			}

			UserRec user =
				userOptional.get ();

			// check password

			if (
				! checkPassword (
					user,
					password)
			) {

				return optionalAbsent ();

			}

			// update user (bring online)

			UserSessionRec userSession =
				userLogon (
					transaction,
					requestContext,
					user,
					userAgent,
					consoleDeploymentCode);

			// and return

			return optionalOf (
				userSession);

		}

	}

	@Override
	public synchronized
	boolean userSessionVerify (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String sessionId,
			@NonNull Long userId,
			@NonNull Boolean forceReload) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"userSessionVerify",
					keyEqualsString (
						"sessionId",
						sessionId),
					keyEqualsDecimalInteger (
						"userId",
						userId),
					keyEqualsYesNo (
						"forceReload",
						forceReload));

		) {

			Instant now =
				Instant.now ();

			// reload if overdue or not done yet, always for root path

			boolean reloaded = false;

			if (

				forceReload

				|| earlierThan (
					nextReload,
					now)

			) {

				taskLogger.debugFormat (
					"Reloading");

				reload (
					taskLogger);

				reloaded = true;

			}

			// check his session is valid, reload if it doesn't look right still

			if (
				optionalValueNotEqualWithClass (
					String.class,
					mapItemForKey (
						onlineSessionIdsByUserId,
						userId),
					sessionId)
			) {

				taskLogger.debugFormat (
					"Session not found in cache (1)");

				if (reloaded) {
					return false;
				}

				taskLogger.debugFormat (
					"Reloading");

				reload (
					taskLogger);

				if (
					optionalValueNotEqualWithClass (
						String.class,
						mapItemForKey (
							onlineSessionIdsByUserId,
							userId),
						sessionId)
				) {

					taskLogger.debugFormat (
						"Session not found in cache (2)");

					return false;

				}

			}

			// update his timestamp next time round

			taskLogger.debugFormat (
				"Session ok");

			activeSessions.put (
				sessionId,
				now);

			return true;

		}

	}

	@Override
	public
	boolean userSessionVerify (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userSessionVerify");

		) {

			// check the cookies are present

			if (

				optionalIsNotPresent (
					requestContext.cookie (
						sessionIdCookieName))

				|| optionalIsNotPresent (
					requestContext.cookie (
						userIdCookieName))

			) {

				transaction.debugFormat (
					"Cookies not found");

				return false;

			}

			String sessionId =
				requestContext.cookieRequired (
					sessionIdCookieName);

			Optional <Long> userIdOptional =
				parseInteger (
					requestContext.cookieRequired (
						userIdCookieName));

			if (
				optionalIsNotPresent (
					userIdOptional)
			) {

				transaction.debugFormat (
					"User id is not valid");

				return false;

			}

			Long userId =
				optionalGetRequired (
					userIdOptional);

			Boolean forceReload =
				stringEqualSafe (
					requestContext.servletPath (),
					"/");

			Boolean result =
				userSessionVerify (
					transaction,
					sessionId,
					userId,
					forceReload);

			if (! result) {

				transaction.noticeFormat (
					"Removing session cookies for user %s",
					integerToDecimalString (
						userId));

				requestContext.cookieUnset (
					sessionIdCookieName);

				requestContext.cookieUnset (
					userIdCookieName);

			}

			return result;

		}

	}

	// private implementation

	private synchronized
	void reload (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"reloadReal");

		) {

			for (
				long attempt = 0l;
				attempt < 16l;
				attempt ++
			) {

				try {

					reloadReal (
						taskLogger);

					return;

				} catch (Exception reloadException) {

					try {

						Thread.sleep (
							attempt);

					} catch (InterruptedException interruptedException) {

						throw new RuntimeException (
							interruptedException);

					}

				}

			}

			// one last try

			reloadReal (
				taskLogger);

		}

	}

	@Override
	public
	Optional <byte[]> userData (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		return optionalMapRequired (
			userDataHelper.findByCode (
				parentTransaction,
				user,
				code),
			UserDataRec::getData);

	}

	@Override
	public
	void userDataStore (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code,
			@NonNull byte[] value) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userDataStore");

		) {

			Optional <UserDataRec> existingUserDataOptional =
				userDataHelper.findByCode (
					transaction,
					user,
					code);

			if (
				optionalIsPresent (
					existingUserDataOptional)
			) {

				transaction.noticeFormat (
					"Update %s.%s.%s",
					user.getSlice ().getCode (),
					user.getUsername (),
					code);

				optionalGetRequired (
					existingUserDataOptional)

					.setCreatedTime (
						transaction.now ())

					.setExpiryTime (
						transaction.now ().plus (
							Duration.standardDays (
								1l)))

					.setData (
						value)

				;

			} else {

				transaction.noticeFormat (
					"Create %s.%s.%s",
					user.getSlice ().getCode (),
					user.getUsername (),
					code);

				userDataHelper.insert (
					transaction,
					userDataHelper.createInstance ()

					.setUser (
						user)

					.setCode (
						code)

					.setCreatedTime (
						transaction.now ())

					.setExpiryTime (
						transaction.now ().plus (
							Duration.standardDays (
								1l)))

					.setData (
						value)

				);

			}

		}

	}

	@Override
	public
	void userDataRemove (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userDataRemove");

		) {

			Optional <UserDataRec> userDataOptional =
				userDataHelper.findByCode (
					transaction,
					user,
					code);

			if (
				optionalIsPresent (
					userDataOptional)
			) {

				transaction.noticeFormat (
					"Remove %s.%s.%s",
					user.getSlice ().getCode (),
					user.getUsername (),
					code);

				userDataHelper.remove (
					transaction,
					optionalGetRequired (
						userDataOptional));

			} else {

				transaction.noticeFormat (
					"Remove %s.%s.%s (did not exist)",
					user.getSlice ().getCode (),
					user.getUsername (),
					code);

			}

		}

	}

	@Override
	public
	Optional <Serializable> userDataObject (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"userDataObject");

		) {

			try {

				return optionalMapRequired (
					userData (
						transaction,
						user,
						code),
					SerializationUtils::deserialize);

			} catch (SerializationException serializationException) {

				transaction.warningFormatException (
					serializationException,
					"Error deserializing user data %s.%s.%s",
					user.getSlice ().getCode (),
					user.getUsername (),
					code);

				return optionalAbsent ();

			}

		}

	}

	@Override
	public
	void userDataObjectStore (
			@NonNull Transaction parentTransaction,
			@NonNull UserRec user,
			@NonNull String code,
			@NonNull Serializable value) {

		userDataStore (
			parentTransaction,
			user,
			code,
			SerializationUtils.serialize (
				value));

	}

	// private implementation

	private synchronized
	void reloadReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"reloadReal");

		) {

			onlineSessionIdsByUserId =
				new HashMap <> ();

			for (
				UserOnlineRec online
					: userOnlineHelper.findAll (
						transaction)
			) {

				UserRec user =
					online.getUser ();

				// update his timestamp if appropriate

				if (
					activeSessions.containsKey (
						online.getSessionId ())
				) {

					Instant activeTimestamp =
						mapItemForKeyRequired (
							activeSessions,
							online.getSessionId ());

					if (
						earlierThan (
							online.getTimestamp (),
							activeTimestamp)
					) {

						transaction.debugFormat (
							"Update session timestamp for user %s ",
							integerToDecimalString (
								user.getId ()),
							"to %s",
							objectToString (
								activeTimestamp));

						online

							.setTimestamp (
								activeSessions.get (
									online.getSessionId ()));

					}

				}

				// log off inactive users

				if (! user.getActive ()) {

					transaction.noticeFormat (
						"Log off disabled user %s",
						integerToDecimalString (
							user.getId ()));

					userLogoff (
						transaction,
						user);

					continue;

				}

				// log off users without a recent update

				if (
					earlierThan (
						online.getTimestamp ().plus (
							logoffTime),
						transaction.now ())
				) {

					transaction.noticeFormat (
						"Log off user %s ",
						integerToDecimalString (
							user.getId ()),
						"with session timestamp %s ",
						objectToString (
							online.getTimestamp ()));

					userLogoff (
						transaction,
						user);

					continue;

				}

				// ok put him in the ok list

				onlineSessionIdsByUserId.put (
					user.getId (),
					online.getSessionId ());

			}

			transaction.commit ();

			activeSessions =
				new HashMap<> ();

		}

		nextReload =
			Instant.now ().plus (
				reloadFrequency.getMillis ()
				- reloadFrequencyDeviation.getMillis ()
				+ randomLogic.randomInteger (
					reloadFrequencyDeviation.getMillis () * 2));

	}

	private
	boolean checkPassword (
			@NonNull UserRec user,
			@NonNull String password) {

		// bypass for dev auto-login

		if (

			stringEqualSafe (
				password,
				"**********")

			&& wbsConfig.testUsers ().contains (
				joinWithFullStop (
					user.getSlice ().getCode (),
					user.getUsername ()))

		) {

			return true;

		}

		// always fail if user has no password

		if (user.getPassword () == null) {

			return false;

		}

		// fail if password hash doesn't match

		if (
			stringNotEqualSafe (
				user.getPassword (),
				hashSha1Base64 (
					password))
		) {

			return false;

		}

		// all correct

		return true;

	}

	// constants

	public final static
	String sessionIdCookieName =
		"wbs-session-id";

	public final static
	String userIdCookieName =
		"wbs-user-id";

	public final static
	Duration reloadFrequency =
		Duration.standardSeconds (
			5l);

	public final static
	Duration reloadFrequencyDeviation =
		Duration.standardSeconds (
			1l);

	public final static
	Duration logoffTime =
		Duration.standardMinutes (
			5l);

}
