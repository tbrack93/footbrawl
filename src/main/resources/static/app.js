
var canvas;
var context;
var background;
var squares;
var animation;
var animationContext;
var selection;
var modal;
// below used todetermine canvas absolute pixel locations
var canvasLeft;
var canvasTop;
var players;
var lastSquareClicked;
var timeSinceClick;
var debounceInterval = 200;
var game;
var team;
var team1;
var team2;
var activePlayer;
var yourTurn;
var route;
var inRoute;
var postRouteSquares;
var waypoints;
var xIncrement;
var yIncrement;
var rolls; 
var animating;
var taskQueue;
var inModal;
var rerollRoute;
var inPickUp;
var ballLocation;
var turnover;

var requestAnimationFrame = window.requestAnimationFrame || 
window.mozRequestAnimationFrame || 
window.webkitRequestAnimationFrame || 
window.msRequestAnimationFrame;

window.onload = init;
document.addEventListener("keydown", escCheck);

function init() {
	setDraggable();
	yourTurn = true; // just for testing
	players = new Array();
	waypoints = new Array();
	route = new Array();
	taskQueue = new Array();
	canvas = document.getElementById("canvas");
	context = canvas.getContext("2d");
	canvas.height = canvas.width * (15 / 26);
	canvasLeft = canvas.offsetLeft;
	background = document.getElementById("backgroundCanvas");
	background.height = background.width * (15/26);
	squares = document.getElementById("squaresCanvas");
	squares.height = squares.width * (15/26);
	animation = document.getElementById("animationCanvas");
	animation.height = animation.width * (15/26);
	animationContext = animation.getContext("2d");
	selection = document.getElementById("selectionCanvas");
	selection.height = selection.width * (15/26);
	modal = document.getElementById("modalCanvas");
	modal.height = modal.width * (15/26);
	canvasTop = canvas.offsetTop;
	rolls = document.getElementById("rolls");
	rolls.style.paddingTop = "" + (canvas.clientHeight + 10) +"px";
	drawBoard();
	timeSinceClick = new Date();
// var player = {id: 1, team: 1, name:"John", location: [0, 1]};
// var player2 = {id: 2, team: 2, name:"Bobby", location: [7,5]};
// var player3 = {id: 3, team: 2, name:"Sam", location: [7,7]};
// var player4 = {id: 4, team: 1, name:"Sarah", location: [5,3]};
// players.push(player);
// players.push(player2);
// players.push(player3);
// players.push(player4);
// drawPlayers();
	    
	    document.getElementById("modalCanvas").addEventListener('click', (e) => {
	    	var time = new Date();
	    	if(time - timeSinceClick > debounceInterval){
	    		timeSinceClick = new Date();
	    	    actOnClick(e);
	    	}});
	    
	    game = document.getElementById("gameId").value;
		team = document.getElementById("teamId").value;
		var socket = new SockJS('/messages');
		stompClient = Stomp.over(socket);
		stompClient.connect({}, function (frame) {
		    stompClient.subscribe('/topic/game/'+ game, function (message) {
		    	console.log("Message received");
		    	decodeMessage(JSON.parse(message.body));
	
		    });
		    stompClient.subscribe('/queue/game/'+ game + "/" + team, function (message) {
		    	console.log("Message received");
		    	decodeMessage(JSON.parse(message.body));
		    });
		    stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		    JSON.stringify({"type": "INFO", action: "TEAMS"}));
		});
		
	}

function drawBoard() {
	var backgroundCtx = background.getContext("2d");
	// size of canvas
	var cw = background.width;
	var ch = background.height;

	var squareH = ch / 15;
	backgroundCtx.globalAlpha = 1;
	backgroundCtx.lineWidth = 0.6;

	for (var x = 0; x <= cw; x += squareH) {
		backgroundCtx.moveTo(0.5 + x, 0);
		backgroundCtx.lineTo(0.5 + x, ch);
	}

	for (var x = 0; x <= ch; x += squareH) {
		backgroundCtx.moveTo(0, 0.5 + x);
		backgroundCtx.lineTo(cw, 0.5 + x);
	}
	
	backgroundCtx.strokeStyle = "white";
	backgroundCtx.stroke();
}

function drawPlayers(){
	context.clearRect(0, 0, canvas.width, canvas.height);
	players.forEach(player => {
		if(player.location != null){
		  drawPlayer(player);
		}
	});
	
}

