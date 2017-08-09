package wbs.platform.object.link;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("object-links-page")
@PrototypeComponent ("objectLinksPageSpec")
public
class ObjectLinksPageSpec
	implements ConsoleSpec {

	// attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		name = "links-field",
		required = true)
	String linksFieldName;

	@DataAttribute (
		name = "target-links-field",
		required = true)
	String targetLinksFieldName;

	@DataAttribute (
		required = true)
	String addEventName;

	@DataAttribute (
		required = true)
	String removeEventName;

	@DataAttribute (
		required = true)
	ObjectLinksAction.EventOrder eventOrder;

	@DataAttribute (
		name = "signal")
	String updateSignalName;

	@DataAttribute (
		name = "target-signal")
	String targetUpdateSignalName;

	@DataAttribute (required = true)
	String successNotice;

	@DataAttribute (
		name = "form")
	String formTypeName;

	@DataAttribute (
		required = true)
	OwnedBy ownedBy;

	public static
	enum OwnedBy {
		us,
		them;
	}

}
