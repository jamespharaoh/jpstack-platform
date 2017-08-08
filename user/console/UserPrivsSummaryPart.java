package wbs.platform.user.console;

import static wbs.utils.etc.LogicUtils.booleanToYesNo;
import static wbs.utils.string.StringUtils.joinWithCommaAndSpace;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.NonNull;

import org.apache.commons.lang3.builder.CompareToBuilder;

import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.group.model.GroupRec;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("userPrivsSummaryPart")
public
class UserPrivsSummaryPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserConsoleHelper userHelper;

	// state

	Set <PrivStuff> privStuffs;

	// implementation

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

			Map <Long, PrivStuff> privStuffsByPrivId =
				new HashMap<> ();

			UserRec user =
				userHelper.findFromContextRequired (
					transaction);

			// load up some info about the acting user

			for (
				UserPrivRec userPriv
					: user.getUserPrivs ()
			) {

				// check we can see this priv

				if (
					! privChecker.canGrant (
						transaction,
						userPriv.getPriv ().getId ())
				) {
					continue;
				}

				PrivRec priv =
					userPriv.getPriv ();

				// create PrivStuff

				PrivStuff privStuff =
					new PrivStuff ();

				Record <?> parent =
					objectManager.getParentRequired (
						transaction,
						priv);

				privStuff.path =
					objectManager.objectPath (
						transaction,
						parent,
						userConsoleLogic.sliceRequired (
							transaction));

				privStuff.privCode =
					priv.getCode ();

				privStuff.userPriv =
					userPriv;

				privStuffsByPrivId.put (
					priv.getId (),
					privStuff);

			}

			for (GroupRec group
					: user.getGroups ()) {

				for (PrivRec priv
						: group.getPrivs ()) {

					// check we can see this priv

					if (
						! privChecker.canGrant (
							transaction,
							priv.getId ())
					) {
						continue;
					}

					// find or create the priv stuff

					PrivStuff privStuff =
						privStuffsByPrivId.get (
							priv.getId ());

					if (privStuff == null) {

						privStuff =
							new PrivStuff ();

						Record <?> parent =
							objectManager.getParentRequired (
								transaction,
								priv);

						privStuff.path =
							objectManager.objectPath (
								transaction,
								parent,
								userConsoleLogic.sliceRequired (
									transaction));

						privStuff.privCode =
							priv.getCode ();

						privStuffsByPrivId.put (
							priv.getId (),
							privStuff);

					}

					// and add this group to it

					privStuff.groups.add (
						group.getCode ());

				}

			}

			privStuffs =
				new TreeSet<PrivStuff> (
					privStuffsByPrivId.values ());

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

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Object",
				"Priv",
				"Can",
				"Grant",
				"Groups");

			for (
				PrivStuff privStuff
					: privStuffs
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					privStuff.path);

				htmlTableCellWrite (
					formatWriter,
					privStuff.privCode);

				htmlTableCellWrite (
					formatWriter,
					booleanToYesNo (
						privStuff.userPriv != null
						&& privStuff.userPriv.getCan ()));

				htmlTableCellWrite (
					formatWriter,
					booleanToYesNo (
						privStuff.userPriv != null
						&& privStuff.userPriv.getCanGrant ()));

				htmlTableCellWrite (
					formatWriter,
					joinWithCommaAndSpace (
						privStuff.groups));

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

		}

	}

	class PrivStuff
		implements Comparable <PrivStuff> {

		String path;
		String privCode;
		UserPrivRec userPriv;

		List <String> groups =
			new ArrayList<> ();

		@Override
		public
		int compareTo (
				PrivStuff other) {

			return new CompareToBuilder ()
				.append (path, other.path)
				.append (privCode, other.privCode)
				.toComparison ();

		}

	}

}