function drawPlayerBorders(){
	selection.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	players.forEach(player => {
		drawSelectionBorder(player);
	});
	drawBall();
}

function drawPlayer(player) {
	  var img = new Image();
	  if(player.hasBall == true){
		  console.log("has ball");
		    var playerImg = new Image();
		    playerImg.src = player.imgUrl;
		    playerImg.onload = function() { 
		    	console.log("player image loaded");
			  var ballImg = new Image();
		      ballImg.src = "/images/ball.png";
		      ballImg.onload = function(){
		    	  console.log("ball image loaded");
			    var offScreenCanvas = document.createElement('canvas');
			    var squareH = canvas.height / 15;
		        offScreenCanvas.width = squareH;
		        offScreenCanvas.height = squareH;
		        var offscreenCtx = offScreenCanvas.getContext("2d");
		        offscreenCtx.drawImage(playerImg, 0, 0, squareH, squareH);
		        offscreenCtx.drawImage(ballImg, squareH/3, squareH/3, squareH/1.5,
					squareH/1.5);
		        img.src = offScreenCanvas.toDataURL();
		      }
		    }
		} else{
			img.src = player.imgUrl;
		}
	  img.onload = function() {
		  context.save();
		  context.globalAlpha = 1;
			var column = player.location[0];
			var row = 14 -player.location[1];
			var squareH = canvas.height / 15;
			context.clearRect(column * squareH-5, row * squareH-5, squareH+10,squareH+10);
			if(player.status != "standing"){
				context.save();
				var angle = 270;
				if(player.status == "stunned"){
					angle = 90;
					context.translate(column * squareH + squareH, row * squareH) ;
				} else {
					context.translate(column * squareH, row * squareH + squareH) ;
				}
				context.rotate(angle * Math.PI / 180);
				context.drawImage(img, 0, 0, squareH,
							squareH);
	
				context.restore();
			} else {
				context.drawImage(img, column * squareH, row * squareH, squareH,
						squareH);
			}
			drawSelectionBorder(player);
	  }
}

function drawSelectionBorder(player){
	if(player == activePlayer && animating == true){
		return;
	}
	var column = player.location[0];
	var row = 14 -player.location[1];
	var squareH = canvas.height / 15;
	var ctx = selectionCanvas.getContext("2d");
	ctx.clearRect(column * squareH-5, row * squareH-5, squareH+10,squareH+10);
	ctx.save();
	ctx.strokeStyle = "white";
	if(player.hasBall == true){
		ctx.strokeStyle = "orange";
		}
		var line = 3;
		if(player == activePlayer){
			line = 8;
		}
		ctx.lineWidth = line;
		ctx.strokeRect(column * squareH, row * squareH, squareH,
				squareH);
		ctx.restore();
}

function drawBall(){
	if(ballLocation != null){
	  var img = new Image();
	  img.src = "/images/ball.png";
	  img.onload = function() {
		  context.save();
		  context.globalAlpha = 1;
			var column = ballLocation[0];
			var row = 14 -ballLocation[1];
			var squareH = canvas.height / 15;
			var ctx = selection.getContext("2d");
			ctx.save();
			ctx.strokeStyle = "orange";
			ctx.lineWidth = 8;
			ctx.strokeRect(column * squareH, row * squareH, squareH,
					squareH);
			context.drawImage(img, column * squareH + squareH/3, row * squareH + squareH/3, squareH/1.5,
					squareH/1.5);
			}
  }
}


