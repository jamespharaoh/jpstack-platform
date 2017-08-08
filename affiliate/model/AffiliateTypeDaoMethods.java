package wbs.platform.affiliate.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface AffiliateTypeDaoMethods {

	List <AffiliateTypeRec> findAll (
			Transaction parentTransaction);

	AffiliateTypeRec findRequired (
			Transaction parentTransaction,
			Long affiliateTypeId);

}