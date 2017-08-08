package wbs.platform.queue.model;

public
enum QueueItemState {

	waiting,
	pending,
	claimed,
	cancelled,
	processed;

}
