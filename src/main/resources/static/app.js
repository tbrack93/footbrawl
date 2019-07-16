window.onload = init;

var canvas;
// below used to determine canvas absolute pixel locations
var canvasLeft;
var canvasTop;

function init() {
	canvas = document.getElementById('canvas');
	context = canvas.getContext('2d');
	canvasLeft = canvas.offsetLeft, canvasTop = canvas.offsetTop;
	canvas.height = canvas.width * (15 / 26);
	drawBoard();
	drawPlayer(5, 5);
}

function drawBoard() {

	// size of canvas
	var cw = canvas.width;
	var ch = canvas.height;

	var squareH = ch / 15;

	for (var x = 0; x <= cw; x += squareH) {
		context.moveTo(0.5 + x, 0);
		context.lineTo(0.5 + x, ch);
	}

	for (var x = 0; x <= ch; x += squareH) {
		context.moveTo(0, 0.5 + x);
		context.lineTo(cw, 0.5 + x);
	}
	context.lineWidth = 0.5;
	// context.strokeStyle = "#696969";
	context.stroke();
}

function drawPlayer(column, row) {
	var img = new Image();
	var squareH = canvas.height / 15;
	img.src = "/images/human_blitzer.png";
	img.onload = function(){
		context.drawImage(img, column * squareH, row * squareH, squareH,
				squareH);	
	}
}