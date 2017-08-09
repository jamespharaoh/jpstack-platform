package wbs.platform.rpc.core;

import static wbs.utils.string.StringUtils.hyphenToCamel;
import static wbs.utils.string.StringUtils.stringFormat;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public
interface RpcChecker {

	Object check (
		RpcDefinition def,
		Object value,
		List<String> errors);

	public final static
	Pattern numericPattern =
		Pattern.compile ("\\d+");

	public final static
	RpcChecker stringNumeric =
		new RpcChecker () {

		@Override
		public
		Object check (
				RpcDefinition def,
				Object value,
				List<String> errors) {

			String string = (String) value;

			if (numericPattern.matcher (string).matches ())
				return string;

			errors.add (
				stringFormat (
					"Parameter should contain digits only: %s",
					def.name ()));

			return null;

		}

	};

	public final static
	RpcChecker integerZeroOrMore =
		new RpcChecker () {

		@Override
		public
		Object check (
				RpcDefinition def,
				Object value,
				List<String> errors) {

			Long longValue =
				(Long) value;

			if (longValue >= 0l)
				return longValue;

			errors.add (
				stringFormat (
					"Parameter should be 0 or more: %s",
					def.name ()));

			return null;

		}

	};

	public final static
	RpcChecker integerOneOrMore =
		new RpcChecker () {

		@Override
		public
		Object check (
				RpcDefinition rpcDefinition,
				Object value,
				List<String> errors) {

			Long longValue =
				(Long)
				value;

			if (longValue >= 1)
				return longValue;

			errors.add (
				stringFormat (
					"Parameter should be 1 or more: %s",
					rpcDefinition.name ()));

			return null;

		}

	};

	public static
	class EnumRpcChecker
		implements RpcChecker {

		private final
		Map<String, ?> map;

		public
		EnumRpcChecker (
				Map<String, ?> newMap) {

			map = newMap;

		}

		@Override
		public
		Object check (
				RpcDefinition def,
				Object value,
				List<String> errors) {

			String string = (String) value;

			if (!map.containsKey(string)) {

				errors.add (
					stringFormat (
						"Parameter value is not valid: %s, value: %s",
						def.name (),
						string));

				return null;

			}

			return map.get (
				value);

		}

	}

	public static
	class SetRpcChecker
		implements RpcChecker {

		@Override
		public
		Object check (
				RpcDefinition rpcDefinition,
				Object value,
				List<String> errors) {

			Set<Object> ret =
				new LinkedHashSet<Object>();

			if (rpcDefinition.type () != RpcType.rList) {

				throw new RuntimeException (
					"Set Rpc Checker must only be used with a list");

			}

			List<?> list =
				(List<?>) value;

			for (Object item : list) {

				if (ret.contains (item)) {

					errors.add (
						stringFormat (
							"Duplicated value in set: %s",
							rpcDefinition.name ()));

					return null;

				}

				ret.add (
					item);

			}

			return ret;

		}

	}

	public static
	class MapRpcChecker
		implements RpcChecker {

		String keyName;
		String valueName;

		public
		MapRpcChecker (
				String keyName,
				String valueName) {

			this.keyName = keyName;
			this.valueName = valueName;

		}

		@Override
		public
		Object check (
				RpcDefinition rpcDefinition,
				Object value,
				List<String> errors) {

			Map<String,String> ret =
				new LinkedHashMap<String,String> ();

			if (rpcDefinition.type () != RpcType.rList) {

				throw new RuntimeException (
					"Map RPC checker must only be used with a list");

			}

			for (Object item
					: (List<?>) value) {

				Map<?,?> itemMap =
					(Map<?,?>) item;

				String itemKey =
					(String) itemMap.get (keyName);

				String itemValue =
					(String) itemMap.get (valueName);

				if (ret.containsKey (itemKey)) {

					errors.add (
						stringFormat (
							"Duplicated key in map: %s",
							rpcDefinition.name ()));

					return null;

				}

				ret.put (itemKey, itemValue);

			}

			return ret;

		}

	}

	public static
	class PublicMemberRpcChecker
		implements RpcChecker {

		Class<?> type;

		public
		PublicMemberRpcChecker (
				Class<?> type) {

			this.type = type;

		}

		@Override
		public
		Object check (
				RpcDefinition rpcDefinition,
				Object value,
				List<String> errors) {

			try {

				if (rpcDefinition.type () != RpcType.rStructure) {

					throw new RuntimeException (
						stringFormat (
							"Public member RPC checker must only be used ",
							"with a structure"));

				}

				Object ret =
					type.newInstance ();

				for (Map.Entry<?,?> ent
						: ((Map<?,?>) value).entrySet ()) {

					Field field =
						type.getField (
							hyphenToCamel (
								(String)
								ent.getKey ()));

					field.set (
						ret,
						ent.getValue ());

				}

				return ret;

			} catch (Exception exception) {

				throw new RuntimeException (
					exception);

			}

		}

	}

}
