package wbs.imchat.api;

import static wbs.utils.etc.LogicUtils.referenceNotEqualWithClass;
import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.string.StringUtils.joinWithCommaAndSpace;
import static wbs.utils.string.StringUtils.objectToString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.time.TimeUtils.millisToInstant;

import javax.inject.Provider;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.google.common.base.Optional;

import lombok.Cleanup;
import wbs.imchat.model.ImChatCustomerRec;
import wbs.imchat.model.ImChatEventObjectHelper;
import wbs.imchat.model.ImChatObjectHelper;
import wbs.imchat.model.ImChatRec;
import wbs.imchat.model.ImChatSessionObjectHelper;
import wbs.imchat.model.ImChatSessionRec;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.data.tools.DataFromJson;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.web.Action;
import wbs.framework.web.JsonResponder;
import wbs.framework.web.RequestContext;
import wbs.framework.web.Responder;

@PrototypeComponent ("imChatEventPostAction")
public
class ImChatEventPostAction
	implements Action {

	// dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	ImChatEventObjectHelper imChatEventHelper;

	@SingletonDependency
	ImChatObjectHelper imChatHelper;

	@SingletonDependency
	ImChatSessionObjectHelper imChatSessionHelper;

	@SingletonDependency
	RequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	Provider <JsonResponder> jsonResponderProvider;

	// implementation

	@Override
	public
	Responder handle () {

		DataFromJson dataFromJson =
			new DataFromJson ();

		// decode request

		JSONObject jsonValue =
			(JSONObject)
			JSONValue.parse (
				requestContext.reader ());

		ImChatEventPostRequest eventPostRequest =
			dataFromJson.fromJson (
				ImChatEventPostRequest.class,
				jsonValue);

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ImChatEventPostAction.handle ()",
				this);

		ImChatRec imChat =
			imChatHelper.findRequired (
				parseIntegerRequired (
					requestContext.requestStringRequired (
						"imChatId")));

		// write events

		long timestampAdjustment =
			+ transaction.now ().getMillis ()
			- eventPostRequest.timestamp ();

		for (
			ImChatEventItemRequest eventItemRequest
				: eventPostRequest.events ()
		) {

			// lookup session

			ImChatSessionRec session = null;
			ImChatCustomerRec customer = null;

			if (
				isNotNull (
					eventItemRequest.sessionSecret ())
			) {

				session =
					imChatSessionHelper.findBySecret (
						eventItemRequest.sessionSecret ());

				if (
					isNotNull (
						session)
				) {

					customer =
						session.getImChatCustomer ();

					if (

						! session.getActive ()

						|| referenceNotEqualWithClass (
							ImChatRec.class,
							session.getImChatCustomer ().getImChat (),
							imChat)

					) {
						session = null;
						customer = null;
					}

				}

			}

			imChatEventHelper.insert (
				imChatEventHelper.createInstance ()

				.setImChat (
					imChat)

				.setTimestamp (
					transaction.now ())

				.setIndex (
					eventItemRequest.index ())

				.setClientTimestamp (
					millisToInstant (
						eventItemRequest.timestamp ()))

				.setAdjustedTimestamp (
					millisToInstant (
						+ eventItemRequest.timestamp ()
						+ timestampAdjustment))

				.setInvocationToken (
					eventPostRequest.invocationToken ())

				.setImChatSession (
					session)

				.setImChatCustomer (
					customer)

				.setSource (
					requestContext.header ("x-forwarded-for") != null
						? joinWithCommaAndSpace (
							requestContext.header ("x-forwarded-for"),
							requestContext.request ().getRemoteHost ())
						: requestContext.request ().getRemoteHost ())

				.setType (
					eventItemRequest.type ())

				.setPayload (
					eventItemRequest.payload ().toJSONString ())

			);

			// write exceptions

			if (
				stringEqualSafe (
					eventItemRequest.type (),
					"unhandled-error")
			) {

				JSONObject payload =
					eventItemRequest.payload ();

				exceptionLogger.logSimple (
					"external",

					objectToString (
						ifNull (
							payload.get ("source"),
							"unknown")),

					objectToString (
						ifNull (
							payload.get ("message"),
							"unknown")),

					stringFormat (

						"URL: %s\n",
						objectToString (
							ifNull (
								payload.get ("url"),
								"unknown")),

						"Line: %s\n",
						objectToString (
							ifNull (
								payload.get ("line"),
								"unknown")),

						"Column: %s\n",
						objectToString (
							ifNull (
								payload.get ("column"),
								"unknown")),

						"Message: %s\n",
						objectToString (
							ifNull (
								payload.get ("message"),
								"unknown")),

						"User agent: %s\n",
						objectToString (
							ifNull (
								payload.get ("userAgent"),
								"unknown")),

						"\n",

						"Trace:\n",
						objectToString (
							ifNull (
								payload.get ("trace"),
								""))),

					Optional.absent (),

					GenericExceptionResolution.ignoreWithThirdPartyWarning);

			} else if (
				stringEqualSafe (
					eventItemRequest.type (),
					"api-error")
			) {

				JSONObject payload =
					eventItemRequest.payload ();

				exceptionLogger.logSimple (
					"external",

					objectToString (
						ifNull (
							payload.get ("source"),
							"unknown")),

					objectToString (
						ifNull (
							payload.get ("error"),
							"unknown")),

					stringFormat (

						"Error: %s\n",
						objectToString (
							ifNull (
								payload.get ("error"),
								"unknown")),

						"Path: %s\n",
						objectToString (
							ifNull (
								payload.get ("path"),
								"none")),

						"User agent: %s\n",
						objectToString (
							ifNull (
								payload.get ("userAgent"),
								"unknown")),

						"Request: %s\n",
						objectToString (
							ifNull (
								payload.get ("request"),
								"none")),

						"Response: %s\n",
						objectToString (
							ifNull (
								payload.get ("response"),
								"none"))),

					Optional.absent (),

					GenericExceptionResolution.ignoreWithThirdPartyWarning);

			}

		}

		// commit and return

		transaction.commit ();

		return jsonResponderProvider.get ()

			.value (
				"HELLO WORLD");

	}

}