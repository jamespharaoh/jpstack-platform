// Copyright (c) Pharaoh Systems Consulting Ltd 2005

// ------------------------------------------------------------ globals

var subtrees = []; // stores subtree icons so they have a global variable name eg subtree[0]
var nextTreeId = 0; // used to generate document ids for divs and plus/minus images

var defaultTreeOpts = {
	imageWidth: 20,
	imageHeight: 20,
	blankImage: 'blank.gif',
	plusImage: 'plus.gif',
	minusImage: 'minus.gif'
};

// ------------------------------------------------------------ function SimpleTreeItemGenerator (...)

function SimpleTreeItemGenerator (icon) {
	this.icon = icon;
}

// ---------------------------------------- function SimpleTreeItemGenerator.prototype.generateTail (...)

SimpleTreeItemGenerator.prototype.generateTail = function (itemData) {
	return ' ' + itemData[2];
}

// ---------------------------------------- function SimpleTreeItemGenerator.prototype.generate (...)

SimpleTreeItemGenerator.prototype.generate = function (itemData, treeOpts, indent) {
	var s = '';

	// get a treeId
	var myTreeId = nextTreeId++;

	// see if we are a leaf and allocate a place in 'subtrees' if so
	var subtreeIndex;
	if (itemData[1] > 0) {
		subtrees[subtreeIndex = subtrees.length] =
			new Subtree (
				itemData[itemData.length - 1],
				treeOpts,
				indent + 1,
				myTreeId,
				itemData[1] == 2? 1 : 0);
	}

	// open table
	s += '<table border="0" cellspacing="0" cellpadding="0"><tr><td nowrap>';

	// do indent
	for (var i = 0; i < indent; i++)
		s += '<img src="' + treeOpts.blankImage + '" align="absbottom"' +
			' width="' + treeOpts.imageWidth + '" height="' + treeOpts.imageHeight + '"/>';

	// do plus/minus
	if (itemData[1] > 0) {
		s += '<a href="javascript:subtrees[' + subtreeIndex + '].toggleVisible ()">';
		s += '<img id="' + myTreeId + '-img"' +
			 ' src="' + (itemData[1] == 2? treeOpts.minusImage : treeOpts.plusImage) + '"' +
			 ' align="absbottom" border="0"/>';
		s += '</a>';
	} else {
		s += '<img src="' + treeOpts.blankImage + '" align="absbottom"' +
			 ' width="' + treeOpts.imageWidth + '" height="' + treeOpts.imageHeight + '"/>';
	}

	// do icon
	s += '<img src="' + this.icon + '" align="absbottom"' +
		 ' width="' + treeOpts.imageWidth + '" height="' + treeOpts.imageHeight + '"/>';

	// do tail
	s += this.generateTail (itemData);

	// close table
	s += '</td></tr></table>\n';

	// create div
	if (itemData[1] > 0) {
		s += '<div id="' + myTreeId + '-div">';
		if (itemData[1] == 2)
			s += buildTree (itemData[itemData.length - 1], treeOpts, indent + 1);
		s += '</div>\n';
	}

	// and return
	return s;
}

// ------------------------------------------------------------ function buildTree (...)

function buildTree (treeData, treeOpts, indent) {
	if (indent == undefined) indent = 0;

	fillMissingTreeOpts (treeOpts);

	s = '';

	for (var i in treeData) {
		itemData = treeData[i];
		if (itemData == undefined) continue;
		s += itemData[0].generate (itemData, treeOpts, indent);
	}

	return s;
}

// ------------------------------------------------------------ function Subtree (...)

function Subtree (treeData, treeOpts, indent, treeId, state) {
	this.treeData = treeData;
	this.treeOpts = treeOpts;
	this.indent = indent;
	this.treeId = treeId;
	this.state = state;
}

// ---------------------------------------- function Subtree.prototype.toggleVisible (...)

Subtree.prototype.toggleVisible = function () {
	switch (this.state) {

	case 0: // subtree doesn't exist yet, go to visible
		document.getElementById (this.treeId + '-div').innerHTML =
			buildTree (this.treeData, this.treeOpts, this.indent);
		document.getElementById (this.treeId + '-img').src = this.treeOpts.minusImage;
		this.state = 1;
		break;

	case 1: // subtree is visible, go to invisible
		document.getElementById (this.treeId + '-div').style.display = 'none';
		document.getElementById (this.treeId + '-img').src = this.treeOpts.plusImage;
		this.state = 2;
		break;

	case 2: // subtree is invisible, go to visible
		document.getElementById (this.treeId + '-div').style.display = 'block';
		document.getElementById (this.treeId + '-img').src = this.treeOpts.minusImage;
		this.state = 1;
		break;
	}
}

// ------------------------------------------------------------ function fillMissingTreeOpts (...)

function fillMissingTreeOpts (treeOpts) {
	for (var key in defaultTreeOpts) {
		if (treeOpts[key] == undefined)
			treeOpts[key] = defaultTreeOpts[key];
	}
}
