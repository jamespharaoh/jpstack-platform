package wbs.platform.object.criteria;

import static wbs.utils.string.StringUtils.stringInSafe;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleSpec;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.utils.etc.PropertyUtils;

@Accessors (fluent = true)
@DataClass ("where-in")
@PrototypeComponent ("whereInCriteriaSpec")
public
class WhereInCriteriaSpec <RecordType extends Record <RecordType>>
	implements
		ConsoleSpec,
		CriteriaSpec <RecordType> {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// attributes

	@DataAttribute (
		name = "field",
		required = true)
	@Getter @Setter
	String fieldName;

	@DataChildren (
		direct = true,
		childElement = "item",
		valueAttribute = "value")
	@Getter @Setter
	List <String> values;

	// implementation

	@Override
	public
	boolean evaluate (
			@NonNull Transaction parentTransaction,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull UserPrivChecker privChecker,
			@NonNull ConsoleHelper <RecordType> objectHelper,
			@NonNull RecordType object) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"evaluate");

		) {

			Object fieldValue =
				PropertyUtils.propertyGetAuto (
					object,
					fieldName);

			if (fieldValue == null) {
				return false;
			}

			return stringInSafe (
				fieldValue.toString (),
				values);

		}

	}

}
