package wbs.platform.queue.model;

import java.io.Serializable;
import java.util.Collection;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.utils.time.interval.TextualInterval;

@Accessors (fluent = true)
@Data
public
class QueueItemStatsSearch
	implements Serializable {

	Collection <Long> queueIds;
	Collection <Long> userIds;

	TextualInterval timestamp;

	Collection <Long> filterQueueIds;
	Collection <Long> filterUserIds;

}
