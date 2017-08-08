package wbs.platform.rpc.core;

public
enum RpcType {

	// primitives

	rBoolean,
	rInteger,
	rString,
	rBinary,
	rFloat,

	// complex types

	rDate,

	// structured types

	rStructure,
	rList,
	rSource;

}
