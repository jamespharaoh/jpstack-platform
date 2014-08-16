package wbs.sms.message.core.model;

import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import lombok.Data;
import lombok.experimental.Accessors;

@Accessors (fluent = true)
@Data
public
class MessageSearch {

	Integer id;

	String number;
	Integer numberId;

	Integer userId;

	Integer serviceId;
	Collection<Integer> serviceIdIn;

	Integer affiliateId;
	Collection<Integer> affiliateIdIn;

	Integer batchId;
	Collection<Integer> batchIdIn;

	Integer routeId;
	Collection<Integer> routeIdIn;

	Integer networkId;

	Date createdTimeAfter;
	Date createdTimeBefore;

	MessageDirection direction;

	MessageStatus status;
	Collection<MessageStatus> statusIn;
	Collection<MessageStatus> statusNotIn;

	String textLike;
	String textILike;

	Integer maxResults;
	MessageSearchOrder orderBy;

	boolean filter;
	Collection<Integer> filterAffiliateIds;
	Collection<Integer> filterRouteIds;
	Collection<Integer> filterServiceIds;

	public MessageSearch () {
	}

	public
	MessageSearch (
			MessageSearch original) {

		this.id =
			original.id;

		this.number =
			original.number;

		this.numberId =
			original.numberId;

		this.serviceId =
			original.serviceId;

		if (original.serviceIdIn != null) {

			this.serviceIdIn =
				new TreeSet<Integer> (
					original.serviceIdIn);

		}

		this.affiliateId =
			original.affiliateId;

		if (original.affiliateIdIn != null) {

			this.affiliateIdIn =
				new TreeSet<Integer> (
					original.affiliateIdIn);

		}

		this.batchId =
			original.batchId;

		if (original.batchIdIn != null) {

			this.batchIdIn =
				new TreeSet<Integer> (
					original.batchIdIn);

		}

		this.routeId =
			original.routeId;

		if (original.routeIdIn != null) {

			this.routeIdIn =
				new TreeSet<Integer> (
					original.routeIdIn);

		}

		this.networkId =
			original.networkId;

		this.createdTimeAfter =
			original.createdTimeAfter;

		this.createdTimeBefore =
			original.createdTimeBefore;

		this.direction =
			original.direction;

		this.status =
			original.status;

		if (original.statusIn != null) {

			this.statusIn =
				new TreeSet<MessageStatus> (
					original.statusIn);

		}

		if (original.statusNotIn != null) {

			this.statusNotIn =
				new TreeSet<MessageStatus> (
					original.statusNotIn);

		}

		this.textLike =
			original.textLike;

		this.textILike =
			original.textILike;

		this.maxResults =
			original.maxResults;

		this.orderBy =
			original.orderBy;

		this.filter =
			original.filter;

		if (original.filterAffiliateIds != null) {

			this.filterAffiliateIds =
				new TreeSet<Integer> (
					original.filterAffiliateIds);

		}

		if (original.filterRouteIds != null) {

			this.filterRouteIds =
				new TreeSet<Integer> (
					original.filterRouteIds);

		}

		if (original.filterServiceIds != null) {

			this.filterServiceIds =
				new TreeSet<Integer> (
					original.filterServiceIds);

		}

	}

	public static
	enum MessageSearchOrder {
		createdTime,
		createdTimeDesc,
	}

}