function drawMovementSquare(tile){
	    squareCtx = squares.getContext("2d");
	    squareCtx.save();
	    squareCtx.globalAlpha = 0.4;
	    var squareH = squares.height/15;
        var colour = "blue";
        if(tile.tackleZones != null){
        	colour = "red";
        	// need to add logic for number of tacklezones
        }
        squareCtx.fillStyle = colour;
	    var column = tile.position[0];
	    var row = 14 - tile.position[1];
	    squareCtx.fillRect(column*squareH+3, row*squareH+3 , squareH-5, squareH-5);
	    if(tile.tackleZones !=null){
	    	squareCtx.globalAlpha = 1;
	    	squareCtx.fillStyle = "white";
	    	squareCtx.font = "30px Arial";
	    	squareCtx.fillText(tile.tackleZones, column*squareH +20, row*squareH +30);
	    }
	    if(tile.goingForItRoll != null){
	    	squareCtx.globalAlpha = 1;
	    	squareCtx.fillStyle = "white";
	    	squareCtx.font = "30px Arial";
	    	squareCtx.fillText("GFI",column*squareH + squareH/3, row*squareH + squareH/2+10);
	    }
	    if(tile.dodgeRoll != null){
	    	squareCtx.globalAlpha = 1;
	    	squareCtx.textAlign = "center"; 
	    	squareCtx.fillStyle = "white";
	    	squareCtx.font = "bold 30px Arial";
	    	squareCtx.fillText("Dodge", column*squareH+ squareH/2, row*squareH + squareH/2+10);
	    	squareCtx.fillText(tile.dodgeRoll + "+", column*squareH+ squareH/2, row*squareH + squareH/1.25)
	    }
	    squareCtx.restore(); 
	}

function drawRouteSquare(tile){
    squareCtx = squares.getContext("2d");
    squareCtx.save();
    squareCtx.globalAlpha = 0.5;
    var squareH = canvas.height/15;
    var colour = "white";
    if(tile.dodgeRoll != null || tile.goingForItRoll != null){
    	colour = "orange";
    }
    squareCtx.fillStyle = colour;
    var column = tile.position[0];
    var row = 14 - tile.position[1];
    squareCtx.fillRect(column*squareH+3, row*squareH+3 , squareH-5, squareH-5);
    if(tile.position[0] == activePlayer.location[0] && tile.position[1] == activePlayer.location[1]){
    	return;
    }
    if(tile.goingForItRoll != null){
    	squareCtx.globalAlpha = 1;
    	squareCtx.fillStyle = "white";
    	squareCtx.textAlign = "center"; 
    	squareCtx.font = "bold 30px Arial";
    	squareCtx.fillText("GFI: " + tile.goingForItRoll + "+",column*squareH + squareH/2, row*squareH + squareH/4);
    }
    if(tile.dodgeRoll != null){
    	squareCtx.globalAlpha = 1;
    	squareCtx.textAlign = "center"; 
    	squareCtx.fillStyle = "white";
    	squareCtx.font = "bold 30px Arial";
    	squareCtx.fillText("Dodge:", column*squareH+ squareH/2, row*squareH + squareH/2+10);
    	squareCtx.fillText(tile.dodgeRoll + "+", column*squareH+ squareH/2, row*squareH + squareH/1.25)
    }
    squareCtx.restore();
}

