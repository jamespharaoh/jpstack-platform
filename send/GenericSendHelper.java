package wbs.platform.send;

import java.util.List;

import org.joda.time.Instant;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.object.ObjectHelper;

public
interface GenericSendHelper <
	Service extends Record <Service>,
	Job extends Record <Job>,
	Item extends Record <Item>
> {

	String parentTypeName ();

	String name ();
	String itemNamePlural ();

	ObjectHelper <Job> jobHelper ();
	ObjectHelper <Item> itemHelper ();

	List <Job> findSendingJobs (
			Transaction parentTransaction);

	List <Job> findScheduledJobs (
			Transaction parentTransaction,
			Instant now);

	List <Item> findItemsLimit (
			Transaction parentTransaction,
			Service service,
			Job job,
			Long maxResults);

	Service getService (
			Transaction parentTransaction,
			Job job);

	Instant getScheduledTime (
			Transaction parentTransaction,
			Service service,
			Job job);

	boolean jobScheduled (
			Transaction parentTransaction,
			Service service,
			Job job);

	boolean jobSending (
			Transaction parentTransaction,
			Service service,
			Job job);

	boolean jobConfigured (
			Transaction parentTransaction,
			Service service,
			Job job);

	void sendStart (
			Transaction parentTransaction,
			Service service,
			Job job);

	boolean verifyItem (
			Transaction parentTransaction,
			Service service,
			Job job,
			Item item);

	void rejectItem (
			Transaction parentTransaction,
			Service service,
			Job job,
			Item item);

	void sendItem (
			Transaction parentTransaction,
			Service service,
			Job job,
			Item item);

	void sendComplete (
			Transaction parentTransaction,
			Service service,
			Job job);

}
