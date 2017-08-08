package wbs.platform.queue.model;

import lombok.Data;

import wbs.platform.user.model.UserRec;

@Data
public
class QueueItemUserStats {

	QueueRec queue;
	UserRec user;

	Long numProcessed;

}
