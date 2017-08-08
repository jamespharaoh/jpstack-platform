package wbs.platform.object.criteria;

import static wbs.utils.etc.PropertyUtils.propertyGetAuto;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleSpec;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.TaskLogger;

@Accessors (fluent = true)
@DataClass ("where-null")
@PrototypeComponent ("whereNullCriteriaSpec")
public
class WhereNullCriteriaSpec
	implements
		ConsoleSpec,
		CriteriaSpec {

	// attributes

	@DataAttribute (
		name = "field",
		required = true)
	@Getter @Setter
	String fieldName;

	// implementation

	@Override
	public
	boolean evaluate (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull UserPrivChecker privChecker,
			@NonNull ConsoleHelper <?> objectHelper,
			@NonNull Record <?> object) {

		Object fieldValue =
			propertyGetAuto (
				object,
				fieldName);

		return fieldValue == null;

	}

}