function decodeMessage(message){
	console.log("Decoding message");
	if(message.type == "INFO"){
		console.log("in info");
		if(message.action == "MOVEMENT"){
			showMovement(message);
		} else if(message.action == "ROUTE"){
			showRoute(message);
		} else if(message.action == "REROLLCHOICE"){
			showRerollUsed(message);
		} else{ if(message.action == "ARMOURROLL"){
			if(animating == true){
				  var task = function(m){
						showArmourRoll(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
		    		showArmourRoll(message);
		      }
		} else if(message.action == "INJURYROLL"){
			if(animating == true){
				  var task = function(m){
						showInjuryRoll(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
					showInjuryRoll(message);
		      }
		} else if(message.action == "TURNOVER"){
			turnover = true;
			if(animating == true){
				  var task = function(m){
					  showTurnover(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
		    	  showTurnover(message); 
		      }
		} else if(message.action == "NEWTURN"){
			var delay = 0;
			if(turnover == true){
				delay = 2500;
			}
			setTimeout(function() {
				showNewTurn(message);
			}, delay);

		} else if(message.action == "TOUCHDOWN"){
			if(animating == true){
				  var task = function(m){
					  showTouchdown(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
		    	  showTouchdown(message); 
		      }
		}
		}
	} else if(message.type == "ACTION"){
	    if(message.action == "ROUTE"){
	      activePlayer = getPlayerById(message.player);
	      if(animating == true){
	    	  var task = function(m){
	    		  showMoved(m, "normal");
	    	  };
	    	  var t = animateWrapFunction(task, this, [message]);
	    	  taskQueue.push(t);
	      } else{ 	
		    showMoved(message, "normal");
	      }
	  } else if(message.action == "ROLL"){
		  activePlayer = getPlayerById(message.player);
		  if(animating == true || inModal == true){
			  var task = function(m){
	    		  showRoll(m);
	    	  };
	    	  var t = animateWrapFunction(task, this, [message]);
	    	  taskQueue.push(t);
	      } else{ 	
	    	  showRoll(message); 
	      }
	  } else if(message.action == "BALLSCATTER"){
		  if(animating == true){
			  var task = function(m){
	    		  showBallScatter(m);
	    	  };
	    	  var t = animateWrapFunction(task, this, [message]);
	    	  taskQueue.push(t);
	      } else{ 	
	    	  showBallScatter(message); 
	      }
	  }	    
   }
}

function showMovement(message){
	squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	message.squares.forEach(tile => {
		drawMovementSquare(tile);
	});
	if(inRoute == true){
	  postRouteSquares = message.squares;
	  route.forEach(tile => {
	    drawRouteSquare(tile);
	  });
	} else{
	activePlayer.movement = message.squares;
	}
}

function actOnClick(click){
	if(inModal == true){
		return;
	}
	var square = determineSquare(click);
	var done = false;
	players.forEach(player => {
		 // console.log("checking");
		 if(player.location[0] == square[0] && player.location[1] == square[1]) {
			 console.log(player.name);
			 console.log(player.id);
			 if(player == activePlayer){
				 resetMovement();
				 done = true; // needed as return just escapes forEach block
				 return;
			 } else{	
			   var pTemp = activePlayer;
			   activePlayer = player;
			   if(pTemp != null){
			     drawPlayer(pTemp);
			   }
			   drawPlayer(activePlayer);
			   inRoute = false;
			   waypoints.length = 0;
			   route.length = 0;
			   
			   // console.log(player);
			   stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
			                 JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": player.id,
			                 "location": player.location, "routeMACost": 0}));
			   done = true;
			   return;
			 }
		 } // will be more options for blitz/ block/ throw actions
	 });
	if(done == false && activePlayer != null && activePlayer.team == team && yourTurn == true){ 
		if(lastSquareClicked != null && square[0] == lastSquareClicked[0] && square[1] == lastSquareClicked[1]){
			var messageRoute = new Array();
			route.forEach(tile => {
				var t = tile.position;
				messageRoute.push(t);
			});
			  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	                    JSON.stringify({"type": "ACTION", "action": "ROUTE", "player": activePlayer.id, 
	        	        "location": activePlayer.location, "route": messageRoute}));
		}
		else if(playerCanReach(square) && route.length <= activePlayer.remainingMA +2){
		   stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
		                    JSON.stringify({"type": "INFO", "action": "ROUTE", "player": activePlayer.id, 
		        	        "location": activePlayer.location, "target": square, "waypoints": waypoints}));
		   lastSquareClicked = square;
		} else{
			console.log("player can't reach that square");
		}
		
	}
	 
}

function determineSquare(click){
	var rect = canvas.getBoundingClientRect()
	var squareSize = canvas.offsetWidth/26;
	var clickX = click.clientX - rect.left;
	var clickY = click.clientY - rect.top;
	var position = [parseInt(clickX/squareSize, 10), 14 - parseInt(clickY/squareSize, 10)];
	return position;
}

function showRoute(message){
	route = message.route;
	if(route.length > 0){
	  if(inRoute == false){
	  	  waypoints.length = 0;
		  inRoute = true;
	  }
	  squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	  var location = route[route.length -1].position;
	  
	  waypoints.push(location);
	  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	         JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": activePlayer.id,
	                         "location": location, "routeMACost": message.routeMACost}));
	} // else say it's empty
}

