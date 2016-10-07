package wbs.imchat.api;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass
public
class ImChatPricePointData {

	@DataAttribute
	String code;

	@DataAttribute
	String name;

	@DataAttribute
	String description;

	@DataAttribute
	String priceString;

	@DataAttribute
	String valueString;

}