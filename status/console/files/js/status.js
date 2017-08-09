var wbsStatus = {

	_handlers: {},

};

wbsStatus._init =
function wbsStatusInit () {

	async.onConnect (
		wbsStatus._handleConnect);

	async.onDisconnect (
		wbsStatus._handleError);

	$("#timeRow").hover (
		function () { $(this).addClass ("hover") },
		function () { $(this).removeClass ("hover") });

	$("#timeRow").click (function () {
		window.parent.frames.main.location = "/coreSystem";
	});

};

wbsStatus.handlerRegister =
function wbsStatusHandlerRegister (type, callback) {

	if (type in wbsStatus._handlers) {

		throw new Error (
			"Duplicated status handler: " + type);

	}

	wbsStatus._handlers [type] = callback;

	console.debug (
		"Registered status handler: " + type);

};

wbsStatus._handleConnect =
function wbsStatusHandleConnect () {

	console.debug (
		"Status update subscribe");

	async.subscribe (
		"/status/update",
		wbsStatus._handleUpdate);

};

wbsStatus._handleUpdate =
function wbsStatusHandleUpdate (payload) {

	payload.updates.forEach (function (update) {

		if (update.type in wbsStatus._handlers) {

			var handler =
				wbsStatus._handlers [update.type];

			handler (
				update.data);

		} else {

			console.warn (
				"Status update of unknown type: " + update.type);

		}

	});

	$("#loadingRow").hide ();

};

wbsStatus._handleError =
function wbsStatusHandleError () {

	$("#headerCell").text ("Status");
	$("#loadingRow").show ();
	$("#timeRow").hide ();

};

$(wbsStatus._init);

$(function () {

	wbsStatus.handlerRegister ("core",
	function (data) {

		// update header

		$("#headerCell").text (
			data.header);

		// update timestamp

		$("#timeCell").text (
			data.timestamp);

		$("#timeRow").show ();

		// update notice

		$("#noticeCell").text (
			data.notice);

		if (data.notice) {
			$("#noticeRow").show ();
		} else {
			$("#noticeRow").hide ();
		}

	});

});

// ex: noet ts=4 filetype=javascript