function showMoved(message, type){
	if(message.route.length >1 || type != "normal"){
		context.save();
		console.log("Type " + type);
		animating = true;
		route = message.route;
		console.log(route.length)
		var end = message.end;
		console.log(end);
	    var player = getPlayerById(message.player);
	    activePlayer = player;
	    var img = new Image();
	    var squareH = canvas.height / 15;
		if(player.hasBall == true){
			  console.log("has ball");
			    var playerImg = new Image();
			    playerImg.src = player.imgUrl;
			    playerImg.onload = function() { 
			    	console.log("player image loaded");
				  var ballImg = new Image();
			      ballImg.src = "/images/ball.png";
			      ballImg.onload = function(){
			    	  console.log("ball image loaded");
				    var offScreenCanvas = document.createElement('canvas');
			        offScreenCanvas.width = squareH;
			        offScreenCanvas.height = squareH;
			        var offscreenCtx = offScreenCanvas.getContext("2d");
			        offscreenCtx.drawImage(playerImg, 0, 0, squareH, squareH);
			        offscreenCtx.drawImage(ballImg, squareH/3, squareH/3, squareH/1.5,
						squareH/1.5);
			        img.src = offScreenCanvas.toDataURL();
			      }
			    }
		} else{
			img.src = player.imgUrl;
		}
		img.onload = function() { 
	      squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	      var startingX = route[0].position[0] * squareH;
		  var startingY = (14 - route[0].position[1]) * squareH;
		  var targetX = route[1].position[0] * squareH;
		  var targetY = (14 - route[1].position[1]) * squareH;
		  var speed = 10;
		  xIncrement = (targetX - startingX) / speed;
	      yIncrement = (targetY - startingY) / speed;
	      if(type === "tripped"){
	    	player.status = "prone";
			console.log("tripping time");
			speed = 5; // lower is faster
		  }
		  if(type === "dodge"){
			speed = 5;
		  }
		  context.clearRect(startingX, startingY, squareH, squareH);
		  context.clearRect(targetX, targetY, squareH, squareH);
		  drawPlayerBorders();
		  drawBall();
		  animateMovement(message.route, 0, img, startingX, startingY, targetX, targetY, squareH, end, "N"); 
		  player.location = route[route.length-1].position;
	      waypoints.length = 0;
	      inRoute = false;
		}
	    // context.restore();
	}
}

function animateMovement(route, counter, img, startingX, startingY, targetX, targetY, squareH, end, ball){
	animationContext = animation.getContext("2d");
	animationContext.clearRect(startingX, startingY, squareH, squareH);
	drawPlayerBorders();
	var newX = startingX + xIncrement;
	var newY = startingY + yIncrement;
	if(ball == "Y"){
		animationContext.drawImage(img, newX, newY, squareH/1.5, squareH/1.5);	
	} else{
	animationContext.drawImage(img, newX, newY, squareH, squareH);
	}
	if(Math.round(newX) == Math.round(targetX) && Math.round(newY) == Math.round(targetY)){
		if(counter == route.length-1){
			route.length = 0;
			animation.getContext("2d").clearRect(0, 0, animation.height, animation.width);
			if(ball == "Y"){
				drawBall();
			} else if(end == "Y" || activePlayer.status == "prone"){
		        drawPlayer(activePlayer);
		        animation.getContext("2d").clearRect(0, 0, animation.height, animation.width);
		      if(activePlayer.team == team && end == "Y"){
			      stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
				         JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": activePlayer.id,
				                         "location": activePlayer.location, "routeMACost": 0}));
			  }
		    } 
			animating = false;
			console.log("tasks in queue: " + taskQueue.length);
			if(taskQueue.length != 0){
		    	(taskQueue.shift())();
		    }
			return;
		}
		counter++;
	    targetX = route[counter].position[0] * squareH;
	    targetY = (14 - route[counter].position[1]) * squareH;
		xIncrement = (targetX - newX) / 10;
	    yIncrement = (targetY - newY) / 10;
	}
	requestAnimationFrame(function() { animateMovement(route, counter, img, newX, newY, targetX, targetY, squareH, end, ball); });	
}

function escCheck (e) {
	console.log("keyPress Time");
    if(e.keyCode === "Escape" || e.keyCode === "Esc" || e.keyCode === 27) {
    	console.log("escape");
    	if(activePlayer != null && activePlayer.movement != null){
    		resetMovement();
        }
    }
}

function resetMovement(){
	inRoute = false;
	waypoints.length = 0;
	route.length = 0;
	lastSquareClicked = null;
	squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	if(activePlayer.movement.length>0){
	  activePlayer.movement.forEach(tile => {
		drawMovementSquare(tile);
	  });
	}
}

function playerCanReach(square){
	var result = false;
	activePlayer.movement.forEach(tile => {
		if(tile.position[0] == square[0] && tile.position[1] == square[1]){
			result = true;
		}
	});
	return result;
}

