package wbs.clients.apn.chat.user.core.daemon;

import static wbs.framework.utils.etc.Misc.isNull;
import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.framework.utils.etc.TimeUtils.earlierThan;

import java.util.List;

import com.google.common.base.Optional;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import org.joda.time.Duration;

import wbs.clients.apn.chat.bill.model.ChatUserCreditMode;
import wbs.clients.apn.chat.contact.logic.ChatMessageLogic;
import wbs.clients.apn.chat.contact.model.ChatMonitorInboxRec;
import wbs.clients.apn.chat.contact.model.ChatUserInitiationLogObjectHelper;
import wbs.clients.apn.chat.contact.model.ChatUserInitiationReason;
import wbs.clients.apn.chat.core.logic.ChatMiscLogic;
import wbs.clients.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.object.ObjectManager;
import wbs.platform.daemon.SleepingDaemonService;

@Log4j
@SingletonComponent ("chatUserQuietDaemon")
public
class ChatUserQuietDaemon
	extends SleepingDaemonService {

	// singleton dependencies

	@SingletonDependency
	ChatMiscLogic chatLogic;

	@SingletonDependency
	ChatMessageLogic chatMessageLogic;

	@SingletonDependency
	ChatUserInitiationLogObjectHelper chatUserInitiationLogHelper;

	@SingletonDependency
	ChatUserObjectHelper chatUserHelper;

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	ObjectManager objectManager;

	// details

	@Override
	protected
	String getThreadName () {
		return "ChatUserQuiet";
	}

	@Override
	protected
	Duration getSleepDuration () {

		return Duration.standardSeconds (
			30);

	}

	@Override
	protected
	String generalErrorSource () {
		return "chat user quiet daemon";
	}

	@Override
	protected
	String generalErrorSummary () {
		return "error checking for quiet chat users to send a message to";
	}

	@Override
	protected
	void runOnce () {

		log.debug ("Looking for quiet users");

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				"ChatUserQuietDaemon.runOnce ()",
				this);

		// get a list of users who are past their outbound timestamp

		List<ChatUserRec> chatUsers =
			chatUserHelper.findWantingQuietOutbound (
				transaction.now ());

		transaction.close ();

		// then do each one

		for (
			ChatUserRec chatUser
				: chatUsers
		) {

			try {

				doUser (
					chatUser.getId ());

			} catch (Exception exception) {

				exceptionLogger.logThrowable (
					"daemon",
					"Chat daemon",
					exception,
					Optional.absent (),
					GenericExceptionResolution.tryAgainLater);

			}

		}

	}

	private
	void doUser (
			@NonNull Long chatUserId) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ChatUserQuietDaemon.doUser (chatUserId)",
				this);

		// find the user

		ChatUserRec user =
			chatUserHelper.findRequired (
				chatUserId);

		String userPath =
			objectManager.objectPath (
				user);

		// check and clear the outbound message flag

		if (

			isNull (
				user.getNextQuietOutbound ())

			|| earlierThan (
				transaction.now (),
				user.getNextQuietOutbound ())

		) {
			return;
		}

		user

			.setNextQuietOutbound (
				null);

		// check if they have been barred

		if (user.getBarred ()) {

			log.info (
				stringFormat (
					"Skipping quiet alarm for %s: barred",
					userPath));

			transaction.commit ();

			return;
		}

		if (user.getCreditMode () == ChatUserCreditMode.barred) {

			log.info (
				stringFormat (
					"Skipping quiet alarm for %s: barred",
					userPath));

			transaction.commit ();

			return;

		}

		// check if they are a "good" user

		if (user.getCreditSuccess () < 300) {

			log.info (
				stringFormat (
					"Skipping quiet alarm for %s: low credit success",
					userPath));

			transaction.commit ();

			return;
		}

		// find a monitor

		ChatUserRec monitor =
			chatLogic.getOnlineMonitorForOutbound (
				user);

		if (monitor == null) {

			log.info (
				stringFormat (
					"Skipping quiet alarm for %s: no available monitor",
					userPath));

			transaction.commit ();

			return;
		}

		String monitorPath =
			objectManager.objectPath (
				monitor);

		// create or update the inbox

		ChatMonitorInboxRec chatMonitorInbox =
			chatMessageLogic.findOrCreateChatMonitorInbox (
				monitor,
				user,
				true);

		chatMonitorInbox

			.setOutbound (
				true);

		// create a log

		chatUserInitiationLogHelper.insert (
			chatUserInitiationLogHelper.createInstance ()

			.setChatUser (
				user)

			.setMonitorChatUser (
				monitor)

			.setReason (
				ChatUserInitiationReason.quietUser)

			.setTimestamp (
				transaction.now ())

		);

		// and return

		log.info (
			stringFormat (
				"Setting quiet alarm for %s with %s",
				userPath,
				monitorPath));

		transaction.commit ();

	}

}
