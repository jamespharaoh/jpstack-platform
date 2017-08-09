package wbs.platform.object.search;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleFormType;

@Accessors (fluent = true)
@Data
public
class ObjectSearchResultsMode <Container> {

	String name;

	ConsoleFormType <Container> formType;

}
