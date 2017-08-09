package wbs.platform.object.summary;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.collection.CollectionUtils.singletonList;
import static wbs.utils.etc.Misc.todo;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.core.ConsoleFormBuilder;
import wbs.console.forms.core.ConsoleFormManager;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.forms.object.ParentFormFieldSpec;
import wbs.console.forms.types.FormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleMetaModuleImplementation;
import wbs.console.module.ConsoleModuleBuilderComponent;
import wbs.console.module.ConsoleModuleImplementation;
import wbs.console.part.TextPart;
import wbs.console.responder.ConsoleFile;
import wbs.console.tab.ConsoleContextTab;
import wbs.console.tab.TabContextResponder;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

@Accessors (fluent = true)
@PrototypeComponent ("objectSummaryPageBuilder")
public
class ObjectSummaryPageBuilder <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@SingletonDependency
	ConsoleFormBuilder consoleFormBuilder;

	@SingletonDependency
	ConsoleFormManager consoleFormManager;

	@SingletonDependency
	ConsoleFormManager formContextManager;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFileProvider;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTabProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSummaryPart> objectSummaryPartProvider;

	@PrototypeDependency
	ComponentProvider <ParentFormFieldSpec> parentFieldProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSummaryFieldsPart <ObjectType, ParentType>>
		summaryFieldsPartProvider;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponderPrvider;

	@PrototypeDependency
	ComponentProvider <TextPart> textPartProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectSummaryPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	@Getter
	ConsoleHelper <ObjectType> consoleHelper;

	ConsoleFormType <ObjectType> formType;

	String privKey;

	// build meta

	public
	void buildMeta (
			@NonNull ConsoleMetaModuleImplementation consoleMetaModule) {

	}

	// build

	@BuildMethod
	@Override
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder <TaskLogger> builder) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			setDefaults (
				taskLogger);

			for (
				ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
					: consoleMetaManager.resolveExtensionPoint (
						taskLogger,
						container.extensionPointName ())
			) {

				buildContextTabs (
					taskLogger,
					resolvedExtensionPoint);

				buildContextFile (
					taskLogger,
					resolvedExtensionPoint);

			}

/*
			builder.descend (
				taskLogger,
				spec,
				spec.builders (),
				this,
				MissingBuilderBehaviour.error);
*/

		}

	}

	void buildContextTabs (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildContextTabs");

		) {

			consoleModule.addContextTab (
				taskLogger,
				"end",
				contextTabProvider.provide (
					taskLogger,
					contextTab ->
						contextTab

					.name (
						stringFormat (
							"%s.summary",
							container.pathPrefix ()))

					.defaultLabel (
						"Summary")

					.localFile (
						stringFormat (
							"%s.summary",
							container.pathPrefix ()))

					.privKeys (
						privKey)

				),
				extensionPoint.contextTypeNames ());

		}

	}

	void buildContextFile (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildContextFile");

		) {

			consoleModule.addContextFile (
				stringFormat (
					"%s.summary",
					container.pathPrefix ()),
				consoleFileProvider.provide (
					taskLogger,
					consoleFile ->
						consoleFile

					.getResponderName (
						taskLogger,
						stringFormat (
							"%sSummaryResponder",
							container.newBeanNamePrefix ()))

					.privKeys (
						taskLogger,
						privKey != null
							? singletonList (
								privKey)
							: emptyList ())

				),
				extensionPoint.contextTypeNames ());

		}

	}

	public
	ObjectSummaryPageBuilder <ObjectType, ParentType> addFieldsPart (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull String fieldsName) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"addFieldsPart");
		) {

			return addFieldsPart (
				taskLogger,
				consoleFormManager.createFormType (
					taskLogger,
					consoleModule,
					fieldsName,
					consoleHelper.objectClass (),
					genericCastUnchecked (
						consoleHelper.parentClass ()),
					FormType.readOnly,
					optionalOf (
						fieldsName),
					optionalAbsent ()));

		}

	}

	public
	ObjectSummaryPageBuilder <ObjectType, ParentType> addFieldsPart (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleFormType <ObjectType> formType) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"addFieldsPart");

		) {

			throw todo ();

			/*
			PagePartFactory partFactory =
				parentTransaction -> {

				try (

					NestedTransaction nestedTansaction =
						parentTransaction.nestTransaction (
							logContext,
							"addFieldsPart.PagePartFactory");

				) {

					return summaryFieldsPartProvider.get ()

						.consoleHelper (
							consoleHelper)

						.formContextBuilder (
							formType)

					;

				}

			};

			pagePartFactories.add (
				partFactory);

			return this;
			*/

		}

	}

	public
	ObjectSummaryPageBuilder <ObjectType, ParentType> addHeading (
			@NonNull String heading) {

		throw todo ();

		/*
		String html =
			stringFormat (
				"<h2>%h</h2>\n",
				heading);

		PagePartFactory pagePartFactory =
			parentTransaction -> {

			try (

				NestedTransaction transaction =
					parentTransaction.nestTransaction (
						logContext,
						"addHeading");

			) {

				return textPart.get ()

					.text (
						html);

			}

		};

		pagePartFactories.add (
			pagePartFactory);

		return this;
		*/

	}

	public
	ObjectSummaryPageBuilder<ObjectType,ParentType> addPart (
			@NonNull String beanName) {

		throw todo ();

		/*
		PagePartFactory partFactory =
			parentTransaction -> {

			try (

				NestedTransaction transaction =
					parentTransaction.nestTransaction (
						logContext,
						"addPart");

			) {

				Object object =
					componentManager.getComponentRequired (
						transaction,
						beanName,
						Object.class);

				if (object instanceof PagePart)
					return (PagePart) object;

				if (object instanceof Provider) {

					Provider<?> provider =
						(Provider<?>) object;

					return (PagePart)
						provider.get ();

				}

				throw new ClassCastException (
					object.getClass ().getName ());

			}

		};

		pagePartFactories.add (
			partFactory);

		return this;
		*/

	}

	void setDefaults (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setDefaults");

		) {

			consoleHelper =
				container.consoleHelper ();

			privKey =
				spec.privKey ();

			/*
			if (spec.builders ().isEmpty ()) {

				addFieldsPart (
					taskLogger,
					formType);

			}

			// if a provider name is provided

			if (spec.fieldsProviderName () != null) {

				fieldsProvider =
					genericCastUnchecked (
						componentManager.getComponentRequired (
							taskLogger,
							spec.fieldsProviderName (),
							FieldsProvider.class));

			} else {

				fieldsProvider =
					null;

			}
			*/

		}

	}

}
