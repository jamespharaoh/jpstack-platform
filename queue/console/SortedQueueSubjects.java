package wbs.platform.queue.console;

import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

import wbs.platform.queue.console.QueueSubjectSorter.QueueInfo;
import wbs.platform.queue.console.QueueSubjectSorter.SubjectInfo;

@Accessors (fluent = true)
public
class SortedQueueSubjects {

	@Getter
	List <SubjectInfo> availableSubjects;

	@Getter
	List <QueueInfo> availableQueues;

	@Getter
	List <QueueInfo> allQueues;

	@Getter
	long totalItems = 0;

	@Getter
	long waitingItems = 0;

	@Getter
	long totalAvailableItems = 0;

	@Getter
	long totalAvailableSubjects = 0;

	@Getter
	long totalClaimedItems = 0;

	@Getter
	long userClaimedItems = 0;

	@Getter
	long totalClaimedSubjects = 0;

	@Getter
	long totalUnavailableItems = 0;

	@Getter
	long totalUnavailableSubjects = 0;

}
