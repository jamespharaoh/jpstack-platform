package wbs.platform.queue.fixture;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.string.CodeUtils.simplifyToCodeRequired;
import static wbs.utils.string.StringUtils.hyphenToUnderscore;
import static wbs.utils.string.StringUtils.stringFormat;

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

import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.queue.metamodel.QueueTypeSpec;
import wbs.platform.queue.model.QueueTypeObjectHelper;

@PrototypeComponent ("queueTypeBuilder")
public
class QueueTypeBuilder
	implements ModelFixtureBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EntityHelper entityHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	QueueTypeObjectHelper queueTypeHelper;

	// builder

	@BuilderParent
	RecordSpec parent;

	@BuilderSource
	QueueTypeSpec spec;

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

			transaction.noticeFormat (
				"Create queue type %s.%s",
				hyphenToUnderscore (
					ifNull (
						spec.subject (),
						parent.name ())),
				simplifyToCodeRequired (
					spec.name ()));

			createQueueType (
				transaction);

		} catch (Exception exception) {

			throw new RuntimeException (
				stringFormat (
					"Error creating queue type %s.%s",
					hyphenToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					simplifyToCodeRequired (
						spec.name ())),
				exception);

		}

	}

	private
	void createQueueType (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createQueueType");

		) {

			// lookup parent type

			String parentTypeCode =
				hyphenToUnderscore (
					ifNull (
						spec.parent (),
						parent.name ()));

			ObjectTypeRec parentType =
				objectTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					parentTypeCode);

			// lookup subject type

			String subjectTypeCode =
				hyphenToUnderscore (
					spec.subject ());

			ObjectTypeRec subjectType =
				objectTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					subjectTypeCode);

			// lookup ref type

			String refTypeCode =
				hyphenToUnderscore (
					spec.ref ());

			ObjectTypeRec refType =
				objectTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					refTypeCode);

			// create queue type

			queueTypeHelper.insert (
				transaction,
				queueTypeHelper.createInstance ()

				.setParentType (
					parentType)

				.setCode (
					simplifyToCodeRequired (
						spec.name ()))

				.setDescription (
					spec.description ())

				.setSubjectType (
					subjectType)

				.setRefType (
					refType)

				.setDefaultPriority (
					spec.defaultPriority ())

			);

		}

	}

}
