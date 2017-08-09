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

	@DataAttribute
	String name;

	@DataAttribute (
		name = "object-type")
	String objectTypeName;

	@DataAttribute
	String sessionKey;

	@DataAttribute (
		name = "search-class",
		required = true)
	String searchClassName;

	@DataAttribute (
		name = "search-form",
		required = true)
	String searchFormTypeName;

	@DataAttribute (
		name = "search-dao-method")
	String searchDaoMethodName;

	@DataAttribute (
		name = "results-class")
	String resultsClassName;

	@DataAttribute (
		name = "results-form")
	String resultsFormTypeName;

	@DataAttribute (
		name = "results-dao-method")
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
		name = "search-responder")
	String searchResponderName;

	@DataAttribute (
		name = "results-responder")
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
