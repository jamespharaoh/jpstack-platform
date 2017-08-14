package wbs.platform.object.verify.console;

import static wbs.utils.collection.CollectionUtils.listFirstElementRequired;
import static wbs.utils.collection.CollectionUtils.listSecondElementRequired;
import static wbs.utils.collection.IterableUtils.iterableOnlyItemByClass;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.string.StringUtils.hyphenToUnderscore;
import static wbs.utils.string.StringUtils.joinWithFullStop;
import static wbs.utils.string.StringUtils.stringSplitColon;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.context.ConsoleContext;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleManager;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.component.scaffold.PluginManager;
import wbs.framework.entity.meta.model.ModelMetaLoader;
import wbs.framework.entity.meta.model.RecordSpec;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.object.verify.metamodel.ObjectVerificationSpec;
import wbs.platform.queue.console.QueueConsolePlugin;
import wbs.platform.queue.model.QueueItemRec;

import wbs.web.responder.WebResponder;

@SingletonComponent ("objectVerifyQueueConsolePlugin")
public
class ObjectVerifyQueueConsolePlugin
	implements QueueConsolePlugin {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ModelMetaLoader modelMetaLoader;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	PluginManager pluginManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("objectVerificationPendingFormResponder")
	ComponentProvider <WebResponder> pendingFormResponderProvider;

	// public implementation

	@Override
	public
	List <String> queueTypeCodes (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeResponder");

		) {

			ImmutableSet.Builder <String> builder =
				ImmutableSet.builder ();

			for (
				RecordSpec recordSpec
					: modelMetaLoader.recordSpecs ().values ()
			) {

				Optional <ObjectVerificationSpec> verificationSpecOptional =
					iterableOnlyItemByClass (
						recordSpec.children (),
						ObjectVerificationSpec.class);

				if (
					optionalIsNotPresent (
						verificationSpecOptional)
				) {
					continue;
				}

				ObjectVerificationSpec verificationSpec =
					optionalGetRequired (
						verificationSpecOptional);

				List <String> queueNameParts =
					stringSplitColon (
						verificationSpec.queueName ());

				Class <?> queueParentClass =
					optionalGetRequired (
						objectManager.dereferenceType (
							taskLogger,
							optionalOf (
								pluginManager.recordModelClass (
									recordSpec.name ())),
							optionalOf (
								listFirstElementRequired (
									queueNameParts))));

				ConsoleHelper <?> queueParentHelper =
					objectManager.consoleHelperForClassRequired (
						queueParentClass);

				builder.add (
					joinWithFullStop (
						hyphenToUnderscore (
							queueParentHelper.objectTypeHyphen ()),
						listSecondElementRequired (
							queueNameParts)));

			}

			return ImmutableList.copyOf (
				builder.build ());

		}

	}

	@Override
	public
	WebResponder makeResponder (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull QueueItemRec queueItem) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeResponder");

		) {

			ConsoleContext targetContext =
				consoleManager.context (
					"objectVerification",
					true);

			consoleManager.changeContext (
				taskLogger,
				privChecker,
				targetContext,
				"/" + queueItem.getRefObjectId ());

			return pendingFormResponderProvider.provide (
				taskLogger);

		}

	}

}
