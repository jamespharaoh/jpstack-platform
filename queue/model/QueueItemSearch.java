package wbs.platform.queue.model;

import java.io.Serializable;
import java.util.Collection;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.utils.time.interval.TextualInterval;

@Accessors (fluent = true)
@Data
public
class QueueItemSearch
	implements Serializable {

	Long sliceId;

	Long queueId;
	Long queueTypeId;
	Long queueParentTypeId;

	Long claimedUserId;
	Long processedUserId;

	TextualInterval createdTime;
	TextualInterval processedTime;

	QueueItemState state;

	boolean filter;

	Collection <Long> filterQueueIds;
	Collection <Long> filterUserIds;

}
