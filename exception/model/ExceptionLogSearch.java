package wbs.platform.exception.model;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import wbs.utils.time.interval.TextualInterval;

@Accessors (fluent = true)
@Data
@EqualsAndHashCode
@ToString
public
class ExceptionLogSearch
	implements Serializable {

	TextualInterval timestamp;

	Long typeId;

	Long userSliceId;
	Long userId;

	String sourceContains;
	String summaryContains;
	String dumpContains;

	Boolean alert;
	Boolean fatal;

	ConcreteExceptionResolution resolution;

	Order order =
		Order.timestampDesc;

	Long maxResults;

	public static
	enum Order {
		timestampDesc
	}

}
