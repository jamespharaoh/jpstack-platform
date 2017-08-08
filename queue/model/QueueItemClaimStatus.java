package wbs.platform.queue.model;

public
enum QueueItemClaimStatus {

	claimed,
	unclaimed,
	forcedUnclaim,
	processed,
	cancelled;

}
