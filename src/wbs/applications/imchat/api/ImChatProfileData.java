package wbs.applications.imchat.api;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass
public
class ImChatProfileData {

	@DataAttribute
	String code;

	@DataAttribute
	String name;

	@DataAttribute
	String description;

	@DataAttribute
	String imageLink;

}