function getPlayerById(id){
	for(i = 0; i< players.length ; i++){
		if(players[i].id == id){
			return players[i]
		}
	}
}

function showRoll(message){
	if(message.rollOutcome == "failed"){
		var task = function(m){
		  showFailedAction(m);
  	  };
  	  var t = animateWrapFunction(task, this, [message]);
  	  taskQueue.unshift(t);
	}
	squares.getContext("2d").clearRect(0, 0, squares.width, squares.height);
	var newRolls = document.getElementById("newRolls");
	console.log(newRolls);
	newRolls.innerHTML =  message.playerName + ": " +message.rollType + " Needed: " + message.rollNeeded + " Rolled: " +
	                  +  message.rolled + " Result: " + message.rollOutcome + "</br>" + newRolls.innerHTML;
	if(message.rollType == "DODGE"){
		showDodgeResult(message);
	}
	if(message.rollType == "GFI"){
		showGFIResult(message);
	}
	if(message.rollType == "PICKUPBALL"){
		showPickUpResult(message);
	}
}

function resizeActions(){
	console.log("resize");
	rolls.style.paddingTop = "" + canvas.clientHeight + "px";
}

function showDodgeResult(message){
	var type = "dodge";
	if(message.rollOutcome === "failed"){
		type = "tripped";
		 activePlayer.status = "prone";
	} else{
		activePlayer.status = "standing";
	}
	message.route = [{position: message.location}, {position: message.target}];
	showMoved(message, type);	
}

function showGFIResult(message){
	var type = "normal";
	if(message.rollOutcome === "failed"){
	  type = "tripped";
	  activePlayer.status = "prone";
	} else{
		activePlayer.status = "standing";
	}
	  message.route = [{position: message.location}, {position: message.target}];
	  showMoved(message, type);
}

function showPickUpResult(message){
	if(inPickUp != true){ 
	  inPickUp = true;
	  message.route = [{position: message.location}, {position: message.target}];
	  showMoved(message, "normal");
	}
	 var p = getPlayerById(message.player);
	 if(message.rollOutcome === "success"){
		 p.hasBall = true;
		 p.location = message.target;
		 inPickUp = false;
	 }
	 if(message.end == "Y"){
		 drawPlayer(p);
	 }
}

function showBallScatter(message){
	console.log("scattering");
	animating = true;
	shadow.style.display = "none";
	//document.getElementById("modal").style.display = "none";
	newRolls.innerHTML =  "Ball scattered to " + message.target + "</br>" + newRolls.innerHTML;
	document.getElementById("modalOptions").innerHTML = document.getElementById("modalOptions").innerHTML + "<p> Ball scattered to " + message.target + "</p>"
	var ballImg = new Image();
	ballImg.src = "/images/ball.png";
    ballImg.onload = function() { 
    squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
    var squareH = canvas.height / 15;
    context.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);

    var startingX = message.location[0] * squareH + squareH/3;
    var startingY = (14 - message.location[1]) * squareH + squareH/3;
    var targetX = message.target[0] * squareH + squareH/3;
    var targetY = (14 - message.target[1]) * squareH + squareH/3;
    var speed = 10;
    xIncrement = (targetX - startingX) / speed;
    yIncrement = (targetY - startingY) / speed;
    drawPlayers();
    var scatterRoute = [message.target];
    console.log("route created: " + scatterRoute);
    ballLocation = message.target;
    animationContext.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
    animateMovement(scatterRoute, 0, ballImg, startingX, startingY, targetX, targetY, squareH, "N", "Y"); 
    setTimeout(function(){
    // document.getElementById("modal").style.display = "block";
    // modal.style.display = "block";
   shadow.style.display = "block";
   if(taskQueue.length != 0){
   (taskQueue.shift())();
   }
   }, 1000);  
  }
}


// adapted from
// https://stackoverflow.com/questions/899102/how-do-i-store-javascript-functions-in-a-queue-for-them-to-be-executed-eventuall
var animateWrapFunction = function(func, context, params) {
    return function() {
        func.apply(context, params);
    };
}

