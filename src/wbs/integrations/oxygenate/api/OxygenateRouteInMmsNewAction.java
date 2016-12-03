package wbs.integrations.oxygenate.api;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.collection.IterableUtils.iterableFilter;
import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.etc.BinaryUtils.bytesFromBase64;
import static wbs.utils.etc.Misc.shouldNeverHappen;
import static wbs.utils.etc.Misc.stringTrim;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringIsEmpty;
import static wbs.utils.string.StringUtils.stringIsNotEmpty;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;
import static wbs.utils.string.StringUtils.stringNotInSafe;
import static wbs.utils.string.StringUtils.stringSplitSlash;
import static wbs.utils.string.StringUtils.utf8ToString;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.inject.Provider;

import lombok.Cleanup;
import lombok.NonNull;

import wbs.api.mvc.ApiLoggingAction;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.data.tools.DataFromXml;
import wbs.framework.data.tools.DataFromXmlBuilder;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.LoggedErrorsException;
import wbs.framework.logging.TaskLogger;

import wbs.integrations.oxygenate.model.OxygenateInboundLogObjectHelper;
import wbs.integrations.oxygenate.model.OxygenateInboundLogType;
import wbs.integrations.oxygenate.model.OxygenateRouteInObjectHelper;
import wbs.integrations.oxygenate.model.OxygenateRouteInRec;

import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaRec;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;
import wbs.platform.text.web.TextResponder;

import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.number.core.model.NumberObjectHelper;
import wbs.sms.number.core.model.NumberRec;
import wbs.sms.route.core.model.RouteObjectHelper;
import wbs.sms.route.core.model.RouteRec;

import wbs.utils.string.FormatWriter;

import wbs.web.exceptions.HttpUnprocessableEntityException;
import wbs.web.responder.Responder;

