
var canvas;
// below used to determine canvas absolute pixel locations
var canvasLeft;
var canvasTop;
var players;
var timeSinceClick;
var debounceInterval = 500;

window.onload = init;

function init() {
	players = new Array();
	canvas = document.getElementById('canvas');
	context = canvas.getContext('2d');
	canvas.height = canvas.width * (15 / 26);
	canvasLeft = canvas.offsetLeft;
	canvasTop = canvas.offsetTop;
	drawBoard();
	drawPlayer(0, 1);
	timeSinceClick = new Date();
	var player = {name:"John", row: 0, column: 1};
	players.push(player);
	    
	    canvas.addEventListener('click', (e) => {
	    	var time = new Date();
	    	if(time - timeSinceClick > debounceInterval){
	    		timeSinceClick = new Date();
	    	    //console.log(e.clientX - canvasLeft);
	    	    //console.log(e.clientY - canvasTop);
	    	  
	    	    players.forEach(player => {
	    		 // console.log("checking");
	    		 if(isIntersect(e, player)) {
	    			 console.log(player);
	    		 }
	    	  });
	    	}});
	    
	    var game = document.getElementById("gameId").value;
		var team = document.getElementById("teamId").value;
		var socket = new SockJS('/messages');
		stompClient = Stomp.over(socket);
		stompClient.connect({}, function (frame) {
		    stompClient.subscribe('/topic/game/'+ game, function (info) {
		    	//players.push(JSON.parse(info.body));
		       // draw(JSON.parse(info.body).content.currentColumn, JSON.parse(info.body).content.currentRow);
		        // showGreeting(JSON.parse(info.body).content.name);
		    });
		    stompClient.subscribe('/queue/game/'+ game + "/" + team, function (info) {
		    	//players.push(JSON.parse(info.body));
		       // draw(JSON.parse(info.body).content.currentColumn, JSON.parse(info.body).content.currentRow);
		        // showGreeting(JSON.parse(info.body).content.name);
		    });
		});
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
		context.strokeStyle = "white";
		context.lineWidth = 3;
		context.strokeRect(column * squareH, row * squareH, squareH,
				squareH);
	}
}

function isIntersect(click, player){
	var rect = canvas.getBoundingClientRect()
	var squareSize = canvas.offsetWidth/26;
	var playerColumn = player.column;
	var playerRow = player.row;
	var clickX = click.clientX - rect.left;
	var clickY = click.clientY - rect.top;
	console.log(clickX);
	console.log(clickY);
	return clickY > (squareSize * playerColumn) &&
	       clickY < (squareSize * (playerColumn +1)) &&
	       clickX > (squareSize * playerRow) &&
	       clickX < (squareSize * (playerRow+1));
	
}