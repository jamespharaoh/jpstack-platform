package wbs.platform.priv.console;

import static wbs.utils.collection.CollectionUtils.listSorted;
import static wbs.utils.collection.IterableUtils.iterableFilter;
import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.collection.IterableUtils.iterableMapToSet;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.group.model.GroupRec;
import wbs.platform.object.core.console.ObjectTypeConsoleHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.user.console.UserPrivConsoleHelper;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

import wbs.utils.string.FormatWriter;

@Accessors (fluent = true)
@PrototypeComponent ("objectPrivsPart")
public
class ObjectPrivsPart <
	ObjectType extends Record <ObjectType>
>
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeConsoleHelper objectTypeHelper;

	@SingletonDependency
	PrivConsoleHelper privHelper;

	@SingletonDependency
	UserPrivConsoleHelper userPrivHelper;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	// state

	ObjectType object;

	Set <String> privCodes;

	List <PrivData> privDatas;

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

			object =
				consoleHelper.findFromContextRequired (
					transaction);

			privCodes =
				iterableMapToSet (
					privHelper.findByParent (
						transaction,
						object),
					PrivRec::getCode);

			privDatas =
				preparePrivDatas (
					transaction,
					object);

		}

	}

	private
	List <PrivData> preparePrivDatas (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"preparePrivDatas");

		) {

			ImmutableList.Builder <PrivData> objectPrivDatasBuilder =
				ImmutableList.builder ();

			for (

				Optional <Record <?>> currentObjectLoop =
					optionalOf (
						object);

				optionalIsPresent (
					currentObjectLoop);

				currentObjectLoop =
					objectManager.getParent (
						transaction,
						optionalGetRequired (
							currentObjectLoop))

			) {

				Record <?> currentObject =
					optionalGetRequired (
						currentObjectLoop);

				ConsoleHelper <?> currentObjectHelper =
					objectManager.consoleHelperForObjectRequired (
						genericCastUnchecked (
							currentObject));

				ObjectTypeRec currentObjectType =
					objectTypeHelper.findRequired (
						transaction,
						currentObjectHelper.objectTypeId ());

				objectPrivDatasBuilder.addAll (
					iterableMapToList (
						listSorted (
							iterableFilter (
								privHelper.findByParent (
									transaction,
									currentObject),
								priv ->
									privCodes.contains (
										priv.getCode ()))),
						priv ->
							preparePrivData (
								transaction,
								currentObjectType,
								currentObject,
								priv)));

			}

			return objectPrivDatasBuilder.build ();

		}

	}

	private
	PrivData preparePrivData (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectTypeRec objectType,
			@NonNull Record <?> object,
			@NonNull PrivRec priv) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"preparePrivData");

		) {

			return new PrivData ()

				.objectType (
					objectType)

				.object (
					object)

				.priv (
					priv)

				.users (
					iterableMapToList (
						iterableFilter (
							userPrivHelper.find (
								transaction,
								priv),
							UserPrivRec::getCan),
						UserPrivRec::getUser))

				.groups (
					listSorted (
						priv.getGroups ()))

			;

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
				"Type",
				"Object",
				"Priv",
				"Description",
				"Users",
				"Groups");

			for (
				PrivData privData
					: privDatas
			) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					privData.objectType.getCode ());

				htmlTableCellWrite (
					formatWriter,
					objectManager.objectPathMini (
						transaction,
						privData.object ()));

				htmlTableCellWrite (
					formatWriter,
					privData.priv ().getCode ());

				htmlTableCellWrite (
					formatWriter,
					privData.priv ().getPrivType ().getDescription ());

				htmlTableCellWriteHtml (
					formatWriter,
					() -> privData.users ().forEach (
						user -> formatWriter.writeLineFormat (
							"%h.%h<br>",
							user.getSlice ().getCode (),
							user.getUsername ())));

				htmlTableCellWrite (
					formatWriter,
					() -> privData.groups ().forEach (
						group -> formatWriter.writeLineFormat (
							"%h.%h<br>",
							group.getSlice ().getCode (),
							group.getCode ())));

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

		}

	}

	// data

	@Accessors (fluent = true)
	@Data
	private static
	class PrivData {

		ObjectTypeRec objectType;
		Record <?> object;

		PrivRec priv;

		List <UserRec> users;
		List <GroupRec> groups;

	}

}
