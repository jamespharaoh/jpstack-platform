package wbs.platform.background.fixture;

import static wbs.utils.etc.NetworkUtils.runHostname;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.string.CodeUtils.simplifyToCodeRequired;
import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.hyphenToUnderscore;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringReplaceAllSimple;

import lombok.NonNull;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.fixtures.ModelFixtureBuilderComponent;
import wbs.framework.entity.helper.EntityHelper;
import wbs.framework.entity.meta.model.RecordSpec;
import wbs.framework.entity.model.Model;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.logging.LogContext;

import wbs.platform.background.metamodel.BackgroundProcessSpec;
import wbs.platform.background.model.BackgroundProcessObjectHelper;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;

import wbs.utils.time.duration.DurationFormatter;

@PrototypeComponent ("backgroundProcessBuilder")
public
class BackgroundProcessBuilder
	implements ModelFixtureBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	BackgroundProcessObjectHelper backgroundProcessHelper;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EntityHelper entityHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	DurationFormatter durationFormatter;

	// builder

	@BuilderParent
	RecordSpec parent;

	@BuilderSource
	BackgroundProcessSpec spec;

	@BuilderTarget
	Model <?> model;

	// build

	@BuildMethod
	@Override
	public
	void build (
			@NonNull Transaction parentTransaction,
			@NonNull Builder <Transaction> builder) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"build");

		) {

			try {

				transaction.noticeFormat (
					"Create background process %s.%s",
					hyphenToCamel (
						spec.objectTypeCode ()),
					simplifyToCodeRequired (
						spec.name ()));

				createBackgroundProcess (
					transaction);

			} catch (Exception exception) {

				throw new RuntimeException (
					stringFormat (
						"Error creating background process %s.%s",
						spec.objectTypeCode (),
						simplifyToCodeRequired (
							spec.name ())),
					exception);

			}

		}

	}

	private
	void createBackgroundProcess (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createBackgroundProcess");

		) {

			// lookup parent type

			String parentTypeCode =
				hyphenToUnderscore (
					spec.objectTypeCode ());

			ObjectTypeRec parentType =
				objectTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					parentTypeCode);

			// create background process

			backgroundProcessHelper.insert (
				transaction,
				backgroundProcessHelper.createInstance ()

				.setParentType (
					parentType)

				.setCode (
					simplifyToCodeRequired (
						stringReplaceAllSimple (
							"${hostname}",
							runHostname (),
							spec.name ())))

				.setName (
					stringReplaceAllSimple (
						"${hostname}",
						runHostname (),
						spec.name ()))

				.setDescription (
					stringReplaceAllSimple (
						"${hostname}",
						runHostname (),
						spec.description ()))

				.setFrequency (
					optionalOrNull (
						durationFormatter.stringToDuration (
							spec.frequency ())))


			);

		}

	}

}
