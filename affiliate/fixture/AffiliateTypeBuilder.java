package wbs.platform.affiliate.fixture;

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

import wbs.platform.affiliate.metamodel.AffiliateTypeSpec;
import wbs.platform.affiliate.model.AffiliateTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;

@PrototypeComponent ("affiliateTypeBuilder")
public
class AffiliateTypeBuilder
	implements ModelFixtureBuilderComponent {

	// singleton depenencies

	@SingletonDependency
	AffiliateTypeObjectHelper affiliateTypeHelper;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EntityHelper entityHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	// builder

	@BuilderParent
	RecordSpec parent;

	@BuilderSource
	AffiliateTypeSpec spec;

	@BuilderTarget
	Model <?> model;

	// build

	@Override
	@BuildMethod
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
				"Create affiliate type %s.%s",
				hyphenToUnderscore (
					ifNull (
						spec.subject (),
						parent.name ())),
				simplifyToCodeRequired (
					spec.name ()));

			createAffiliateType (
				transaction);

		} catch (Exception exception) {

			throw new RuntimeException (
				stringFormat (
					"Error creating affiliate type %s.%s",
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
	void createAffiliateType (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createAffiliateType");

		) {

			// lookup parent type

			String parentTypeCode =
				hyphenToUnderscore (
					ifNull (
						spec.subject (),
						parent.name ()));

			ObjectTypeRec parentType =
				objectTypeHelper.findByCodeRequired (
					transaction,
					GlobalId.root,
					parentTypeCode);

			// create affiliate type

			affiliateTypeHelper.insert (
				transaction,
				affiliateTypeHelper.createInstance ()

				.setParentType (
					parentType)

				.setCode (
					simplifyToCodeRequired (
						spec.name ()))

				.setName (
					spec.name ())

				.setDescription (
					spec.description ())

			);

		}

	}

}
