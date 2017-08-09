package wbs.apn.chat.contact.logic;

import java.util.List;

import com.google.common.base.Optional;

import wbs.framework.database.Transaction;

import wbs.platform.media.model.MediaRec;
import wbs.platform.text.model.TextRec;

import wbs.sms.message.core.model.MessageRec;

import wbs.apn.chat.contact.model.ChatMessageMethod;
import wbs.apn.chat.contact.model.ChatMessageRec;
import wbs.apn.chat.contact.model.ChatMonitorInboxRec;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.user.core.model.ChatUserRec;

public
interface ChatMessageLogic {

	boolean chatMessageDeliverViaSms (
			Transaction parentTransaction,
			ChatMessageRec chatMessage,
			String text);

	boolean chatMessageDeliverToUser (
			Transaction parentTransaction,
			ChatMessageRec chatMessage);

	boolean chatMessageDeliverViaJigsaw (
			Transaction parentTransaction,
			ChatMessageRec chatMessage,
			String text);

	void chatMessageDeliver (
			Transaction parentTransaction,
			ChatMessageRec chatMessage);

	boolean chatMessageIsRecentDupe (
			Transaction parentTransaction,
			ChatUserRec fromUser,
			ChatUserRec toUser,
			TextRec originalText);

	String chatMessagePrependWarning (
			Transaction parentTransaction,
			ChatMessageRec chatMessage);

	String chatMessageSendFromUser (
			Transaction parentTransaction,
			ChatUserRec fromUser,
			ChatUserRec toUser,
			String text,
			Optional <Long> threadId,
			ChatMessageMethod source,
			List<MediaRec> medias);

	void chatMessageSendFromUserPartTwo (
			Transaction parentTransaction,
			ChatMessageRec chatMessage);

	ApprovalResult checkForApproval (
			Transaction parentTransaction,
			ChatRec chat,
			String message);

	ChatMonitorInboxRec findOrCreateChatMonitorInbox (
			Transaction parentTransaction,
			ChatUserRec monitor,
			ChatUserRec user,
			boolean alarm);

	void chatUserRejectionCountInc (
			Transaction parentTransaction,
			ChatUserRec chatUser,
			MessageRec message);

	static
	class ApprovalResult {

		public static
		enum Status {
			clean,
			auto,
			manual
		}

		public
		Status status;

		public
		String message;

	}

}