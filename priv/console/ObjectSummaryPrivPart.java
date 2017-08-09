package wbs.platform.priv.console;

import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.joinWithCommaAndSpace;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenGetAction;
import static wbs.web.utils.HtmlInputUtils.htmlOptionWrite;
import static wbs.web.utils.HtmlInputUtils.htmlSelectClose;
import static wbs.web.utils.HtmlInputUtils.htmlSelectOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.lookup.ObjectLookup;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.group.console.GroupConsoleHelper;
import wbs.platform.group.model.GroupRec;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("objectSummaryPrivPart")
public
class ObjectSummaryPrivPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	GroupConsoleHelper groupHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserObjectHelper userHelper;

	// state

	Record <?> object;

	Map <String, UserPrivSets> userPrivs =
		new TreeMap<> ();

	Map <String, Set <String>> groupPrivs =
		new TreeMap<> ();

	Map <String, UserRec> users =
		new TreeMap<> ();

	Map <String, GroupRec> groups =
		new TreeMap<> ();

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			ObjectLookup <? extends Record <?>> objectLookup =
				genericCastUnchecked (
					requestContext.stuff (
						"dataObjectLookup"));

			object =
				objectLookup.lookupObject (
					transaction,
					requestContext.consoleContextStuffRequired ());

			List <PrivRec> privs =
				objectManager.getChildren (
					transaction,
					object,
					PrivRec.class);

			// for each priv

			for (
				PrivRec priv
					: privs
			) {

				// build user priv list

				for (
					UserPrivRec userPriv
						: priv.getUserPrivs ()
				) {

					String userPath =
						objectManager.objectPathMini (
							transaction,
							userPriv.getUser ());

					UserPrivSets userPrivSets =
						userPrivs.get (userPath);

					if (userPrivSets == null) {

						userPrivs.put (
							userPath,
							userPrivSets = new UserPrivSets ());

					}

					if (userPriv.getCan ()) {

						userPrivSets.canPrivCodes.add (
							priv.getCode ());

					}

					if (userPriv.getCanGrant ()) {

						userPrivSets.canGrantPrivCodes.add (
							priv.getCode ());

					}

				}

				// build group priv list

				for (
					GroupRec group
						: priv.getGroups ()
				) {

					String groupPath =
						objectManager.objectPathMini (
							transaction,
							group);

					Set<String> privCodes =
						groupPrivs.get (groupPath);

					if (privCodes == null) {

						privCodes =
							new TreeSet<String>();

						groupPrivs.put (
							groupPath,
							privCodes);

					}

					privCodes.add (
						priv.getCode ());

				}

			}

			for (
				UserRec user
					: userHelper.findAll (
						transaction)
			) {

				if (
					! objectManager.canView (
						transaction,
						privChecker,
						user)
				) {
					continue;
				}

				users.put (
					objectManager.objectPathMini (
						transaction,
						user),
					user);

			}

			for (
				GroupRec group
					: groupHelper.findAll (
						transaction)
			) {

				if (
					! objectManager.canView (
						transaction,
						privChecker,
						group)
				) {
					continue;
				}

				groups.put (
					objectManager.objectPathMini (
						transaction,
						group),
					group);

			}

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			renderUsers (
				transaction,
				formatWriter);

			renderGroups (
				transaction,
				formatWriter);

		}

	}

	private
	void renderUsers (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderUsers");

		) {

			htmlHeadingTwoWrite (
				formatWriter,
				"Users");

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"User",
				"Privs",
				"Grant privs");

			if (userPrivs.size () == 0) {

				htmlTableCellWrite (
					formatWriter,
					"No user privs to show",
					htmlColumnSpanAttribute (3l));

			}

			for (
				Map.Entry <String, UserPrivSets> entry
					: userPrivs.entrySet ()
			) {

				String userPath =
					entry.getKey ();

				UserPrivSets userPrivSets =
					entry.getValue ();

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					userPath);

				htmlTableCellWrite (
					formatWriter,
					joinWithCommaAndSpace (
						userPrivSets.canPrivCodes));

				htmlTableCellWrite (
					formatWriter,
					joinWithCommaAndSpace (
						userPrivSets.canGrantPrivCodes));

				htmlTableRowClose (
					formatWriter);

			}

			// table close

			htmlTableClose (
				formatWriter);

			// form open

			htmlFormOpenGetAction (
				formatWriter,
				"");

			// edit privs

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeFormat (
				"Edit privs for user to user<br>");

			htmlSelectOpen (
				formatWriter,
				"userId");

			for (
				Map.Entry <String, UserRec> entry :
					users.entrySet ()
			) {

				htmlOptionWrite (
					formatWriter,
					entry.getValue ().getId ().toString (),
					entry.getKey ());

			}

			htmlSelectClose (
				formatWriter);

			formatWriter.writeFormat (
				"<input",
				" type=\"submit\"",
				" value=\"go\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// form close

			htmlFormClose (
				formatWriter);

		}

	}

	private
	void renderGroups (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderGroups");

		) {

			// heading

			htmlHeadingTwoWrite (
				formatWriter,
				"Groups");

			// table open

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Group",
				"Privs");

			if (groupPrivs.size () == 0) {

				htmlTableCellWrite (
					formatWriter,
					"No user privs to show",
					htmlColumnSpanAttribute (3l));

			}

			for (
				Map.Entry <String, Set <String>> entry
					: groupPrivs.entrySet ()
			) {

				String groupPath =
					entry.getKey ();

				Set <String> privCodes =
					entry.getValue ();

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					groupPath);

				htmlTableCellWrite (
					formatWriter,
					joinWithCommaAndSpace (
						privCodes));

				htmlTableRowClose (
					formatWriter);

			}

			// table close

			htmlTableClose (
				formatWriter);

		}

	}

	@Accessors (fluent = true)
	@Data
	public static
	class UserPrivSets {

		Set<String> canPrivCodes =
			new TreeSet<String> ();

		Set<String> canGrantPrivCodes =
			new TreeSet<String> ();

	}

}
