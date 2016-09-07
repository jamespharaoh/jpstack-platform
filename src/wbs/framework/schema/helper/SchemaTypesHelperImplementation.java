package wbs.framework.schema.helper;

import static wbs.framework.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.framework.utils.etc.StringUtils.camelToUnderscore;
import static wbs.framework.utils.etc.StringUtils.capitalise;
import static wbs.framework.utils.etc.StringUtils.joinWithCommaAndSpace;
import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.framework.utils.etc.TypeUtils.classForName;

import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Cleanup;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j;
import wbs.framework.activitymanager.ActiveTask;
import wbs.framework.activitymanager.ActivityManager;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.scaffold.PluginCustomTypeSpec;
import wbs.framework.component.scaffold.PluginEnumTypeSpec;
import wbs.framework.component.scaffold.PluginManager;
import wbs.framework.component.scaffold.PluginSpec;
import wbs.framework.hibernate.EnumUserType;
import wbs.framework.logging.TaskLogger;

@Accessors (fluent = true)
@Log4j
@SingletonComponent ("schemaTypesHelper")
public
class SchemaTypesHelperImplementation
	implements SchemaTypesHelper {

	// dependencies

	@Inject
	ActivityManager activityManager;

	@Inject
	SchemaNamesHelper schemaNamesHelper;

	@Inject
	PluginManager pluginManager;

	// properties

	@Getter
	Map <Class <?>, List <String>> fieldTypeNames;

	@Getter
	Map <String, List <String>> enumTypes;

	// implementation

	@PostConstruct
	public
	void init () {

		initTypeNames ();

	}

	void initTypeNames () {

		TaskLogger taskLog =
			new TaskLogger (
				log);

		ImmutableMap.Builder <Class <?>, List <String>> fieldTypeNamesBuilder =
			ImmutableMap.builder ();

		ImmutableMap.Builder <String, List <String>> enumTypesBuilder =
			ImmutableMap.builder ();

		for (
			Map.Entry <Class <?>, String> entry
				: builtinFieldTypeNames.entrySet ()
		) {

			fieldTypeNamesBuilder.put (
				entry.getKey (),
				ImmutableList.of (
					entry.getValue ()));

		}

		for (
			PluginSpec plugin
				: pluginManager.plugins ()
		) {

			if (plugin.models () == null)
				continue;

			for (
				PluginEnumTypeSpec enumType
					: plugin.models ().enumTypes ()
			) {

				initEnumType (
					taskLog,
					fieldTypeNamesBuilder,
					enumTypesBuilder,
					enumType);

			}

			for (
				PluginCustomTypeSpec customType
					: plugin.models ().customTypes ()
			) {

				initCustomType (
					taskLog,
					fieldTypeNamesBuilder,
					enumTypesBuilder,
					customType);

			}

		}

		if (taskLog.errors ()) {

			throw new RuntimeException (
				stringFormat (
					"Aborting due to %s errors",
					taskLog.errorCount ()));

		}

		fieldTypeNames =
			fieldTypeNamesBuilder.build ();

		enumTypes =
			enumTypesBuilder.build ();

	}

	@SuppressWarnings ({ "unchecked", "rawtypes" })
	void initEnumType (
			TaskLogger taskLog,
			ImmutableMap.Builder<Class<?>,List<String>> fieldTypeNamesBuilder,
			ImmutableMap.Builder<String,List<String>> enumTypesBuilder,
			PluginEnumTypeSpec enumType) {

		@Cleanup
		ActiveTask activeTask =
			activityManager.start (
				"schema",
				"schemaTypesHelper.initEnumType (...)",
				this);

		String enumClassName =
			stringFormat (
				"%s.model.%s",
				enumType.plugin ().packageName (),
				capitalise (enumType.name ()));

		Class enumClass;

		try {

			enumClass =
				Class.forName (
					enumClassName);

		} catch (ClassNotFoundException exception) {

			taskLog.errorFormat (
				"No such class %s",
				enumClassName);

			return;

		}

		EnumUserType enumUserType =
			new EnumUserType ()

			.sqlType (
				1111)

			.enumClass (
				enumClass)

			.auto (
				String.class);

		String typeName =
			camelToUnderscore (
				enumClass.getSimpleName ());

		fieldTypeNamesBuilder.put (
			enumClass,
			ImmutableList.<String>of (
				typeName));

		if (enumUserType.sqlType () == 1111) {

			ImmutableList.Builder<String> enumValuesBuilder =
				ImmutableList.<String>builder ();

			for (
				Object enumValue
					: enumUserType.databaseValues ()
			) {

				enumValuesBuilder.add (
					(String) enumValue);

			}

			enumTypesBuilder.put (
				typeName,
				enumValuesBuilder.build ());

		}

	}

	void initCustomType (
			TaskLogger taskLog,
			ImmutableMap.Builder<Class<?>,List<String>> fieldTypeNamesBuilder,
			ImmutableMap.Builder<String,List<String>> enumTypesBuilder,
			PluginCustomTypeSpec customType) {

		@Cleanup
		ActiveTask activeTask =
			activityManager.start (
				"schema",
				stringFormat (
					"schemaTypesHelper.initCustomType (%s)",
					joinWithCommaAndSpace (
						", ",
						"taskLog",
						"fieldTypeNamesBuilder",
						"enumTypesBuilder",
						stringFormat (
							"customType:%s",
							customType.name ()))),
				this);

		String objectClassName =
			stringFormat (
				"%s.model.%s",
				customType.plugin ().packageName (),
				capitalise (
					customType.name ()));

		Optional <Class <?>> objectClassOptional =
			classForName (
				objectClassName);

		if (
			optionalIsNotPresent (
				objectClassOptional)
		) {

			taskLog.errorFormat (
				"No such class %s",
				objectClassName);

		}

		String helperClassName =
			stringFormat (
				"%s.hibernate.%sType",
				customType.plugin ().packageName (),
				capitalise (
					customType.name ()));

		Optional<Class<?>> helperClassOptional =
			classForName (
				helperClassName);

		if (
			optionalIsNotPresent (
				helperClassOptional)
		) {

			taskLog.errorFormat (
				"No such class %s",
				helperClassName);

		}

		if (

			optionalIsNotPresent (
				objectClassOptional)

			|| optionalIsNotPresent (
				helperClassOptional)

		) {
			return;
		}

		Class<?> objectClass =
			objectClassOptional.get ();

		Class<?> helperClass =
			helperClassOptional.get ();

		Object helper;

		try {

			helper =
				helperClass.newInstance ();

		} catch (Exception exception) {

			taskLog.errorFormatException (
				exception,
				"Error instantiating %s",
				helperClass.getName ());

			return;

		}

		if (helper instanceof EnumUserType) {

			EnumUserType<?,?> enumHelper =
				(EnumUserType<?,?>)
				helper;

			String typeName =
				enumHelper.sqlType () == 1111

				? camelToUnderscore (
					enumHelper.enumClass ().getSimpleName ())

				: builtinSqlTypeNames.get (
					enumHelper.sqlType ());

			if (typeName == null) {

				taskLog.errorFormat (
					"Don't know how to handle sql type %s for %s",
					enumHelper.sqlType (),
					helper.getClass ().getName ());

				return;

			}

			fieldTypeNamesBuilder.put (
				objectClass,
				ImmutableList.<String>of (typeName));

			if (enumHelper.sqlType () == 1111) {

				ImmutableList.Builder<String> enumValuesBuilder =
					ImmutableList.<String>builder ();

				for (
					Object enumValue
						: enumHelper.databaseValues ()
				) {

					enumValuesBuilder.add (
						(String) enumValue);

				}

				enumTypesBuilder.put (
					typeName,
					enumValuesBuilder.build ());

			}

			return;

		}

		if (helper instanceof CompositeUserType) {

			CompositeUserType compositeUserType =
				(CompositeUserType)
				helper;

			ImmutableList.Builder<String> typeNamesBuilder =
				ImmutableList.<String>builder ();

			for (
				Type propertyType
					: compositeUserType.getPropertyTypes ()
			) {

				String typeName =
					builtinFieldTypeNames.get (
						propertyType.getReturnedClass ());

				if (typeName == null) {

					taskLog.errorFormat (
						"Don't know how to handle sql type %s for %s",
						propertyType.getReturnedClass (),
						helper.getClass ().getName ());

					return;

				}

				typeNamesBuilder.add (
					typeName);

			}

			fieldTypeNamesBuilder.put (
				compositeUserType.returnedClass (),
				typeNamesBuilder.build ());

			return;

		}

		taskLog.errorFormat (
			"Don't know how to handle %s",
			helper.getClass ());

		return;

	}

	Map <Class <?>, String> builtinFieldTypeNames =
		ImmutableMap.<Class <?>, String> builder ()

		.put (
			Boolean.class,
			"boolean")

		.put (
			Double.class,
			"double precision")

		.put (
			Integer.class,
			"integer")

		.put (
			Long.class,
			"bigint")

		.put (
			String.class,
			"text")

		.put (
			byte[].class,
			"bytea")

		.put (
			LocalDate.class,
			"date")

		.put (
			Instant.class,
			"text")

		.put (
			Date.class,
			"timestamp with time zone")

		.put (
			Character.class,
			"char (1)")

		.build ();

	Map <Integer, String> builtinSqlTypeNames =
		ImmutableMap.<Integer, String> builder ()

		.put (
			Types.VARCHAR,
			"text")

		.put (
			Types.CHAR,
			"char (1)")

		.put (
			Types.INTEGER,
			"int")

		.put (
			Types.BIGINT,
			"bigint")

		.build ();

}
