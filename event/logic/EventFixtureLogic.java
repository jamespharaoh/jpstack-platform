package wbs.platform.event.logic;

import java.util.Map;
import java.util.Set;

import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.object.ObjectHelper;

public
interface EventFixtureLogic {

	<Type extends Record <Type>>
	Type createRecordAndEvents (
			Transaction parentTransaction,
			String fixtureProviderName,
			ObjectHelper <Type> objectHelper,
			Record <?> parent,
			Map <String, String> unresolvedParams,
			Set <String> ignoreParamNames);

	void createEvents (
			Transaction parentTransaction,
			String fixtureProviderName,
			Record <?> parent,
			Record <?> object,
			Map <String, Object> resolvedParams);

}
