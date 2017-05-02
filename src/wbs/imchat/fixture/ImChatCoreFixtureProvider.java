package wbs.imchat.fixture;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.io.FileUtils.fileReadBytes;
import static wbs.utils.string.StringUtils.joinWithSlash;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.fixtures.FixtureProvider;
import wbs.framework.logging.LogContext;

import wbs.integrations.paypal.model.PaypalAccountObjectHelper;

import wbs.platform.currency.model.CurrencyObjectHelper;
import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaRec;
import wbs.platform.menu.model.MenuGroupObjectHelper;
import wbs.platform.menu.model.MenuItemObjectHelper;
import wbs.platform.scaffold.model.SliceObjectHelper;
import wbs.platform.text.model.TextObjectHelper;

import wbs.utils.random.RandomLogic;

import wbs.imchat.model.ImChatConversationObjectHelper;
import wbs.imchat.model.ImChatConversationRec;
import wbs.imchat.model.ImChatCustomerDetailDataType;
import wbs.imchat.model.ImChatCustomerDetailTypeObjectHelper;
import wbs.imchat.model.ImChatCustomerObjectHelper;
import wbs.imchat.model.ImChatCustomerRec;
import wbs.imchat.model.ImChatMessageObjectHelper;
import wbs.imchat.model.ImChatObjectHelper;
import wbs.imchat.model.ImChatPricePointObjectHelper;
import wbs.imchat.model.ImChatPricePointRec;
import wbs.imchat.model.ImChatProfileObjectHelper;
import wbs.imchat.model.ImChatProfileRec;
import wbs.imchat.model.ImChatProfileState;
import wbs.imchat.model.ImChatPurchaseObjectHelper;
import wbs.imchat.model.ImChatPurchaseState;
import wbs.imchat.model.ImChatRec;
import wbs.imchat.model.ImChatSessionObjectHelper;
import wbs.imchat.model.ImChatSessionRec;
import wbs.imchat.model.ImChatTemplateObjectHelper;
import wbs.services.messagetemplate.logic.MessageTemplateLogic;
import wbs.services.messagetemplate.model.MessageTemplateDatabaseRec;
import wbs.services.messagetemplate.model.MessageTemplateSetObjectHelper;

