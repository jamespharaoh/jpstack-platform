package wbs.platform.user.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.web.utils.HtmlAttributeUtils.htmlAttributeFormat;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockClose;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockOpen;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockWrite;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.context.ConsoleApplicationScriptRef;
import wbs.console.html.HtmlLink;
import wbs.console.html.ScriptRef;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.group.model.GroupRec;
import wbs.platform.priv.console.PrivConsoleHelper;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.scaffold.model.RootRec;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("userPrivsEditorPart")
public
class UserPrivsEditorPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	PrivConsoleHelper privHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleHelper userHelper;

	// state

	PrivsEditorNode rootNode =
		new PrivsEditorNode ();

	Set <Long> canPrivIds =
		new HashSet<> ();

	Set <Long> canGrantPrivIds =
		new HashSet<> ();

	Set <Long> groupPrivIds =
		new HashSet<> ();

	Map <GlobalId, PrivsEditorNode> nodesByDataObjectId =
		new HashMap<> ();

	// implementation

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef>of (

			ConsoleApplicationScriptRef.javascript (
				"/js/tree.js")

		);

	}

	@Override
	public
	Set <HtmlLink> links () {

		return ImmutableSet.<HtmlLink> builder ()

			.addAll (
				super.links ())

			.add (
				HtmlLink.applicationCssStyle (
					"/style/user-privs-editor.css"))

			.build ();

	}

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

			// build the privs tree

			List <PrivRec> privs =
				privHelper.findAll (
					transaction);

			for (
				PrivRec priv
					: privs
			) {

				if (
					! privChecker.canGrant (
						transaction,
						priv.getId ())
				) {
					continue;
				}

				Record <?> parentObject =
					objectManager.getParentRequired (
						transaction,
						priv);

				PrivsEditorNode parentNode =
					findNode (
						transaction,
						parentObject);

				parentNode.privs.put (
					priv.getCode (),
					priv);

			}

			// and fill the current priv data sets

			UserRec user =
				userHelper.findFromContextRequired (
					transaction);

			for (
				UserPrivRec userPriv
					: user.getUserPrivs ()
			) {

				if (userPriv.getCan ()) {

					canPrivIds.add (
						userPriv.getPriv ().getId ());

				}

				if (userPriv.getCanGrant ()) {

					canGrantPrivIds.add (
						userPriv.getPriv ().getId ());

				}

			}

			for (
				GroupRec group
					: user.getGroups ()
			) {

				for (
					PrivRec priv
						: group.getPrivs ()
				) {

					groupPrivIds.add (
						priv.getId ());

				}
			}

			// now check which tree nodes to expand initially

			doExpansion (
				rootNode);

		}

	}

	private
	PrivsEditorNode findNode (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findNode");

		) {

			// end the recursion if we reach the root node

			if ((Object) object instanceof RootRec)
				return rootNode;

			// check if the node already exists

			GlobalId dataObjectId =
				objectManager.getGlobalId (
					transaction,
					object);

			PrivsEditorNode node =
				nodesByDataObjectId.get (
					dataObjectId);

			if (node != null)
				return node;

			// find the parent node by recursion

			Record <?> parent =
				objectManager.getParentRequired (
					transaction,
					object);

			PrivsEditorNode parentNode =
				findNode (
					transaction,
					parent);

			// lookup or create the map containing this node's children of this
			// type

			Map <String, PrivsEditorNode> typeNodesByCode =
				parentNode.nodesByCodeByTypeCode.get (
					objectManager.getObjectTypeCode (
						transaction,
						object));

			if (typeNodesByCode == null) {

				typeNodesByCode =
					new TreeMap <String, PrivsEditorNode> ();

				parentNode.nodesByCodeByTypeCode.put (
					objectManager.getObjectTypeCode (
						transaction,
						object),
					typeNodesByCode);
			}

			// lookup or create the node for this object

			node =
				typeNodesByCode.get (
					objectManager.getCode (
						transaction,
						object));

			if (node == null) {

				node =
					new PrivsEditorNode ();

				typeNodesByCode.put (
					objectManager.getCode (
						transaction,
						object),
					node);

				nodesByDataObjectId.put (
					dataObjectId,
					node);

			}

			// and return

			return node;

		}

	}

	@Override
	public
	void renderHtmlHeadContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlHeadContent");

		) {

			super.renderHtmlHeadContent (
				transaction,
				formatWriter);

			renderScriptBlock (
				transaction,
				formatWriter);

		}

	}

	private
	void renderScriptBlock (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderScriptBlock");

		) {

			// script open

			htmlScriptBlockOpen (
				formatWriter);

			// new priv data

			formatWriter.writeLineFormat (
				"var newPrivData = {}");

			// tree opts

			formatWriter.writeLineFormatIncreaseIndent (
				"var treeOpts = {");

			formatWriter.writeLineFormat (
				"blankImage: '%j',",
				requestContext.resolveApplicationUrl (
					"/images/tree-blank.gif"));

			formatWriter.writeLineFormat (
				"plusImage: '%j',",
				requestContext.resolveApplicationUrl (
					"/images/tree-plus.gif"));

			formatWriter.writeLineFormat (
				"minusImage: '%j',",
				requestContext.resolveApplicationUrl (
					"/images/tree-minus.gif"));

			formatWriter.writeLineFormat (
				"imageWidth: 17,");

			formatWriter.writeLineFormat (
				"imageHeight: 17");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			// ob

			formatWriter.writeLineFormat (
				"var ob = new SimpleTreeItemGenerator ('%j');",
				requestContext.resolveApplicationUrl (
					"/images/tree-object.gif"));

			formatWriter.writeLineFormatIncreaseIndent (
				"ob.generateTail = function (itemData) {");

			formatWriter.writeLineFormat (
				"return ' <span class=\"node\">' + itemData[2] + '</span>';");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			// pr

			formatWriter.writeLineFormat (
				"var pr = new SimpleTreeItemGenerator ('%j');",
				requestContext.resolveApplicationUrl (
					"/images/tree-priv.gif"));

			// next priv doc id

			formatWriter.writeLineFormat (
				"var nextPrivDocId = 0;");

			// pr generate tail

			formatWriter.writeLineFormatIncreaseIndent (
				"pr.generateTail = function (itemData) {");

			formatWriter.writeLineFormat (
				"var privDocId = nextPrivDocId ++;");

			formatWriter.writeLineFormat (
				"s = ' <span class=\"priv\">' + itemData[2] + '</span>'");

			formatWriter.writeLineFormat (
				"  + ' |'");

			formatWriter.writeLineFormat (
				"  + ' <a href=\"javascript:togglePriv (' + privDocId + ', &quot;",
				"can&quot;, ' + itemData[6] + ')\"'");

			formatWriter.writeLineFormat (
				"  + ' id=\"' + privDocId + '-can\"'");

			formatWriter.writeLineFormat (
				"  + ' class=\"' + (itemData[3]? 'can-on' : 'can-off') + '\">'");

			formatWriter.writeLineFormat (
				"  + 'can</a>'");

			formatWriter.writeLineFormat (
				"  + ' |'");

			formatWriter.writeLineFormat (
				"  + ' <a href=\"javascript:togglePriv (' + privDocId + ', &quot;",
				"cangrant&quot;, ' + itemData[6] + ')\"'");

			formatWriter.writeLineFormat (
				"  + ' id=\"' + privDocId + '-cangrant\"'");

			formatWriter.writeLineFormat (
				"  + ' class=\"' + (itemData[4]? 'cangrant-on' : 'cangrant-off') ",
				"+ '\">'");

			formatWriter.writeLineFormat (
				"  + 'can grant</a>';");

			formatWriter.writeLineFormatIncreaseIndent (
				"if (itemData[5]) {");

			formatWriter.writeLineFormat (
				"s += ' | <span class=\"group-can\">(group can)</span>';");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			formatWriter.writeLineFormat (
				"return s;");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			// toggle priv

			formatWriter.writeLineFormatIncreaseIndent (
				"function togglePriv (id, type, privId) {");

			formatWriter.writeLineFormat (
				"var a = document.getElementById (id + '-' + type);");

			formatWriter.writeLineFormatIncreaseIndent (
				"if (a.className == type + '-off') {");

			formatWriter.writeLineFormat (
				"a.className = type + '-on';");

			formatWriter.writeLineFormat (
				"newPrivData[privId + '-' + type] = 1;");

			formatWriter.writeLineFormatDecreaseIncreaseIndent (
				"} else {");

			formatWriter.writeLineFormat (
				"a.className = type + '-off';");

			formatWriter.writeLineFormat (
				"newPrivData[privId + '-' + type] = 0;");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			// get update string

			formatWriter.writeLineFormatIncreaseIndent (
				"function getUpdateString () {");

			formatWriter.writeLineFormat (
				"var s = '';");

			formatWriter.writeLineFormatIncreaseIndent (
				"for (var key in newPrivData) {");

			formatWriter.writeLineFormat (
				"if (s.length > 0) s += ',';");

			formatWriter.writeLineFormat (
				"s += key + '=' + newPrivData[key];");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			formatWriter.writeLineFormat (
				"return s;");

			formatWriter.writeLineFormatDecreaseIndent (
				"}");

			// tree data

			formatWriter.writeLineFormatIncreaseIndent (
				"var treeData = [");

			goNode (
				formatWriter,
				rootNode);

			formatWriter.writeLineFormatDecreaseIndent (
				"]");

			// script close

			htmlScriptBlockClose (
				formatWriter);

		}

	}

	void goNode (
			@NonNull FormatWriter formatWriter,
			@NonNull PrivsEditorNode node) {

		for (
			PrivRec priv
				: node.privs.values ()
		) {

			formatWriter.writeLineFormat (
				"[pr, 0, '%h', %s, %s, %s, %s],",
				priv.getCode (),
				canPrivIds.contains (priv.getId ())
					? "1"
					: "0",
				canGrantPrivIds.contains (priv.getId ())
					? "1"
					: "0",
				groupPrivIds.contains (priv.getId ())
					? "1"
					: "0",
				integerToDecimalString (
					priv.getId ()));

		}

		for (
			Map.Entry <String, Map <String, PrivsEditorNode>> nodesByCodeEntry
				: node.nodesByCodeByTypeCode.entrySet ()
		) {

			String typeCode =
				nodesByCodeEntry.getKey ();

			Map <String, PrivsEditorNode> typeNodesByCode =
				nodesByCodeEntry.getValue ();

			boolean expanded = false;

			for (
				PrivsEditorNode childNode
					: typeNodesByCode.values ()
			) {

				if (childNode.expanded) {

					expanded = true;

					break;

				}

			}

			formatWriter.writeLineFormatIncreaseIndent (
				"[ob, %s, '%h', [",
				expanded
					? "2"
					: "1",
				typeCode);

			for (
				Map.Entry <String, PrivsEditorNode> typeNodeEntry
					: typeNodesByCode.entrySet ()
			) {

				String code =
					typeNodeEntry.getKey ();

				PrivsEditorNode childNode =
					typeNodeEntry.getValue ();

				formatWriter.writeLineFormatIncreaseIndent (
					"[ob, %s, '%h', [",
					childNode.expanded
						? "2"
						: "1",
					code);

				goNode (
					formatWriter,
					childNode);

				formatWriter.writeLineFormatDecreaseIndent (
					"]],");

			}

			formatWriter.writeLineFormatDecreaseIndent (
				"]],");

		}

	}

	public
	boolean doExpansion (
			PrivsEditorNode node) {

		boolean expanded = false;

		// see if we have any active privs

		for (PrivRec priv
				: node.privs.values ()) {

			if (
				canPrivIds.contains (priv.getId ())
				|| canGrantPrivIds.contains (priv.getId ())
			) {

				expanded = true;

				break;

			}

		}

		// then call doExpansion for all child nodes

		for (Map<String,PrivsEditorNode> typeNodesByCode
				: node.nodesByCodeByTypeCode.values ()) {

			for (PrivsEditorNode childNode
					: typeNodesByCode.values ()) {

				if (doExpansion (childNode))
					expanded = true;

			}

		}

		// update this node's expanded status

		node.expanded = expanded;

		// and return

		return expanded;

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

			// form open

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveLocalUrl (
					"/user.privs.editor"),
				htmlAttributeFormat (
					"onsubmit",
					"javascript:%s = %s",
					"document.getElementById ('privdata').value",
					"getUpdateString ()"));

			// form hidden

			formatWriter.writeLineFormat (
				"<input",
				" type=\"hidden\"",
				" name=\"privdata\"",
				" id=\"privdata\"",
				" value=\"\"",
				">");

			// form controls

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" value=\"save changes\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// build tree script

			htmlScriptBlockWrite (
				formatWriter,
				"document.write (buildTree (treeData, treeOpts));");

			// form controls

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" value=\"save changes\"",
				">");

			htmlParagraphClose (
				formatWriter);

			// form close

			htmlFormClose (
				formatWriter);

		}

	}

	// data structures

	static
	class PrivsEditorNode {

		Map<String,Map<String,PrivsEditorNode>>
			nodesByCodeByTypeCode =
				new TreeMap<String,Map<String,PrivsEditorNode>> ();

		Map<String,PrivRec> privs =
			new TreeMap<String,PrivRec> ();

		boolean expanded = false;

	}

}
