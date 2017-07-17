package wbs.platform.text.logic;

import static wbs.utils.etc.OptionalUtils.optionalFromNullable;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.LateLifecycleSetup;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.CloseableTransaction;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextObjectHelperMethods;
import wbs.platform.text.model.TextRec;

import wbs.utils.cache.AdvancedCache;
import wbs.utils.cache.IdCacheBuilder;

public
class TextObjectHelperMethodsImplementation
	implements TextObjectHelperMethods {

	int maxExistingTexts = 65536;
	boolean optimistic = false;

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@WeakSingletonDependency
	TextObjectHelper textHelper;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <IdCacheBuilder <
		CloseableTransaction,
		String,
		Long,
		TextRec
	>> textCacheBuilderProvider;

	// state

	AdvancedCache <CloseableTransaction, String, TextRec> textCache;

	// life cycle

	@LateLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			// from and to user id

			textCache =
				textCacheBuilderProvider.provide (
					taskLogger)

				.lookupByIdFunction (
					textHelper::find)

				.lookupByKeyFunction (
					(innerTransaction, textValue) ->
						optionalFromNullable (
							textHelper.findByTextNoFlush (
								innerTransaction,
								textValue)))

				.getIdFunction (
					TextRec::getId)

				.createFunction (
					this::createReal)

				.wrapperFunction (
					CloseableTransaction::genericWrapper)

				.build (
					taskLogger)

			;

		}

	}

	// implementation

	@Override
	public
	TextRec findOrCreate (
			@NonNull Transaction parentTransaction,
			@NonNull String stringValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findOrCreate");

		) {

			return textCache.findOrCreate (
				transaction,
				stringValue);

		}

	}

	private
	TextRec createReal (
			@NonNull Transaction parentTransaction,
			@NonNull String stringValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createReal");

		) {

			return textHelper.insert (
				transaction,
				textHelper.createInstance ()

				.setText (
					stringValue)

			);

		}

	}

}