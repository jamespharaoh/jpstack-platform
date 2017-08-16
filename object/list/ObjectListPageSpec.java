package wbs.platform.object.list;

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataChildrenIndex;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("object-list-page")
@PrototypeComponent ("objectListPageSpec")
public
class ObjectListPageSpec
	implements ConsoleSpec {

	// attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	@DataAttribute
	String typeCode;

	@DataAttribute (
		name = "form")
	String formTypeName;

	@DataAttribute (
		name = "target-context-type")
	String targetContextTypeName;

	@DataAttribute
	Boolean moveControls;

	@DataAttribute
	Boolean editControl;

	@DataAttribute
	Boolean deleteControl;

	// children

	@DataChildren (
		direct = true,
		childElement = "list-browser")
	List <ObjectListBrowserSpec> listBrowsers;

	@DataChildrenIndex
	Map <String, ObjectListBrowserSpec> listBrowsersByFieldName;

	@DataChildren (
		direct = true,
		childElement = "list-tab")
	List <ObjectListTabSpec> listTabs;

	@DataChildrenIndex
	Map <String, ObjectListTabSpec> listTabsByName;

}
