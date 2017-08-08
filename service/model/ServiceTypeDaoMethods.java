package wbs.platform.service.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface ServiceTypeDaoMethods {

	ServiceTypeRec findRequired (
			Transaction parentTransaction,
			Long id);

	List <ServiceTypeRec> findAll (
			Transaction parentTransaction);

}