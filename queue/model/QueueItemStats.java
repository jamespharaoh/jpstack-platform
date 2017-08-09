package wbs.platform.queue.model;

import lombok.Data;

@Data
public
class QueueItemStats {

	QueueRec queue;

	Long numCreated;
	Long numProcessed;
	Long numPreferred;
	Long numNotPreferred;

}
