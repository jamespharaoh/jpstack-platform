package wbs.platform.object.core.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface ObjectTypeDaoMethods {

	ObjectTypeRec findById (
			Transaction transaction,
			Long id);

	ObjectTypeRec findByCode (
			Transaction transaction,
			String code);

	List <ObjectTypeRec> findAll (
			Transaction transaction);

}