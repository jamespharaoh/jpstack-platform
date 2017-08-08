package wbs.platform.object.criteria;

import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleSpec;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.TaskLogger;

import wbs.utils.etc.PropertyUtils;

@Accessors (fluent = true)
@DataClass ("where-not-deleted")
@PrototypeComponent ("whereNotDeletedCriteriaSpec")
public
class WhereNotDeletedCriteriaSpec
	implements
		ConsoleSpec,
		CriteriaSpec {

	// implementation

	@Override
	public
	boolean evaluate (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ConsoleRequestContext requestContext,
			@NonNull UserPrivChecker privChecker,
			@NonNull ConsoleHelper <?> objectHelper,
			@NonNull Record <?> object) {

		Boolean deleted =
			(Boolean)
			PropertyUtils.propertyGetAuto (
				object,
				"deleted");

		return ! deleted;

	}

}
