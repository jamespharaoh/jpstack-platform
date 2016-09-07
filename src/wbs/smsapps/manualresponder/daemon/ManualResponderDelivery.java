package wbs.smsapps.manualresponder.daemon;

import static wbs.framework.utils.etc.CollectionUtils.listIndexOfRequired;
import static wbs.framework.utils.etc.CollectionUtils.listItemAtIndexRequired;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.NonNull;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.sms.message.core.logic.SmsMessageLogic;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.core.model.MessageStatus;
import wbs.sms.message.delivery.daemon.DeliveryHandler;
import wbs.sms.message.delivery.model.DeliveryObjectHelper;
import wbs.sms.message.delivery.model.DeliveryRec;
import wbs.sms.message.outbox.logic.SmsOutboxLogic;
import wbs.smsapps.manualresponder.model.ManualResponderReplyObjectHelper;
import wbs.smsapps.manualresponder.model.ManualResponderReplyRec;

@PrototypeComponent ("manualResponderDelivery")
public
class ManualResponderDelivery
	implements DeliveryHandler {

	// dependencies

	@Inject
	Database database;

	@Inject
	DeliveryObjectHelper deliveryHelper;

	@Inject
	ManualResponderReplyObjectHelper manualResponderReplyHelper;

	@Inject
	SmsMessageLogic messageLogic;

	@Inject
	SmsOutboxLogic outboxLogic;

	@Inject
	ServiceObjectHelper serviceHelper;

	// details

	@Override
	public
	Collection<String> getDeliveryTypeCodes () {

		return Arrays.<String>asList (
			"manual_responder");

	}

	// implementation

	@Override
	public
	void handle (
			@NonNull Long deliveryId,
			@NonNull Long ref) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ManualResopnderDelivery.handle (deliveryId, ref)",
				this);

		DeliveryRec delivery =
			deliveryHelper.findRequired (
				deliveryId);

		if (delivery.getNewMessageStatus ().isGoodType ()) {

			MessageRec deliveryMessage =
				delivery.getMessage ();

			ManualResponderReplyRec reply =
				manualResponderReplyHelper.findRequired (
					deliveryMessage.getRef ());

			Long deliveryMessageIndex =
				listIndexOfRequired (
					reply.getMessages (),
					deliveryMessage);

			if (
				reply.getMessages ().size ()
					> deliveryMessageIndex + 1
			) {

				MessageRec nextMessage =
					listItemAtIndexRequired (
						reply.getMessages (),
						deliveryMessageIndex + 1);

				if (nextMessage.getStatus () == MessageStatus.held) {

					outboxLogic.unholdMessage (
						nextMessage);

				}

			}

		}

		if (delivery.getNewMessageStatus ().isBadType ()) {

			MessageRec deliveryMessage =
				delivery.getMessage ();

			ManualResponderReplyRec reply =
				manualResponderReplyHelper.findRequired (
					deliveryMessage.getRef ());

			Long deliveryMessageIndex =
				listIndexOfRequired (
					reply.getMessages (),
					deliveryMessage);

			for (
				long messageIndex = deliveryMessageIndex + 1;
				messageIndex < reply.getMessages ().size ();
				messageIndex ++
			) {

				MessageRec heldMessage =
					listItemAtIndexRequired (
						reply.getMessages (),
						messageIndex);

				if (heldMessage.getStatus () == MessageStatus.held) {

					messageLogic.messageStatus (
						heldMessage,
						MessageStatus.cancelled);

				}

			}

		}

		deliveryHelper.remove (
			delivery);

		transaction.commit ();

	}

}
