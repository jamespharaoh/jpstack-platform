package wbs.platform.object.search;

import static wbs.utils.etc.NullUtils.ifNull;

import java.util.List;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataSetupMethod;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.utils.string.StringFormat;

@Accessors (fluent = true)
@Data
@DataClass ("object-search-page")
@PrototypeComponent ("objectSearchPageSpec")
public
class ObjectSearchPageSpec
	implements ConsoleSpec {

	// singleton components

	@ClassSingletonDependency
	LogContext logContext;

	// tree attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	// attributes

	@DataAttribute (
		format = StringFormat.camelCase)
	String name;

	@DataAttribute (
		name = "object-type",
		format = StringFormat.hyphenated)
	String objectTypeName;

	@DataAttribute
	String sessionKey;

	@DataAttribute (
		name = "search-class",
		required = true,
		format = StringFormat.className)
	String searchClassName;

	@DataAttribute (
		name = "search-form",
		required = true,
		format = StringFormat.hyphenated)
	String searchFormTypeName;

	@DataAttribute (
		name = "search-dao-method",
		format = StringFormat.camelCase)
	String searchDaoMethodName;

	@DataAttribute (
		name = "results-class",
		format = StringFormat.className)
	String resultsClassName;

	@DataAttribute (
		name = "results-form",
		format = StringFormat.hyphenated)
	String resultsFormTypeName;

	@DataAttribute (
		name = "results-dao-method",
		format = StringFormat.camelCase)
	String resultsDaoMethodName;

	@DataAttribute
	String privKey;

	@DataAttribute
	String parentIdKey;

	@DataAttribute
	String parentIdName;

	@DataAttribute
	String tabName;

	@DataAttribute
	String tabLabel;

	@DataAttribute
	String fileName;

	@DataAttribute (
		name = "search-responder",
		format = StringFormat.camelCase)
	String searchResponderName;

	@DataAttribute (
		name = "results-responder",
		format = StringFormat.camelCase)
	String searchResultsResponderName;

	@DataChildren (
		direct = true,
		childElement = "results-mode")
	List <ObjectSearchResultsModeSpec> resultsModes;

	// setup

	@DataSetupMethod
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setup");

		) {

			name =
				ifNull (
					name,
					"search");

		}

	}

}
