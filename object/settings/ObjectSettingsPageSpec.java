package wbs.platform.object.settings;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

import wbs.utils.string.StringFormat;

@Accessors (fluent = true)
@Data
@DataClass ("object-settings-page")
@PrototypeComponent ("objectSettingsPageSpec")
public
class ObjectSettingsPageSpec
	implements ConsoleSpec {

	// tree attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	// attributes

	@DataAttribute (
		format = StringFormat.camelCase)
	String objectName;

	@DataAttribute (
		name = "form",
		required = true,
		format = StringFormat.hyphenated)
	String formTypeName;

	@DataAttribute
	String privKey;

	@DataAttribute (
		format = StringFormat.camelCase)
	String name;

	@DataAttribute (
		format = StringFormat.camelCase)
	String shortName;

	@DataAttribute (
		format = StringFormat.camelCase)
	String longName;

	@DataAttribute
	String friendlyLongName;

	@DataAttribute
	String friendlyShortName;

	@DataAttribute (
		format = StringFormat.camelCase)
	String responderName;

	@DataAttribute (
		format = StringFormat.camelCase)
	String fileName;

	@DataAttribute (
		format = StringFormat.camelCase)
	String tabName;

	@DataAttribute (
		format = StringFormat.camelCase)
	String tabLocation;

	@DataAttribute (
		format = StringFormat.camelCase)
	String listContextTypeName;

}