@PrototypeComponent ("oxygenateRouteInMmsNewAction")
public
class OxygenateRouteInMmsNewAction
	extends ApiLoggingAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaLogic mediaLogic;

	@SingletonDependency
	OxygenateInboundLogObjectHelper oxygenateInboundLogHelper;

	@SingletonDependency
	OxygenateRouteInObjectHelper oxygenateRouteInHelper;

	@SingletonDependency
	SmsInboxLogic smsInboxLogic;

	@SingletonDependency
	NumberObjectHelper smsNumberHelper;

	@SingletonDependency
	RouteObjectHelper smsRouteHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	// prototype dependencies

	@PrototypeDependency
	Provider <TextResponder> textResponderProvider;

	// state

	OxygenateRouteInMmsNewRequest request;

	Boolean success = false;

	// abstract implementation

	@Override
	protected
	void processRequest (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull FormatWriter debugWriter) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"processRequest");

		// read request

		byte[] requestBytes =
			requestContext.requestBodyRaw ();

		String requestString =
			utf8ToString (
				requestBytes);

		debugWriter.writeLineFormat (
			"===== REQUEST BODY =====");

		debugWriter.writeNewline ();

		debugWriter.writeString (
			stringTrim (
				requestString));

		debugWriter.writeNewline ();
		debugWriter.writeNewline ();

		// decode request

		try {

			request =
				genericCastUnchecked (
					requestFromXml.readInputStream (
						taskLogger,
						new ByteArrayInputStream (
							requestBytes),
						"oxygen8-route-in-mms-new.xml"));

		} catch (LoggedErrorsException loggedErrorsException) {

			throw new HttpUnprocessableEntityException (
				"Unable to interpret MMS request",
				emptyList ());

		}

		// simple verification

		if (
			stringNotEqualSafe (
				request.type (),
				"MMS")
		) {

			taskLogger.errorFormat (
				"Invalid value for type attribute: %s",
				request.type ());

		}

		for (
			OxygenateRouteInMmsNewRequest.Attachment attachment
				: request.attachments ()
		) {

			if (
				stringNotInSafe (
					attachment.encoding (),
					"base64",
					"text")
			) {

				taskLogger.errorFormat (
					"Invalid value for 'Encoding' attribute: %s",
					attachment.encoding ());

			}

		}

		// check for errors

		taskLogger.makeException (
			() -> new HttpUnprocessableEntityException (
				stringFormat (
					"Unable to process request due to %s errors",
					integerToDecimalString (
						taskLogger.errorCount ())),
				emptyList ()));

	}

	@Override
	protected
	void updateDatabase (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"updateDatabase");

		// begin transaction

		try (

			Transaction transaction =
				database.beginReadWrite (
					stringFormat (
						"%s.%s ()",
						getClass ().getSimpleName (),
						"updateDatabase"),
					this);

		) {

			OxygenateRouteInRec oxygenateRouteIn =
				oxygenateRouteInHelper.findRequired (
					parseIntegerRequired (
						requestContext.requestStringRequired (
							"smsRouteId")));

			RouteRec smsRoute =
				oxygenateRouteIn.getRoute ();

			List <MediaRec> medias =
				iterableMapToList (
					attachment ->
						processAttachment (
							taskLogger,
							attachment),
					request.attachments ());

			// combine text parts

			StringBuilder stringBuilder =
				new StringBuilder ();

			if (
				stringIsNotEmpty (
					request.subject ())
			) {

				stringBuilder.append (
					stringTrim (
						request.subject ()));

				stringBuilder.append (
					":\n");

			}

			for (
				MediaRec textMedia
					: iterableFilter (
						mediaLogic::isText,
						medias)
			) {

				String mediaString =
					utf8ToString (
						textMedia.getContent ().getData ());

				if (
					stringIsEmpty (
						mediaString)
				) {
					continue;
				}

				stringBuilder.append (
					stringTrim (
						mediaString));

				stringBuilder.append (
					"\n");

			}

			TextRec messageBodyText =
				textHelper.findOrCreate (
					stringBuilder.toString ());

			// lookup number, discarding extra info

			NumberRec number =
				smsNumberHelper.findOrCreate (
					listFirstElementRequired (
						stringSplitSlash (
							request.source ())));

			// insert message

			smsInboxLogic.inboxInsert (
				optionalOf (
					request.oxygenateReference ()),
				messageBodyText,
				number,
				request.destination (),
				smsRoute,
				optionalAbsent (),
				optionalAbsent (),
				medias,
				optionalAbsent (),
				optionalOf (
					request.subject ()));

			transaction.commit ();

		}

		success = true;

	}

	@Override
	protected
	Responder createResponse (
			@NonNull TaskLogger taskLogger,
			@NonNull FormatWriter debugWriter) {

		debugWriter.writeLineFormat (
			"===== RESPONSE =====");

		debugWriter.writeNewline ();

		debugWriter.writeLineFormat (
			"SUCCESS");

		debugWriter.writeNewline ();

		return textResponderProvider.get ()

			.text (
				"SUCCESS\n");

	}

	@Override
	protected
	void storeLog (
			@NonNull TaskLogger taskLogger,
			@NonNull String debugLog) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ClockworkSmsRouteInAction.storeLog ()",
				this);

		oxygenateInboundLogHelper.insert (
			oxygenateInboundLogHelper.createInstance ()

			.setRoute (
				smsRouteHelper.findRequired (
					Long.parseLong (
						requestContext.requestStringRequired (
							"smsRouteId"))))

			.setType (
				OxygenateInboundLogType.mmsMessage)

			.setTimestamp (
				transaction.now ())

			.setDetails (
				debugLog)

			.setSuccess (
				success)

		);

		transaction.commit ();

	}

	// private implementation

	private
	MediaRec processAttachment (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull OxygenateRouteInMmsNewRequest.Attachment attachment) {

		if (
			stringEqualSafe (
				attachment.encoding (),
				"text")
		) {

			return mediaLogic.createTextMedia (
				attachment.content (),
				attachment.contentType (),
				attachment.fileName ());

		} else if (
			stringEqualSafe (
				attachment.encoding (),
				"base64")
		) {

			byte[] attachmentContent =
				bytesFromBase64 (
					attachment.content ());

			return mediaLogic.createMediaRequired (
				attachmentContent,
				attachment.contentType (),
				attachment.fileName (),
				optionalOf (
					"utf8"));

		} else {

			throw shouldNeverHappen ();

		}

	}

	// misc

	private final static
	DataFromXml requestFromXml =
		new DataFromXmlBuilder ()

		.registerBuilderClasses (
			OxygenateRouteInMmsNewRequest.class,
			OxygenateRouteInMmsNewRequest.Attachment.class)

		.build ();

}