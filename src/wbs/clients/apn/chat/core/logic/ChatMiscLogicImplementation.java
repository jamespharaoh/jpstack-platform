package wbs.clients.apn.chat.core.logic;

import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.TimeUtils.earlierThan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import wbs.clients.apn.chat.bill.logic.ChatCreditLogic;
import wbs.clients.apn.chat.contact.logic.ChatSendLogic;
import wbs.clients.apn.chat.contact.logic.ChatSendLogic.TemplateMissing;
import wbs.clients.apn.chat.contact.model.ChatContactRec;
import wbs.clients.apn.chat.contact.model.ChatMessageMethod;
import wbs.clients.apn.chat.core.model.ChatRec;
import wbs.clients.apn.chat.date.logic.ChatDateLogic;
import wbs.clients.apn.chat.help.logic.ChatHelpLogic;
import wbs.clients.apn.chat.help.logic.ChatHelpTemplateLogic;
import wbs.clients.apn.chat.user.core.logic.ChatUserLogic;
import wbs.clients.apn.chat.user.core.logic.ChatUserLogic.UserDistance;
import wbs.clients.apn.chat.user.core.model.ChatUserDateMode;
import wbs.clients.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.clients.apn.chat.user.core.model.ChatUserSessionObjectHelper;
import wbs.clients.apn.chat.user.core.model.ChatUserType;
import wbs.clients.apn.chat.user.core.model.Gender;
import wbs.clients.apn.chat.user.core.model.Orient;
import wbs.clients.apn.chat.user.info.model.ChatUserInfoStatus;
import wbs.clients.apn.chat.user.info.model.ChatUserNameObjectHelper;
import wbs.clients.apn.chat.user.info.model.ChatUserNameRec;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import wbs.framework.utils.RandomLogic;
import wbs.framework.utils.TimeFormatter;
import wbs.platform.event.logic.EventLogic;
import wbs.platform.exception.logic.ExceptionLogLogic;
import wbs.platform.media.logic.MediaLogic;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.locator.logic.LocatorManager;
import wbs.sms.locator.model.LocatorObjectHelper;
import wbs.sms.locator.model.LocatorRec;
import wbs.sms.locator.model.LongLat;
import wbs.sms.magicnumber.logic.MagicNumberLogic;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.outbox.logic.MessageSender;

