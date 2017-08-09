package wbs.platform.currency.console;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("currency-field")
@PrototypeComponent ("currencyFormFieldSpec")
public
class CurrencyFormFieldSpec
	implements ConsoleSpec {

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		name = "field")
	String fieldName;

	@DataAttribute
	String label;

	@DataAttribute
	Boolean nullable;

	@DataAttribute
	Boolean readOnly;

	@DataAttribute
	Long minimum =
		Long.MIN_VALUE;

	@DataAttribute
	Long maximum =
		Long.MAX_VALUE;

	@DataAttribute
	Integer size;

	@DataAttribute (
		name = "currency",
		required = true)
	String currencyPath;

	@DataAttribute
	Boolean blankIfZero;

}