@PrototypeComponent ("imChatCoreFixtureProvider")
public
class ImChatCoreFixtureProvider
	implements FixtureProvider {

	// dependencies

	@SingletonDependency
	CurrencyObjectHelper currencyHelper;

	@SingletonDependency
	Database database;

	@SingletonDependency
	ImChatCustomerDetailTypeObjectHelper imChatCustomerDetailTypeHelper;

	@SingletonDependency
	ImChatCustomerObjectHelper imChatCustomerHelper;

	@SingletonDependency
	ImChatConversationObjectHelper imChatConversationHelper;

	@SingletonDependency
	ImChatObjectHelper imChatHelper;

	@SingletonDependency
	ImChatMessageObjectHelper imChatMessageHelper;

	@SingletonDependency
	ImChatPricePointObjectHelper imChatPricePointHelper;

	@SingletonDependency
	ImChatProfileObjectHelper imChatProfileHelper;

	@SingletonDependency
	ImChatPurchaseObjectHelper imChatPurchaseHelper;

	@SingletonDependency
	ImChatSessionObjectHelper imChatSessionHelper;

	@SingletonDependency
	ImChatTemplateObjectHelper imChatTemplateHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaLogic mediaLogic;

	@SingletonDependency
	MenuGroupObjectHelper menuGroupHelper;

	@SingletonDependency
	MenuItemObjectHelper menuItemHelper;

	@SingletonDependency
	MessageTemplateLogic messageTemplateLogic;

	@SingletonDependency
	MessageTemplateSetObjectHelper messageTemplateSetHelper;

	@SingletonDependency
	PaypalAccountObjectHelper paypalAccountHelper;

	@SingletonDependency
	RandomLogic randomLogic;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	// implementation

	@Override
	public
	void createFixtures (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createFixtures");

		) {

			// menu

			menuItemHelper.insert (
				transaction,
				menuItemHelper.createInstance ()

				.setMenuGroup (
					menuGroupHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test",
						"facility"))

				.setCode (
					"im_chat")

				.setName (
					"IM Chat")

				.setDescription (
					"Instant message chat service")

				.setLabel (
					"IM Chat")

				.setTargetPath (
					"/imChats")

				.setTargetFrame (
					"main")

			);

			// message template databases

			MessageTemplateDatabaseRec primaryMessageTemplateDatabase =
				messageTemplateLogic.readMessageTemplateDatabaseFromClasspath (
					transaction,
					sliceHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test"),
					joinWithSlash (
						"/wbs/imchat/fixture",
						"im-chat-message-template-database.xml"));

			messageTemplateSetHelper.insert (
				transaction,
				messageTemplateSetHelper.createInstance ()

				.setMessageTemplateDatabase (
					primaryMessageTemplateDatabase)

				.setCode (
					"default")

				.setName (
					"Default")

				.setDescription (
					"")

			);

			MessageTemplateDatabaseRec embeddedMessageTemplateDatabase =
				messageTemplateLogic.readMessageTemplateDatabaseFromClasspath (
					transaction,
					sliceHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test"),
					joinWithSlash (
						"/wbs/imchat/fixture",
						"im-chat-embedded-message-template-database.xml"));

			messageTemplateSetHelper.insert (
				transaction,
				messageTemplateSetHelper.createInstance ()

				.setMessageTemplateDatabase (
					embeddedMessageTemplateDatabase)

				.setCode (
					"default")

				.setName (
					"Default")

				.setDescription (
					"")

			);

			// im chat

			ImChatRec imChat =
				imChatHelper.insert (
					transaction,
					imChatHelper.createInstance ()

				.setSlice (
					sliceHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test"))

				.setCode (
					"test")

				.setName (
					"Test")

				.setDescription (
					"Test IM chat")

				.setPaypalAccount (
					paypalAccountHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test",
						"sandbox"))

				.setBillingCurrency (
					currencyHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test",
						"gbp"))

				.setCreditCurrency (
					currencyHelper.findByCodeRequired (
						transaction,
						GlobalId.root,
						"test",
						"credit"))

				.setMessageTemplateDatabase (
					primaryMessageTemplateDatabase)

				.setPreferredQueueTime (
					5l * 60l)

				.setMessageCost (
					300l)

				.setFreeMessageLimit (
					3l)

				.setBillMessageEnabled (
					true)

				.setBillMessageMinChars (
					50l)

				.setBillMessageMaxChars (
					100l)

				.setFreeMessageEnabled (
					true)

				.setFreeMessageMinChars (
					10l)

				.setFreeMessageMaxChars (
					50l)

				.setDevelopmentMode (
					true)

				.setEmailFromName (
					"IM chat test")

				.setEmailFromAddress (
					"im-chat-test@wellbehavedsoftware.com")

				.setEmailReplyToAddress (
					"im-chat-test@wellbehavedsoftware.com")

				.setEmailSubjectForgotPassword (
					"New password for IM chat test")

				.setProfilePageBeforeLogin (
					true)

				.setDetailsPageOnFirstLogin (
					true)

			);

			// price points

			ImChatPricePointRec basicPricePoint =
				imChatPricePointHelper.insert (
					transaction,
					imChatPricePointHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"basic")

				.setName (
					"Basic")

				.setDescription (
					"Basic credit package")

				.setPublicName (
					"Basic")

				.setPublicDescription (
					"Basic credit package")

				.setPrice (
					599l)

				.setValue (
					600l)

				.setOrder (
					1l)

			);

			imChatPricePointHelper.insert (
				transaction,
				imChatPricePointHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"saver")

				.setName (
					"Saver")

				.setDescription (
					"Saver credit offer")

				.setPublicName (
					"Saver")

				.setPublicDescription (
					"Saver credit offer")

				.setPrice (
					1099l)

				.setValue (
					1200l)

				.setOrder (
					2l)

			);

			imChatPricePointHelper.insert (
				transaction,
				imChatPricePointHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"super_saver")

				.setName (
					"Super saver")

				.setDescription (
					"Super saver credit offer")

				.setPublicName (
					"Super saver")

				.setPublicDescription (
					"Super saver credit offer")

				.setPrice (
					1499l)

				.setValue (
					1800l)

				.setOrder (
					3l)

			);

			// im chat template

			for (
				int index = 0;
				index < 3;
				index ++
			) {

				imChatTemplateHelper.insert (
					transaction,
					imChatTemplateHelper.createInstance ()

					.setImChat (
						imChat)

					.setCode (
						stringFormat (
							"template_%s",
							integerToDecimalString (
								index)))

					.setName (
						stringFormat (
							"Template %s",
							integerToDecimalString (
								index)))

					.setDescription (
						stringFormat (
							"Test template %s",
							integerToDecimalString (
								index)))

					.setText (
						stringFormat (
							"Test IM chat template %s",
							integerToDecimalString (
								index)))

				);

			}

			// customer detail types

			imChatCustomerDetailTypeHelper.insert (
				transaction,
				imChatCustomerDetailTypeHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"name")

				.setName (
					"Name")

				.setDescription (
					"")

				.setLabel (
					"Name")

				.setHelp (
					"Please enter your name")

				.setRequired (
					true)

				.setRequiredLabel (
					"(required)")

				.setRestricted (
					false)

				.setWhenCreatingAccount (
					true)

				.setDataType (
					ImChatCustomerDetailDataType.text)

				.setOrdering (
					1l)

			);

			imChatCustomerDetailTypeHelper.insert (
				transaction,
				imChatCustomerDetailTypeHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"date_of_birth")

				.setName (
					"Date of birth")

				.setDescription (
					"")

				.setLabel (
					"Date of birth")

				.setHelp (
					"Please enter your date of birth")

				.setRequired (
					true)

				.setRequiredLabel (
					"")

				.setRestricted (
					false)

				.setWhenCreatingAccount (
					true)

				.setDataType (
					ImChatCustomerDetailDataType.dateOfBirth)

				.setMinimumAge (
					18l)

				.setOrdering (
					2l)

			);

			imChatCustomerDetailTypeHelper.insert (
				transaction,
				imChatCustomerDetailTypeHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"star_sign")

				.setName (
					"Star sign")

				.setDescription (
					"")

				.setLabel (
					"Star sign")

				.setHelp (
					"Please enter your star sign")

				.setRequired (
					false)

				.setRequiredLabel (
					"")

				.setRestricted (
					false)

				.setWhenCreatingAccount (
					false)

				.setDataType (
					ImChatCustomerDetailDataType.chooseOne)

				.setOrdering (
					3l)

			);

			imChatCustomerDetailTypeHelper.insert (
				transaction,
				imChatCustomerDetailTypeHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					"phone_number")

				.setName (
					"Phone number")

				.setDescription (
					"")

				.setLabel (
					"Phone number")

				.setHelp (
					"Please enter your phone number")

				.setRequired (
					false)

				.setRequiredLabel (
					"(required)")

				.setRestricted (
					true)

				.setWhenCreatingAccount (
					false)

				.setDataType (
					ImChatCustomerDetailDataType.text)

				.setOrdering (
					4l)

			);

			// im chat profile

			MediaRec dougalMedia =
				mediaLogic.createMediaFromImageRequired (
					transaction,
					fileReadBytes (
						"binaries/test/dougal.jpg"),
					"image/jpeg",
					"dougal.jpg");

			MediaRec ermintrudeMedia =
				mediaLogic.createMediaFromImageRequired (
					transaction,
					fileReadBytes (
						"binaries/test/dougal.jpg"),
					"image/jpeg",
					"ermintrude.jpg");

			List<ImChatProfileRec> profiles =
				new ArrayList<ImChatProfileRec> ();

			for (
				int index = 0;
				index < 10;
				index ++
			) {

				profiles.add (
					imChatProfileHelper.insert (
						transaction,
						imChatProfileHelper.createInstance ()

					.setImChat (
						imChat)

					.setCode (
						stringFormat (
							"profile_%s",
							integerToDecimalString (
								index)))

					.setName (
						stringFormat (
							"Profile %s",
							integerToDecimalString (
								index)))

					.setDescription (
						stringFormat (
							"Test IM chat profile %s",
							integerToDecimalString (
								index)))

					.setState (
						ImChatProfileState.ready)

					.setPublicName (
						stringFormat (
							"Profile %s",
							integerToDecimalString (
								index)))

					.setPublicDescription (
						stringFormat (
							"Test IM chat profile %s",
							integerToDecimalString (
								index)))

					.setPublicDescriptionShort (
						stringFormat (
							"Test IM chat profile %s",
							integerToDecimalString (
								index)))

					.setProfileImage (
						index % 2 == 0
							? dougalMedia
							: ermintrudeMedia)

				));

			}

			// im chat customer

			ImChatCustomerRec imChatCustomer =
				imChatCustomerHelper.insert (
					transaction,
					imChatCustomerHelper.createInstance ()

				.setImChat (
					imChat)

				.setCode (
					randomLogic.generateNumericNoZero (8))

				.setEmail (
					"test@example.com")

				.setPassword (
					"topsecret")

				.setFirstSession (
					transaction.now ())

				.setLastSession (
					transaction.now ())

			);

			// im chat conversation

			ImChatConversationRec imChatConversation =
				imChatConversationHelper.insert (
					transaction,
					imChatConversationHelper.createInstance ()

				.setImChatCustomer (
					imChatCustomer)

				.setIndex (
					imChatCustomer.getNumConversations ())

				.setImChatProfile (
					profiles.get (0))

				.setStartTime (
					transaction.now ())

				.setPendingReply (
					false)

			);

			imChatCustomer

				.setNumConversations (
					imChatCustomer.getNumConversations () + 1);

			// im chat session

			ImChatSessionRec imChatSession =
				imChatSessionHelper.insert (
					transaction,
					imChatSessionHelper.createInstance ()

				.setImChatCustomer (
					imChatCustomer)

				.setSecret (
					randomLogic.generateLowercase (20))

				.setStartTime (
					transaction.now ())

				.setUpdateTime (
					transaction.now ())

				.setEndTime (
					transaction.now ())

				.setActive (
					true)

				.setUserAgentText (
					textHelper.findOrCreate (
						transaction,
						"User agent"))

				.setIpAddress (
					"1.2.3.4")

			);

			// im chat purchase

			imChatPurchaseHelper.insert (
				transaction,
				imChatPurchaseHelper.createInstance ()

				.setImChatCustomer (
					imChatCustomer)

				.setIndex (
					imChatCustomer.getNumPurchases ())

				.setImChatSession (
					imChatSession)

				.setImChatPricePoint (
					basicPricePoint)

				.setState (
					ImChatPurchaseState.unknown)

				.setPrice (
					599l)

				.setValue (
					600l)

				.setCreatedTime (
					transaction.now ())

				.setPaypalPayment (
					null)

			);

			imChatCustomer

				.setNumPurchases (
					imChatCustomer.getNumPurchases () + 1);

			// im chat message

			imChatMessageHelper.insert (
				transaction,
				imChatMessageHelper.createInstance ()

				.setImChatConversation (
					imChatConversation)

				.setIndex (
					imChatConversation.getNumMessages ())

				.setMessageText (
					"Text message.")

				.setTimestamp (
					transaction.now ())

			);

			imChatConversation

				.setNumMessages (
					imChatConversation.getNumMessages () + 1);

		}

	}

}
