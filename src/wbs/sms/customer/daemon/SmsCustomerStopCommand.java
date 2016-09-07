package wbs.sms.customer.daemon;

import static wbs.framework.utils.etc.Misc.isNotNull;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.google.common.base.Optional;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import static wbs.framework.utils.etc.OptionalUtils.optionalOrNull;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.service.model.ServiceRec;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.customer.logic.SmsCustomerLogic;
import wbs.sms.customer.model.SmsCustomerManagerRec;
import wbs.sms.customer.model.SmsCustomerObjectHelper;
import wbs.sms.customer.model.SmsCustomerRec;
import wbs.sms.customer.model.SmsCustomerSessionRec;
import wbs.sms.customer.model.SmsCustomerTemplateObjectHelper;
import wbs.sms.customer.model.SmsCustomerTemplateRec;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.inbox.daemon.CommandHandler;
import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.message.inbox.model.InboxAttemptRec;
import wbs.sms.message.inbox.model.InboxRec;
import wbs.sms.message.outbox.logic.MessageSender;
import wbs.sms.number.list.logic.NumberListLogic;

@Accessors (fluent = true)
@PrototypeComponent ("smsCustomerStopCommand")
public
class SmsCustomerStopCommand
	implements CommandHandler {

	// dependencies

	@Inject
	CommandObjectHelper commandHelper;

	@Inject
	Database database;

	@Inject
	SmsInboxLogic smsInboxLogic;

	@Inject
	MessageObjectHelper messageHelper;

	@Inject
	NumberListLogic numberListLogic;

	@Inject
	ObjectManager objectManager;

	@Inject
	ServiceObjectHelper serviceHelper;

	@Inject
	SmsCustomerLogic smsCustomerLogic;

	@Inject
	SmsCustomerObjectHelper smsCustomerHelper;

	@Inject
	SmsCustomerTemplateObjectHelper smsCustomerTemplateHelper;

	// prototype dependencies

	@Inject
	Provider<MessageSender> messageSenderProvider;

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
			"sms_customer_manager.stop",
		};

	}

	// implementation

	@Override
	public
	InboxAttemptRec handle () {

		Transaction transaction =
			database.currentTransaction ();

		MessageRec inboundMessage =
			inbox.getMessage ();

		SmsCustomerManagerRec customerManager =
			(SmsCustomerManagerRec)
			objectManager.getParent (
				command);

		SmsCustomerRec customer =
			smsCustomerHelper.findOrCreate (
				customerManager,
				inboundMessage.getNumber ());

		ServiceRec stopService =
			serviceHelper.findByCodeRequired (
				customerManager,
				"stop");

		// send stop message

		SmsCustomerTemplateRec stopTemplate =
			smsCustomerTemplateHelper.findByCodeRequired (
				customerManager,
				"stop");

		MessageRec outboundMessage;

		if (stopTemplate == null) {

			outboundMessage = null;

		} else {

			outboundMessage =
				messageSenderProvider.get ()

				.threadId (
					inboundMessage.getThreadId ())

				.number (
					customer.getNumber ())

				.messageText (
					stopTemplate.getText ())

				.numFrom (
					stopTemplate.getNumber ())

				.routerResolve (
					stopTemplate.getRouter ())

				.service (
					stopService)

				.affiliate (
					optionalOrNull (
						smsCustomerLogic.customerAffiliate (
							customer)))

				.send ();

		}

		// update session

		SmsCustomerSessionRec activeSession =
			customer.getActiveSession ();

		if (activeSession != null) {

			activeSession

				.setEndTime (
					transaction.now ())

				.setStopMessage (
					outboundMessage);

		}

		customer

			.setLastActionTime (
				transaction.now ())

			.setActiveSession (
				null);

		// add to number list

		if (
			isNotNull (
				customerManager.getStopNumberList ())
		) {

			numberListLogic.addDueToMessage (
				customerManager.getStopNumberList (),
				inboundMessage.getNumber (),
				inboundMessage,
				stopService);

		}

		// process message

		return smsInboxLogic.inboxProcessed (
			inbox,
			Optional.of (
				stopService),
			smsCustomerLogic.customerAffiliate (
				customer),
			command);

	}

}
