package wbs.platform.core.console;

import static wbs.web.utils.HtmlAttributeUtils.htmlAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import lombok.NonNull;

import org.apache.commons.lang3.tuple.Pair;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;

import wbs.platform.menu.console.MenuGroupConsoleHelper;
import wbs.platform.menu.model.MenuGroupRec;
import wbs.platform.menu.model.MenuItemRec;
import wbs.platform.scaffold.model.SliceObjectHelper;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserObjectHelper;

import wbs.utils.string.FormatWriter;

import wbs.web.utils.ABSwap;

@PrototypeComponent ("coreSidebarMenuResponder")
public
class CoreSidebarMenuResponder
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MenuGroupConsoleHelper menuGroupHelper;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	SliceObjectHelper sliceHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserObjectHelper userHelper;

	@SingletonDependency
	WbsConfig wbsConfig;

	// state

	List <MenuGroupRec> menuGroups;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			SliceRec defaultSlice =
				sliceHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					wbsConfig.defaultSlice ());

			menuGroups =
				menuGroupHelper.findByParent (
					transaction,
					defaultSlice);

			Collections.sort (
				menuGroups,
				Ordering.natural ().onResultOf (
					menuGroup ->
						Pair.of (
							menuGroup.getOrder (),
							menuGroup.getCode ())));

		}

	}

	@Override
	protected
	void renderHtmlBodyContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContents");

		) {

			htmlTableOpen (
				formatWriter,
				htmlClassAttribute (
					"menu"),
				htmlAttribute (
					"width",
					"100%"));

			ABSwap abSwap =
				new ABSwap ();

			for (
				MenuGroupRec menuGroup
					: menuGroups
			) {

				boolean doneGroup = false;

				for (
					MenuItemRec menu
						: menuGroup.getMenus ()
				) {

					if (menu.getDeleted ())
						continue;

					if (
						! objectManager.canView (
							transaction,
							menu)
					) {
						continue;
					}

					if (! doneGroup) {

						htmlTableRowOpen (
							formatWriter);

						htmlTableHeaderCellWrite (
							formatWriter,
							menuGroup.getLabel ());

						htmlTableRowClose (
							formatWriter);

						doneGroup = true;

					}

					htmlTableRowOpen (
						formatWriter,

						htmlClassAttribute (
							"magic-table-row",
							abSwap.swap ()),

						htmlDataAttribute (
							"target-href",
							requestContext.resolveApplicationUrl (
								menu.getTargetPath ())),

						htmlDataAttribute (
							"target-frame",
							menu.getTargetFrame ())

					);

					htmlTableCellWrite (
						formatWriter,
						menu.getLabel ());

					htmlTableRowClose (
						formatWriter);

				}

			}

			htmlTableClose (
				formatWriter);

		}

	}

}
