package wbs.apn.chat.help.daemon;

import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.IdObject;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.service.model.ServiceRec;

import wbs.sms.command.logic.CommandLogic;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.inbox.daemon.CommandHandler;
import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.message.inbox.model.InboxAttemptRec;
import wbs.sms.message.inbox.model.InboxRec;

import wbs.apn.chat.bill.logic.ChatCreditCheckResult;
import wbs.apn.chat.bill.logic.ChatCreditLogic;
import wbs.apn.chat.contact.logic.ChatSendLogic;
import wbs.apn.chat.contact.logic.ChatSendLogic.TemplateMissing;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.help.logic.ChatHelpLogLogic;
import wbs.apn.chat.user.core.logic.ChatUserLogic;
import wbs.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;

@Accessors (fluent = true)
@PrototypeComponent ("chatHelpCommand")
public
class ChatHelpCommand
	implements CommandHandler {

	// singleton dependencies

	@SingletonDependency
	ChatCreditLogic chatCreditLogic;

	@SingletonDependency
	ChatHelpLogLogic chatHelpLogLogic;

	@SingletonDependency
	ChatSendLogic chatSendLogic;

	@SingletonDependency
	ChatUserObjectHelper chatUserHelper;

	@SingletonDependency
	ChatUserLogic chatUserLogic;

	@SingletonDependency
	CommandObjectHelper commandHelper;

	@SingletonDependency
	CommandLogic commandLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ServiceObjectHelper serviceHelper;

	@SingletonDependency
	SmsInboxLogic smsInboxLogic;

	@SingletonDependency
	MessageObjectHelper messageHelper;

	@SingletonDependency
	ObjectManager objectManager;

	// properties

	@Getter @Setter
	InboxRec inbox;

	@Getter @Setter
	CommandRec command;

	@Getter @Setter
	Optional<Long> commandRef;

	@Getter @Setter
	String rest;

	// details

	@Override
	public
	String[] getCommandTypes () {

		return new String [] {
			"chat.help"
		};

	}

	// implementation

	@Override
	public
	InboxAttemptRec handle (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"handle");

		) {

			ChatRec chat =
				genericCastUnchecked (
					objectManager.getParentRequired (
						transaction,
						command));

			ServiceRec defaultService =
				serviceHelper.findByCodeRequired (
					transaction,
					chat,
					"default");

			MessageRec message =
				inbox.getMessage ();

			ChatUserRec chatUser =
				chatUserHelper.findOrCreate (
					transaction,
					chat,
					message);

			AffiliateRec affiliate =
				chatUserLogic.getAffiliate (
					transaction,
					chatUser);

			// send barred users to help

			ChatCreditCheckResult creditCheckResult =
				chatCreditLogic.userSpendCreditCheck (
					transaction,
					chatUser,
					true,
					optionalOf (
						message.getThreadId ()));

			if (creditCheckResult.failed ()) {

				chatHelpLogLogic.createChatHelpLogIn (
					transaction,
					chatUser,
					message,
					rest,
					optionalAbsent (),
					true);

				return smsInboxLogic.inboxProcessed (
					transaction,
					inbox,
					optionalOf (
						defaultService),
					optionalOf (
						affiliate),
					command);

			}

			if (rest.length () == 0) {

				// send help error

				chatSendLogic.sendSystemMagic (
					transaction,
					chatUser,
					optionalOf (
						message.getThreadId ()),
					"help_error",
					commandHelper.findByCodeRequired (
						transaction,
						chat,
						"magic"),
					IdObject.objectId (
						commandHelper.findByCodeRequired (
							transaction,
							chat,
							"help")),
					TemplateMissing.error,
					emptyMap ());

			} else {

				// store message as help request

				chatHelpLogLogic.createChatHelpLogIn (
					transaction,
					chatUser,
					message,
					rest,
					optionalOf (
						commandHelper.findByCodeRequired (
							transaction,
							chat,
							"help")),
					true);

			}

			// process inbox

			return smsInboxLogic.inboxProcessed (
				transaction,
				inbox,
				optionalOf (
					defaultService),
				optionalOf (
					affiliate),
				command);

		}

	}

}