@Log4j
@SingletonComponent ("chatMiscLogic")
public
class ChatMiscLogicImplementation
	implements ChatMiscLogic {

	// dependencies

	@Inject
	ChatCreditLogic chatCreditLogic;

	@Inject
	ChatDateLogic chatDateLogic;

	@Inject
	ChatHelpLogic chatHelpLogic;

	@Inject
	ChatNumberReportLogic chatNumberReportLogic;

	@Inject
	ChatSendLogic chatSendLogic;

	@Inject
	ChatHelpTemplateLogic chatTemplateLogic;

	@Inject
	ChatUserObjectHelper chatUserHelper;

	@Inject
	ChatUserNameObjectHelper chatUserNameHelper;

	@Inject
	ChatUserLogic chatUserLogic;

	@Inject
	ChatUserSessionObjectHelper chatUserSessionHelper;

	@Inject
	CommandObjectHelper commandHelper;

	@Inject
	Database database;

	@Inject
	EventLogic eventLogic;

	@Inject
	ExceptionLogLogic exceptionLogic;

	@Inject
	LocatorObjectHelper locatorHelper;

	@Inject
	LocatorManager locatorManager;

	@Inject
	MagicNumberLogic magicNumberLogic;

	@Inject
	MediaLogic mediaUtils;

	@Inject
	ObjectManager objectManager;

	@Inject
	QueueLogic queueLogic;

	@Inject
	RandomLogic randomLogic;

	@Inject
	ServiceObjectHelper serviceHelper;

	@Inject
	TimeFormatter timeFormatter;

	// prototype dependencies

	@Inject
	Provider<MessageSender> messageSender;

	// implementation

	@Override
	public
	List<ChatUserRec> getOnlineMonitorsForOutbound (
			ChatUserRec thisUser) {

		ChatRec chat =
			thisUser.getChat ();

		Collection<ChatUserRec> onlineUsers =
			chatUserHelper.findOnline (
				chat);

		List<ChatUserRec> ret =
			new ArrayList<ChatUserRec> ();

		for (ChatUserRec chatUser : onlineUsers) {

			// ignore non-monitors
			if (chatUser.getType () != ChatUserType.monitor)
				continue;

			// ignore blocked users
			if (thisUser.getBlocked ().containsKey (chatUser.getId ()))
				continue;

			// if we aren't suitable gender/orients for each other skip it

			if (! chatUserLogic.compatible (
					thisUser,
					chatUser))
				continue;

			// ignore users we have previously had a message from

			ChatContactRec chatContact =
				thisUser.getFromContacts ().get (
					chatUser.getId ());

			if (chatContact != null
					&& chatContact.getLastDeliveredMessageTime () != null)
				continue;

			// ignore users with no info or pic

			if (chatUser.getInfoText () == null)
				continue;

			if (chatUser.getMainChatUserImage () == null)
				continue;

			// ignore users according to monitor cap

			if (thisUser.getMonitorCap () != null
					&& chatUser.getType () == ChatUserType.monitor
					&& (chatUser.getCode ().charAt (2) - '0')
						< thisUser.getMonitorCap ())
				continue;

			ret.add (chatUser);

		}

		return ret;

	}

	/**
	 * Get the closest online monitor suitable to use for an outbound message.
	 *
	 * @see getOnlineMoniorsForOutbound for detailed criteria.
	 *
	 * @param thisUser
	 *            User message is to be sent to
	 * @return Monitor to send
	 */
	@Override
	public
	ChatUserRec getOnlineMonitorForOutbound (
			ChatUserRec thisUser) {

		List<ChatUserRec> monitors =
			getOnlineMonitorsForOutbound (
				thisUser);

		List<UserDistance> distances =
			chatUserLogic.getUserDistances (
				thisUser,
				monitors);

		Collections.sort (
			distances);

		return distances.size () > 0
			? distances.get (0).user ()
			: null;

	}

	@Override
	public
	void blockAll (
			ChatUserRec chatUser,
			MessageRec message) {

		// log them off

		chatUserLogic.logoff (
			chatUser,
			message == null);

		// block all allMessages and ads

		chatUser

			.setBlockAll (
				true)

			.setNextAd (
				null);

		// turn off dating

		chatDateLogic.userDateStuff (
			chatUser,
			null,
			message,
			null,
			false);

		// send message

		if (chatUser.getChatScheme () != null) {

			chatSendLogic.sendSystemRbFree (
				chatUser,
				Optional.<Long>absent (),
				"block_all_confirm",
				TemplateMissing.error,
				Collections.<String,String>emptyMap ());

		}

	}

	@Override
	public
	void userAutoJoin (
			ChatUserRec chatUser,
			MessageRec message,
			boolean sendMessage) {

		ChatRec chat =
			chatUser.getChat ();

		// join chat

		if (
			chat.getAutoJoinChat ()
			&& ! chatUser.getOnline ()
		) {

			userJoin (
				chatUser,
				sendMessage,
				message.getThreadId (),
				ChatMessageMethod.sms);

			sendMessage = false;

		}

		// join date

		if (
			chat.getAutoJoinDate ()
			&& chatUser.getDateMode () == ChatUserDateMode.none
		) {

			chatDateLogic.userDateStuff (
				chatUser,
				null,
				message,
				chatUser.getMainChatUserImage () != null
					? ChatUserDateMode.photo
					: ChatUserDateMode.text,
				sendMessage);

		}

	}

	@Override
	public
	void userJoin (
			ChatUserRec chatUser,
			boolean sendMessage,
			Long threadId,
			ChatMessageMethod deliveryMethod) {

		Transaction transaction =
			database.currentTransaction ();

		ChatRec chat =
			chatUser.getChat ();

		// if they're already online do nothing

		if (
			chatUser.getOnline ()
			&& chatUser.getDeliveryMethod () == deliveryMethod
		) {
			return;
		}

		boolean wasOnline =
			chatUser.getOnline ();

		// log the user on

		chatUser

			.setOnline (
				true)

			.setBlockAll (
				false)

			.setLastJoin (
				transaction.now ())

			.setLastAction (
				transaction.now ())

			.setFirstJoin (
				ifNull (
					chatUser.getFirstJoin (),
					transaction.now ()))

			.setNextRegisterHelp (
				null)

			.setDeliveryMethod (
				deliveryMethod);

		// schedule an ad

		chatUserLogic.scheduleAd (
			chatUser);

		// create session

		if (! wasOnline) {

			chatUserSessionHelper.insert (
				chatUserSessionHelper.createInstance ()

				.setChatUser (
					chatUser)

				.setStartTime (
					transaction.now ())

			);

		}

		// send message

		if (sendMessage) {

			chatSendLogic.sendSystemMagic (
				chatUser,
				Optional.fromNullable (
					threadId),
				"logon",
				commandHelper.findByCodeRequired (
					chat,
					"magic"),
				(long) commandHelper.findByCodeRequired (
					chat,
					"help").getId (),
				TemplateMissing.error,
				Collections.<String,String>emptyMap ());

		}

		// lookup location for web users

		if (

			deliveryMethod == ChatMessageMethod.web

			&& (

				chatUser.getLocationTime () == null

				|| earlierThan (
					chatUser.getLocationTime (),
					transaction.now ().minus (
						Duration.standardHours (1)))

			)

		) {

			final int chatUserId =
				chatUser.getId ();

			final int locatorId =
				chat.getLocator ().getId ();

			locatorManager.locate (
				chat.getLocator ().getId (),
				chatUser.getNumber ().getId (),
				serviceHelper.findByCodeRequired (
					chat,
					"default").getId (),
				chatUserLogic.getAffiliateId (chatUser),
				new LocatorManager.AbstractCallback () {

				@Override
				public
				void success (
						LongLat longLat) {

					@Cleanup
					Transaction transaction =
						database.beginReadWrite (
							"ChatMiscLogicImplementation.userJoin.locatorCallback.success (..)",
							this);

					ChatUserRec chatUser =
						chatUserHelper.findRequired (
							chatUserId);

					if (longLat == null) {
						throw new NullPointerException ();
					}

					{

						if (
							! transaction.contains (
								chatUser)
						) {

							throw new IllegalStateException (
								stringFormat (
									"Chat user %s not in transaction",
									chatUser.getId ()));

						}

					}

					chatUser

						.setLocationLongLat (
							longLat)

						.setLocationBackupLongLat (
							longLat)

						.setLocationTime (
							transaction.now ());

					LocatorRec locator =
						locatorHelper.findRequired (
							locatorId);

					eventLogic.createEvent (
						"chat_user_location_locator",
						chatUser,
						longLat.longitude (),
						longLat.latitude (),
						locator);

					transaction.commit ();

					log.info (
						stringFormat (
							"Got location for %s: %s",
							chatUser.getCode (),
							longLat));

				}

			});

		}

	}

	@Override
	public
	void userLogoffWithMessage (
			ChatUserRec chatUser,
			Long threadId,
			boolean automatic) {

		ChatRec chat = chatUser.getChat ();

		// if they aren't online possibly send them a stop dating hint

		if (! chatUser.getOnline ()) {

			if (chatUser.getDateMode () != ChatUserDateMode.none) {

				chatSendLogic.sendSystemMagic (
					chatUser,
					Optional.fromNullable (
						threadId),
					"date_stop_hint",
					commandHelper.findByCodeRequired (
						chat,
						"help"),
					0l,
					TemplateMissing.error,
					Collections.<String,String>emptyMap ());

			}

			return;

		}

		// log the user off

		chatUserLogic.logoff (
				chatUser,
				automatic);

		// send a message

		if (chatUser.getNumber () != null) {

			chatSendLogic.sendSystemRbFree (
				chatUser,
				Optional.fromNullable (threadId),
				"logoff_confirm",
				TemplateMissing.error,
				Collections.<String,String>emptyMap ());

		}

	}

	@Override
	public
	void monitorsToTarget (
			ChatRec chat,
			Gender gender,
			Orient orient,
			long target) {

		// fetch all appropriate monitors

		List<ChatUserRec> allMonitors =
			chatUserHelper.find (
				chat,
				ChatUserType.monitor,
				orient,
				gender);

		// now sort into online and offline ones

		List<ChatUserRec> onlineMonitors =
			new ArrayList<ChatUserRec> ();

		List<ChatUserRec> offlineMonitors =
			new ArrayList<ChatUserRec> ();

		for (ChatUserRec monitor : allMonitors) {

			if (monitor.getOnline ()) {
				onlineMonitors.add (monitor);
			} else {
				offlineMonitors.add (monitor);
			}
		}

		// put monitors online
		while (onlineMonitors.size () < target
				&& offlineMonitors.size () > 0) {

			ChatUserRec monitor =
				randomLogic.sample (
					offlineMonitors);

			monitor.setOnline (true);

			onlineMonitors.add (monitor);

		}

		// take monitors offline

		while (onlineMonitors.size () > target) {

			ChatUserRec monitor =
				randomLogic.sample (
					onlineMonitors);

			monitor.setOnline (false);
			offlineMonitors.add (monitor);

		}

	}

	@Override
	public
	void chatUserSetName (
			ChatUserRec chatUser,
			String name,
			Long threadId) {

		Transaction transaction =
			database.currentTransaction ();

		ChatRec chat =
			chatUser.getChat ();

		// create the chat user name


		ChatUserNameRec chatUserName =
			chatUserNameHelper.insert (
				chatUserNameHelper.createInstance ()

			.setChatUser (
				chatUser)

			.setCreationTime (
				transaction.now ())

			.setOriginalName (
				name)

			.setEditedName (
				name)

			.setStatus (
				ChatUserInfoStatus.moderatorPending)

			.setThreadId (
				threadId)

		);

		chatUser.getChatUserNames ().add (
			chatUserName);

		chatUser

			.setNewChatUserName (
				chatUserName);

		// create the queue item

		if (chatUser.getQueueItem () == null) {

			QueueItemRec qi =
				queueLogic.createQueueItem (
					queueLogic.findQueue (chat, "user"),
					chatUser,
					chatUser,
					chatUserLogic.getPrettyName (
						chatUser),
					"Name to approve");

			chatUser.setQueueItem (qi);

		}

		// send reply

		if (threadId != null)

			chatSendLogic.sendSystemMagic (
				chatUser,
				Optional.of (
					threadId),
				"name_confirm",
				commandHelper.findByCodeRequired (
					chat,
					"magic"),
				(long) commandHelper.findByCodeRequired (
					chat,
					"name").getId (),
				TemplateMissing.error,
				ImmutableMap.<String,String>builder ()
					.put ("newName", name)
					.build ());

	}

	@Override
	public
	DateTimeZone timezone (
			@NonNull ChatRec chat) {

		return timeFormatter.timezone (
			chat.getTimezone ());

	}

}