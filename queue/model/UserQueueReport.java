package wbs.platform.queue.model;

import lombok.Data;
import lombok.experimental.Accessors;

import org.joda.time.Instant;

import wbs.framework.entity.record.IdObject;

import wbs.platform.user.model.UserRec;

@Accessors (fluent = true)
@Data
public
class UserQueueReport
	implements IdObject {

	UserRec user;

	Long messageCount;

	Instant firstMessage;
	Instant lastMessage;

	Long timeToProcess;

	@Override
	public
	Long getId () {
		return user.getId ();
	}

}
