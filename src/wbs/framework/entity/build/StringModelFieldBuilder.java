package wbs.framework.entity.build;

import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.StringUtils.camelToSpaces;
import static wbs.framework.utils.etc.StringUtils.camelToUnderscore;

import com.google.common.collect.ImmutableList;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.entity.meta.StringFieldSpec;
import wbs.framework.entity.model.ModelField;
import wbs.framework.entity.model.ModelFieldType;

@PrototypeComponent ("stringModelFieldBuilder")
@ModelBuilder
public
class StringModelFieldBuilder {

	// builder

	@BuilderParent
	ModelFieldBuilderContext context;

	@BuilderSource
	StringFieldSpec spec;

	@BuilderTarget
	ModelFieldBuilderTarget target;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		String fieldName =
			spec.name ();

		// create model field

		ModelField modelField =
			new ModelField ()

			.model (
				target.model ())

			.parentField (
				context.parentModelField ())

			.name (
				fieldName)

			.label (
				camelToSpaces (
					fieldName))

			.type (
				ModelFieldType.simple)

			.parent (
				false)

			.identity (
				false)

			.valueType (
				String.class)

			.nullable (
				ifNull (
					spec.nullable (),
					false))

			.columnNames (
				ImmutableList.<String>of (
					ifNull (
						spec.columnName (),
						camelToUnderscore (
							fieldName))));

		// store field

		target.fields ().add (
			modelField);

		target.fieldsByName ().put (
			modelField.name (),
			modelField);

	}

}
