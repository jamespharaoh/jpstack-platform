$(function () {

	var allQueueItems;
	var optionSets;

	// store references

	var optionSetTemplate = $(".optionSet");
	var optionTemplate = $(".option");
	var optionSetsElem = $(".optionSets");

	// go function, called below

	var go = function () {

		getQueueItems ();

		fetchOptionSets (function () {
			createOptionSets ();
			showAndHideQueueItems ();
			createShowHideLink ();
		});

	};

	var createShowHideLink = function () {

		var shown = true;

		$(".showHideLink").text ("hide");

		var toggle = function () {
			if (shown) {

				$(".optionSets").hide ();
				$(".showHideLink").text ("show");
				shown = false;
				localStorage.setItem ("showHide", "hide");

			} else {

				$(".optionSets").show ();
				$(".showHideLink").text ("hide");
				shown = true;
				localStorage.setItem ("showHide", "show");

			}
		};

		$(".showHideLink").click (toggle);

		if (localStorage.getItem ("showHide") == "hide")
			toggle ();

	};

	// read queue item data from html

	var getQueueItems = function () {

		allQueueItems = [];

		$(".queueItemTable .queueItemRow").each (function () {

			var row = $(this);

			var queueItem = {
				parentObjectTypeCode: row.data ("parent-object-type-code"),
				parentObjectCode: row.data ("parent-object-code"),
				queueTypeCode: row.data ("queue-type-code"),
				queueCode: row.data ("queue-code"),
				sliceCode: row.data ("slice-code"),
				row: row,
				oldestTimestamp: row.data ("oldest-timestamp"),
				oldestString: row.find (".queueItemOldest").text (),
			};

			allQueueItems.push (queueItem);

		});

	};

	// load option set data via ajax

	var fetchOptionSets = function (callback) {

		req = $.ajax ("/queues/queue.filter")

		req.success (function (optionSetsYaml) {

			optionSets = jsyaml.load (optionSetsYaml);

			callback ();

		});

	};

	// create html elements for options

	var createOptionSets = function () {

		optionSets.forEach (
			function (optionSet) {

			// create the new option set element

			optionSet.elem =
				optionSetTemplate.clone ();

			optionSet.nameElem =
				optionSet.elem.find (
					".optionSetName")

			optionSet.nameElem.text (
				optionSet.name);

			// iterate option

			optionSet.options.forEach (
				function (option) {

				option.key =
					"optionSet/" + optionSet.name + "/" + option.name;

				option.enabled =
					localStorage.getItem (option.key) != "false";

				// create the option element

				option.elem =
					optionTemplate.clone ();

				option.checkboxElem =
					option.elem.find (".optionCheckbox");

				option.labelElem =
					option.elem.find (".optionLabel");

				option.checkboxId =
					wbs.uniqueId ();

				// setup checkbox

				option.checkboxElem.attr (
					"id",
					option.checkboxId);

				option.checkboxElem.prop (
					"checked",
					option.enabled);

				(function (option) { // capture value of option

					option.checkboxElem.change (function () {

						// update local storage and stuff

						option.enabled =
							option.checkboxElem.prop (
								"checked");

						localStorage.setItem (
							option.key,
							option.enabled
								? "true"
								: "false");

						// update display of queue items

						showAndHideQueueItems ();

					});

				}) (option);

				// setup label

				option.labelElem.text (
					option.name);

				option.labelElem.attr (
					"for",
					option.checkboxId);

				// add the option element

				optionSet.elem.append (
					option.elem);

			});

			// add the option set element

			optionSetsElem.append (
				optionSet.elem);

		});

	};

	// display and hide queue items based on selection

	var queueItemMatches =
		function (queueItem, option) {

		if (option.or) {

			var match = false;

			option.or.forEach (
				function (subOption) {

				var subMatch =
					queueItemMatches (
						queueItem,
						subOption);

				if (subMatch)
					match = true;

			});

			return match;
		}

		var match = true;

		[
			"parentObjectTypeCode",
			"parentObjectCode",
			"queueTypeCode",
			"queueCode",
			"sliceCode"
		].forEach (function (field) {

			if (! option [field])
				return;

			if (option [field] == queueItem [field])
				return;

			match = false;
		});

		return match;
	}

	// filter queue items by a single option set

	var filterQueueItems =
		function (optionSet, queueItems) {

		var enabledQueueItems = [];
		var disabledQueueItems = [];

		optionSet.options.forEach (function (option) {

			var nextQueueItems = [];

			queueItems.forEach (function (queueItem) {

				var match =
					queueItemMatches (queueItem, option);

				if (! match) {
					nextQueueItems.push (queueItem);
					return;
				}

				if (option.checkboxElem.prop ("checked")) {

					enabledQueueItems.push (
						queueItem);

				} else {

					disabledQueueItems.push (
						queueItem);

				}

			});

			queueItems = nextQueueItems;

		});

		var oldestDisabled = undefined;

		disabledQueueItems.forEach (
			function (queueItem) {

			if (! oldestDisabled) {
				oldestDisabled = queueItem;
				return;
			}

			if (oldestDisabled.oldestTimestamp < queueItem.oldestTimestamp)
				return;

			oldestDisabled = queueItem;

		});

		return {
			queueItems: enabledQueueItems,
			oldestDisabled: oldestDisabled
		};

	};

	// show or hide every queue item as appropriate

	var showAndHideQueueItems = function () {

		var queueItems = allQueueItems;
		var oldestDisabled = undefined;

		// filter queue items based on each option set

		optionSets.forEach (
			function (optionSet) {

			var ret =
				filterQueueItems (
					optionSet,
					queueItems);

			queueItems =
				ret.queueItems;

			if (
				ret.oldestDisabled
				&& (
					oldestDisabled == undefined
					|| ret.oldestDisabled.oldestTimestamp
						< oldestDisabled.oldestTimestamp
				)
			) {

				oldestDisabled = ret.oldestDisabled;

			}

		});

		// hide and display them as appropriate

		$(".queueItemTable .queueItemRow").hide ();

		queueItems.forEach (
			function (queueItem) {

			queueItem.row.show ();

		});

		$(".queueItemTable .loadingRow").hide ();

		// display disabled info

		var numDisabled =
			allQueueItems.length - queueItems.length;

		var disabledInfo =
			"" + numDisabled + " items disabled";

		if (oldestDisabled) {

			disabledInfo +=
				", oldest is " + oldestDisabled.oldestString;

		}

		$(".disabledInfo").text (
			disabledInfo);

	};

	// go

	go ();

});