function showFailedAction(message){
	animationContext.clearRect(0,0, animation.width, animation.height);
	inModal = true;
	console.log("showingFailed");
	var shadow = modal.getContext("2d");
   // shadow.clearRect(0, 0, shadow.width, shadow.height);
    shadow.globalAlpha = 0.4;
    shadow.fillStyle = "black";
    shadow.fillRect(0,0, modal.width, modal.height);
    var player = getPlayerById(message.player); 
    var column = player.location[0];
	var row = 14 -player.location[1];
	var squareH = modal.height / 15;
	shadow.clearRect(column * squareH-5, row * squareH-5, squareH+10,squareH+10);
	var display = document.getElementById("modal");
	display.style.display = "block";
	squareH = modal.clientHeight/15;
	display.style.left = ""+ (column +2) * squareH-5 + "px";
	display.style.top = "" + (row -5) * squareH-5 + "px";
	var effect = " fell down.";
	if(message.rollType == "PICKUPBALL"){
		effect = " dropped the ball.";
	}
	document.getElementById("modalTitle").innerHTML = message.playerName + effect;
	document.getElementById("modalText").innerHTML = message.playerName + " failed to " + message.rollType + "</br></br>" +
	                                                 "Needed: " + message.rollNeeded + "  Rolled: " + message.rolled;
	if(message.rerollOptions == null || message.rerollOptions.length == 0){
	  document.getElementById("modalOptions").innerHTML = "<p> No possible rerolls </p>";
	} else if(message.userToChoose != team){
	   document.getElementById("modalOptions").innerHTML = "<p> Awaiting opponent reroll decision </p>" +
	                          "Possible rerolls: " + message.rerollOptions;
	} else{
		rerollRoute = [{position: message.location}, {position: message.target}]; 
		requestReroll(message.rerollOptions);
	}
	drawPlayer(activePlayer);

	 setTimeout(function(){
		    // document.getElementById("modal").style.display = "block";
		    // modal.style.display = "block";
		   if(taskQueue.length != 0){
		   (taskQueue.shift())();
		   }
		   }, 200);  
}

function requestReroll(options){
	var modalOptions = document.getElementById("modalOptions");
	modalOptions.innerHTML = "Reroll Options </br> </br>";
	for(i = 0; i < options.length; i++){
		var button = document.createElement("BUTTON");
        button.innerHTML = options[i];
        button.onclick = function() {sendRerollChoice(this.innerHTML)};
        modalOptions.appendChild(button);
	}
    var button = document.createElement("BUTTON")
    button.innerHTML = "Don't reroll";
    button.id = "dontReroll";
    button.onclick = function() {sendRerollChoice("Don't reroll")};
    modalOptions.appendChild(button);
}

function sendRerollChoice(choice){
	console.log(choice);
	 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
             JSON.stringify({"type": "ACTION", "action": "REROLL", "player": activePlayer.id,
             "rerollChoice": choice}));
}

function resetModal(message){
	console.log("resetting Modal");
	modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	document.getElementById("modal").style.display = "none";
	var p = getPlayerById(message.player);
    p.status = "standing";
    if(inPickUp != true){
    p.location = message.location;
    drawPlayers();
    }
	inModal = false;
// if(taskQueue.length != 0){
// setTimeOut((taskQueue.shift())(), 1000);
// }
}

function showRerollUsed(message){
	var newRolls = document.getElementById("newRolls");
	var choice = "";
	if(message.rerollChoice === "Don't reroll"){
		choice = " chose not to use a reroll.";
	} else{
		choice = " chose to reroll using " + message.rerollChoice + ".";
	}
	newRolls.innerHTML =  message.teamName + choice + "</br>" + newRolls.innerHTML;
	if(message.rerollChoice != "Don't reroll"){
		   resetModal(message);
    } else{
    	var chooser = message.teamName;
    	if(message.userToChoose == team){
    		chooser = "You"
    	}
    	document.getElementById("modalOptions").innerHTML = "<p>" + chooser + choice + "</p>";
    } if(message.rerollChoice == "Team Reroll"){
    	var rerolls = "";
    	if(message.userToChoose == team1.id){
    		rerolls = "team1Rerolls";
    		team1.remainingTeamRerolls--;
    	} else{
    		rerolls = "team2Rerolls";
    		team2.remainingTeamRerolls--;
    	}
    		document.getElementById(rerolls).innerHTML = "Team Rerolls: " + team1.remainingTeamRerolls;
    	}
    }

