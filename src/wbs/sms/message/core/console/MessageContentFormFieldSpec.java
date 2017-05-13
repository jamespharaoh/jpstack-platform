package wbs.sms.message.core.console;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleData;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("message-content-field")
@PrototypeComponent ("messageContentFormFieldSpec")
public
class MessageContentFormFieldSpec
	implements ConsoleModuleData {

}
