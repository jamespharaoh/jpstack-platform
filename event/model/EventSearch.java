package wbs.platform.event.model;

import java.io.Serializable;

import lombok.Data;
import lombok.experimental.Accessors;

import org.joda.time.Instant;

@Accessors (fluent = true)
@Data
public
class EventSearch
	implements Serializable {

	Instant timestampAfter;
	Instant timestampBefore;

	Boolean admin;

}