function showArmourRoll(message){
	console.log("showing armour");
	document.getElementById("modal").style.display = "block"; 
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + "'s "+ message.rollOutcome + ". Armour: "  + message.rollNeeded + " Rolled: " +
	                      message.rolled + "</br>" + newRolls.innerHTML;
	document.getElementById("modalOptions").innerHTML = "<p>" + message.playerName + "'s " + message.rollOutcome + "." + "</p>";
	document.getElementById("animationCanvas").getContext("2d").clearRect(0, 0, animating.width, animating.height);
	if(taskQueue.length != 0){
    	(taskQueue.shift())();
    }
}

function showInjuryRoll(message){
	console.log("showing Injury");
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + " was "+ message.rollOutcome + ". " + "Rolled: " + message.rolled + "</br>" + newRolls.innerHTML;
	var existing = document.getElementById("modalOptions").innerHTML;
	document.getElementById("modalOptions").innerHTML = existing + "<p>" + message.playerName + " is " + message.rollOutcome + "." + "</p>";
	var player = getPlayerById(message.player);
    player.status = message.playerStatus;
    player.location = message.location;
    player.hasBall = false;
    document.getElementById("animationCanvas").getContext("2d").clearRect(0, 0, animating.width, animating.height);
	// drawPlayers();
	if(taskQueue.length != 0){
    	(taskQueue.shift())();
    }
}

function showTurnover(message){
    document.getElementById("modal").style.display = "block"; 
    modal.style.display = "block";
	var existing = document.getElementById("modalOptions").innerHTML;
	var name = message.teamName;
	if(message.userToChoose == team){
		name = "You";
	}
	newRolls.innerHTML =  name + " suffered a turnover" + "</br>" + newRolls.innerHTML;
	document.getElementById("modalOptions").innerHTML = existing + "<hr> <p style='color:red;'>" + name + " suffered a turnover" + "</p>";
	  if(taskQueue.length != 0){  
	   (taskQueue.shift())();
	   }
}

function showNewTurn(message){
	inModal = false;
	animation.getContext("2d").clearRect(0,0, animation.width, animation.height);
	ballLocation = message.ballLocation;
	modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	selection.getContext("2d").clearRect(0, 0, selection.width, selection.height);
	document.getElementById("modal").style.display = "none";
	newRolls.innerHTML =  message.teamName + "'s turn." + "</br>" + newRolls.innerHTML;
	team1 = message.team1FullDetails;
	team2 = message.team2FullDetails;
	players.length = 0;
	team1.playersOnPitch.forEach(player =>{
		players.push(player);
	});
	team2.playersOnPitch.forEach(player =>{
		players.push(player);
	});
	activePlayer = null;
	var teamName = message.teamName + "'s Turn";
	yourTurn = false;
	if(message.userToChoose == team){
		teamName = "Your turn";
		yourTurn = true;
		document.getElementById("endTurn").classList.remove("disabled");
	} else {
		document.getElementById("endTurn").classList.add("disabled");
	}
	document.getElementById("team1Name").innerHTML = message.team1Name;
	document.getElementById("team2Name").innerHTML = message.team2Name;
	document.getElementById("activeTeam").innerHTML = teamName;
	turnover = false;
	document.getElementById("score").innerHTML = ""+ message.team1Score + " - " + message.team2Score;
	document.getElementById("team2Turn").innerHTML = "Current Turn: " + team2.turn;
	document.getElementById("team1Turn").innerHTML = "Current Turn: " + team1.turn;
	document.getElementById('team1Rerolls').innerHTML = "Team Rerolls: " + team1.remainingTeamRerolls;
	document.getElementById('team2Rerolls').innerHTML = "Team Rerolls: " + team2.remainingTeamRerolls;
	drawPlayers();
    drawBall();
}

function endTurn(){
	if(yourTurn == true){
	  var result = confirm("Are you sure you want to end your turn?");
		if (result == true) {
		  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
					    JSON.stringify({"type": "ACTION", action: "ENDTURN"}));
		} else {
		  return;
		}
	} else{
		alert("Not your turn");
	}
}

function showTouchdown(message){
	document.getElementById("score").innerHTML = ""+ message.team1Score + " - " + message.team2Score;
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + " scored a touchdown for Team " + message.teamName + "!</br>" + newRolls.innerHTML;
	alert(message.playerName + " scored a touchdown for Team " + message.teamName + "!");
}