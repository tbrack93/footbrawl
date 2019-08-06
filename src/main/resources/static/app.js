
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
var team1Reserves;
var team2Reserves;
var team1PlayersOnPitch;
var team2PlayersOnPitch;
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
var inBlock;
var inThrow;
var lastRollLocation;
var pushOptions;
var followUp;
var actionChoice;
var phase;
var blockResults = ["Attacker Down", "Both Down", "Pushed", "Pushed", "Defender Stumbles",
"Defender Down"];
var diceImages = ["/images/attacker_down.png", "/images/both_down.png", 
	                        "/images/push_back.png", "/images/push_back.png",
	                        "/images/defender_stumbles.png", "/images/defender_down.png"];
var requestAnimationFrame = window.requestAnimationFrame || 
window.mozRequestAnimationFrame || 
window.webkitRequestAnimationFrame || 
window.msRequestAnimationFrame;

window.onload = init;
document.addEventListener("keydown", escCheck);

function init() {
	phase = "yourSetup";
	setDraggable();
	inPush = false;
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
		if(player.location != null){
		  drawSelectionBorder(player);
		}
	});
	drawBallBorder();
}

function drawPlayer(player) {
	console.log("drawing");
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
		  if(player.hasBall == false && !this.src.includes(player.imgUrl)){ // in case lost ball since request to draw made
			  drawPlayer(player);
			  return;
		  }
		  if(getPlayerById(player.id) == null){ // in case taken off pitch in meantime
			  return;
		  }
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
				console.log("player displayed");
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
	if(ballLocation != null && getPlayerWithBall() == null){
		console.log("drawing ball");
	  var img = new Image();
	  img.src = "/images/ball.png";
	  img.onload = function() {
		  if(ballLocation == null){
			  console.log("where's the ball?");
			  return;
		  }
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
			context.restore();
			}
  }
}

function drawBallBorder(){
	if(ballLocation != null && getPlayerWithBall() == null){
	  var column = ballLocation[0];
	  var row = 14 -ballLocation[1];
	  var squareH = canvas.height / 15;
	  var ctx = selection.getContext("2d");
      ctx.save();
	  ctx.strokeStyle = "orange";
	  ctx.lineWidth = 8;
	  ctx.strokeRect(column * squareH, row * squareH, squareH,
			squareH);
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
		if(message.action == "ACTIONS"){
			showPossibleActions(message);
		}
		else if(message.action == "MOVEMENT"){
			showMovement(message);
		} else if(message.action == "ROUTE"){
			showRoute(message);
		} else if(message.action == "THROWRANGES"){
			showThrowRanges(message);
		} else if(message.action == "THROW"){
		    showThrowDetails(message);
		} else if(message.action == "HANDOFF"){
			showHandOffDetails(message);
		} else if(message.action == "BLOCK"){
			showBlock(message, false);
	    } else if(message.action == "BLITZ"){
		    showRoute(message);
		    showBlock(message, true);
	    }else if(message.action == "BLOCKDICECHOICE"){
	    	if(animating == true || turnover == true){
				  var task = function(m){
					  showBlockDiceChoice(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
		    	  showBlockDiceChoice(message); 
		      }
	    }else if(message.action == "REROLLCHOICE"){
			showRerollUsed(message);
		} else if(message.action == "BLOCKOVER"){
			showBlockEnd(message);
		}else if(message.action == "SKILLUSED"){
			console.log("skill used");
			if(animating == true){
				  var task = function(m){
					  console.log("showing skill use in task");
						showSkillUsed(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 
		    	  console.log("showing skill use immediately");
		    		showSkillUsed(message);
		      }
		}else{ if(message.action == "ARMOURROLL"){
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
				console.log("saving turnover");
				  var task = function(m){
					  showTurnover(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
		    	  console.log("going straight to turnover");
		    	  showTurnover(message); 
		      }
		} else if(message.action == "NEWTURN"){
			if(animating == true || turnover == true){
				  var task = function(m){
					  showNewTurn(message);
		    	  };
		    	  var t = animateWrapFunction(task, this, [message]);
		    	  taskQueue.push(t);
		      } else{ 	
		    	  message.alert = true;
		    	  showNewTurn(message); 
		      }
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
		} else if(message.action == "TEAMBLITZED"){
			showBlitzUsed(message);
		} 
		}
	} else if(message.type == "ACTION"){
		if(message.action == "SETUPUPDATE"){
			updateSetup(message);
		}
		if(actionChoice != "blitz"){
			actionChoice = null;
		}
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
	  } else if(message.action == "STANDUP"){
		 showStandUp(message); 
	  }  else if(message.action == "ROLL"){
		  activePlayer = getPlayerById(message.player);
		  if(animating == true){
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
	  } else if(message.action == "BLOCK"){
		  if(animating == true){
			  var task = function(m){
				  showBlockResult(message);
	    	  };
	    	  var t = animateWrapFunction(task, this, [message]);
	    	  taskQueue.push(t);
	      } else{ 	
	    	  showBlockResult(message);
	      }
	  }	else if(message.action == "BLOCKDICECHOICE"){
		  if(animating == true){
			  var task = function(m){
				  requestBlockDiceChoice(message);
	    	  };
	    	  var t = animateWrapFunction(task, this, [message]);
	    	  taskQueue.push(t);
	      } else{ 	
	    	  requestBlockDiceChoice(message);
	      }
	  }  else if(message.action == "PUSHCHOICE"){
		  requestPushChoice(message);
	  }  else if(message.action == "PUSHRESULT"){
		  if(animating == true){
			  var task = function(m){
	    		  showPushResult(m);
	    	  };
	    	  var t = animateWrapFunction(task, this, [message]);
	    	  taskQueue.push(t);
	      } else{ 	
	    	  showPushResult(message); 
	      }
	  } else if(message.action == "HANDOFF"){
		  showHandOff(message);
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
	if(phase == "yourSetup"){
		actOnsetupClick(click);
		return;
	}
	if(inPush == true){
		console.log("in push");
		validatePush(click);
	}
	if(inModal == true || inBlock == true){
		console.log("In modal/ block");
		return;
	}
	var square = determineSquare(click);
	console.log(square);
    if(actionChoice == "throw" && !(square[0] == activePlayer.location[0] && square[1] == activePlayer.location[1])){
    	stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
                JSON.stringify({"type": "INFO", "action": "THROW", "player": activePlayer.id,
	                 "location": activePlayer.location, "target": square}));
    	return;
	} else if(actionChoice == "throw" && "throw" && square[0] == activePlayer.location[0] && square[1] == activePlayer.location[1]){
		 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
                 JSON.stringify({"type": "INFO", "action": "ACTIONS", "player": activePlayer.id}));
	} 
	for(var i = 0 ; i < players.length; i++){
		var player = players[i];
		 // console.log("checking");
		 if(player.location[0] == square[0] && player.location[1] == square[1]) {
			 taskQueue.length = 0;
			 console.log(player.name);
			 console.log(player.id);
			    
			    if(activePlayer!= null & activePlayer != player && actionChoice != null && actionChoice == "handOff" && Math.abs(activePlayer.location[0] - player.location[0]) <=1 && Math.abs(activePlayer.location[1] - player.location[1]) <=1){
							stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
					                 JSON.stringify({"type": "INFO", "action": "HANDOFF", "player": activePlayer.id,
						                 "location": activePlayer.location, "target": square, "opponent": player.id}));
				}
				if(activePlayer != null && activePlayer.team == team && player.team != team && yourTurn == true){
					showPlayerOnPitch(player);
					if(Math.abs(activePlayer.location[0] - player.location[0]) <=1 && Math.abs(activePlayer.location[1] - player.location[1]) <=1) { 
						if(actionChoice != null && actionChoice == "block"){
		
						stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
				                 JSON.stringify({"type": "INFO", "action": "BLOCK", "player": activePlayer.id,
					                 "location": activePlayer.location, "opponent": player.id}));
						return;
						} 
						
					} else if (actionChoice != null && actionChoice == "blitz"){
						  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
				                 JSON.stringify({"type": "INFO", "action": "BLITZ", "player": activePlayer.id,
					                 "location": activePlayer.location, "opponent": player.id, 
					                 "target": square, "waypoints": waypoints}));
						  return;
					}
				
				}
				if(player.team != team && actionChoice == null|| player.team == team && yourTurn == false){
					showPlayerOnPitch(player);
					closeActions();
					stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
						                 JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": player.id,
						                 "location": player.location, "routeMACost": 0}));
					return;
				} else if(actionChoice == null || player.team == team && (actionChoice == "move" || actionChoice == "blitz" || actionChoice == "block")){
			     var pTemp = activePlayer;
			     activePlayer = player;
			     drawPlayerBorders();
			     drawSelectionBorder(activePlayer);
			     inRoute = false;
			     waypoints.length = 0;
			     route.length = 0;
			     showPlayerOnPitch(player);  
			     if(player.team == team){
			       stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
			                 JSON.stringify({"type": "INFO", "action": "ACTIONS", "player": player.id}));
			     }
			     lastSquareClicked = square;
			     // console.log(player);
			    // stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
			      //           JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": player.id,
			        //         "location": player.location, "routeMACost": 0}));
			 }
		    return;
		 }
		 } 
	if(activePlayer != null && activePlayer.team == team && yourTurn == true && actionChoice != null && actionChoice != "throw" && actionChoice != "handOff"){ 
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
		  if(type == "dodge"){
				speed = 5;
		   }
		  xIncrement = (targetX - startingX) / speed;
	      yIncrement = (targetY - startingY) / speed;
	      if(type === "tripped"){
	    	player.status = "prone";
			console.log("tripping time");
			speed = 5; // lower is faster
		  }
		  context.clearRect(startingX, startingY, squareH, squareH);
		  context.clearRect(targetX, targetY, squareH, squareH);
		  drawPlayerBorders();
		  drawBall();
		  animateMovement(message.route, 0, img, startingX, startingY, targetX, targetY, squareH, end, type); 
		  player.location = route[route.length-1].position;
	      waypoints.length = 0;
	      inRoute = false;
		}
	    // context.restore();
	}
}

function animateMovement(route, counter, img, startingX, startingY, targetX, targetY, squareH, end, type){
	animationContext = animation.getContext("2d");
	animationContext.clearRect(startingX, startingY, squareH, squareH);
	drawPlayerBorders();
	var newX = startingX + xIncrement;
	var newY = startingY + yIncrement;
	if(type == "BALL"){
		animationContext.drawImage(img, newX, newY, squareH/1.5, squareH/1.5);	
	} else{
	animationContext.drawImage(img, newX, newY, squareH, squareH);
	}
	if(Math.round(newX) == Math.round(targetX) && Math.round(newY) == Math.round(targetY)){
		console.log("finished route");
		if(counter == route.length-1){
			route.length = 0;
			animation.getContext("2d").clearRect(0, 0, animation.height, animation.width);
			if(type == "BALL"){
				drawBall();
			}else if(type == "PUSH"){
				drawPlayers();
				drawBall();
				animation.getContext("2d").clearRect(0, 0, animation.height, animation.width);
			} else if(end == "Y" || activePlayer.status == "prone"){
		        drawPlayer(activePlayer);
		        animation.getContext("2d").clearRect(0, 0, animation.height, animation.width);
		      if(activePlayer.team == team && end == "Y"){
			      stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
				         JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": activePlayer.id,
				                         "location": activePlayer.location, "routeMACost": 0}));
			      actionChoice = "move";
			  }
		    } 
			console.log("tasks in queue: " + taskQueue.length);
			var timeout = 100;
			if(type == "BALL"){ 
				timeout = 100;
			}
			animating = false;
			 setTimeout(function(){	   
				   if(taskQueue.length != 0){
				   (taskQueue.shift())();
				   }
				   }, timeout);
			return;
		}
		counter++;
	    targetX = route[counter].position[0] * squareH;
	    targetY = (14 - route[counter].position[1]) * squareH;
		xIncrement = (targetX - newX) / 10;
	    yIncrement = (targetY - newY) / 10;
	}
	requestAnimationFrame(function() { animateMovement(route, counter, img, newX, newY, targetX, targetY, squareH, end, type); });	
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
	if(activePlayer.movement.length>0 && (actionChoice != "blitz"|| actionChoice != "block")){
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
		if(!(message.rollType == "INTERCEPT" && (message.rerollOptions == null || messsage.rerollOptions.length > 0))){
		  var task = function(m){
		    showFailedAction(m);
  	      };
  	      var t = animateWrapFunction(task, this, [message]);
  	      taskQueue.unshift(t);
		}
	}
	squares.getContext("2d").clearRect(0, 0, squares.width, squares.height);
	var newRolls = document.getElementById("newRolls");
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
	if(message.rollType == "THROW"){
		if(message.rollOutcome == "failed"){
		  (taskQueue.shift())();
		   return;
		}
		showThrowResult(message);
	}
	if(message.rollType == "INTERCEPT"){
		if(message.rollOutcome == "failed"){
		     if(taskQueue.length != 0){
		       (taskQueue.shift())();
		      }
		}else{
			showIntercept(message);
		}
	}
	if(message.rollType == "CATCH"){
		if(message.rollOutcome == "failed"){
			  (taskQueue.shift())();
			   return;
			}
			showCatchResult(message);
	}
	if(message.rollOutcome == "success"){
		inModal = false;
		modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
		document.getElementById("modal").style.display = "none";
	}
}

function resizeActions(){
	console.log("resize");
	rolls.style.paddingTop = "" + canvas.clientHeight + "px";
}

function showDodgeResult(message){
	console.log("Last Roll Location: " + lastRollLocation);
	console.log("Message: " + message.location + " " + message.target);
	console.log("Showing dodge result");
	var type = "dodge";
	if(message.rollOutcome === "failed"){
		type = "tripped";
		 activePlayer.status = "prone";
	} else{
		activePlayer.status = "standing";
	}
	if(message.reroll == false && lastRollLocation != null  && (lastRollLocation[0][0] == message.location[0] && lastRollLocation[0][1] == message.location[1] &&
			lastRollLocation[1][0] == message.target[0] && lastRollLocation[1][1] == message.target[1])){
		console.log("same as last location");
		if(message.end == "Y"){
			if(taskQueue.length == 0){
				drawPlayer(getPlayerById(message.player));
			}
			if(message.rollOutcome == "success"){
			  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	          JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": message.player,
	          "location": message.target, "routeMACost": 0}));
			}
		}
		if(taskQueue.length > 0){
			console.log("continue?");
		  (taskQueue.shift())();
		}	
	} else {
	  lastRollLocation = [message.location, message.target];
	  message.route = [{position: message.location}, {position: message.target}];
	  showMoved(message, type);	
	}
}

function showGFIResult(message){
	console.log("Last Roll Location: " + lastRollLocation);
	console.log("Message: " + message.location + " " + message.target);
	console.log("Showing GFI result");
	var type = "normal";
	if(message.rollOutcome === "failed"){
	  type = "tripped";
	  activePlayer.status = "prone";
	} else{
		activePlayer.status = "standing";
	}
	if(message.reroll == false && lastRollLocation != null  && (lastRollLocation[0][0] == message.location[0] && lastRollLocation[0][1] == message.location[1] &&
			lastRollLocation[1][0] == message.target[0] && lastRollLocation[1][1] == message.target[1])){
		console.log("same as last location");
		if(message.end == "Y"){
			if(taskQueue.length == 0){
				drawPlayer(getPlayerById(message.player));
			}
			if(message.rollOutcome == "success"){
			  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	          JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": message.player,
	          "location": message.target, "routeMACost": 0}));
			}
		}
		if(taskQueue.length > 0){
		  (taskQueue.shift())();
		}	
	} else {
	  lastRollLocation = [message.location, message.target];
	  message.route = [{position: message.location}, {position: message.target}];
	  showMoved(message, type);
	}
}

function showPickUpResult(message){
	//animating = true;
	inPickup = true;
	if(!(lastRollLocation != null  && (lastRollLocation[0][0] == message.location[0] && lastRollLocation[0][1] == message.location[1] &&
			lastRollLocation[1][0] == message.target[0] && lastRollLocation[1][1] == message.target[1]))){ 
	    message.route = [{position: message.location}, {position: message.target}];
	    showMoved(message, "normal");
		lastRollLocation = [message.location, message.target];
	}
	 var p = getPlayerById(message.player);
	 p.location = message.target;
	 drawBall();
	 if(message.rollOutcome == "success"){
		 p.hasBall = true;
		 ballLocation = null;
	 }
	 if(message.end == "Y"){
			if(taskQueue.length == 0 && message.rollOutcome != "failed"){
                 drawPlayer(getPlayerById(message.player));
                 drawBall();
			}
			if(message.rollOutcome == "success"){
			  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	          JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": message.player,
	          "location": message.target, "routeMACost": 0}));
			   inPickup = false;
			}
	} if(taskQueue.length > 0 && (animating == false || message.rollOutcome == "failed")){
		  (taskQueue.shift())();
	}
	
}


function showBallScatter(message){
	console.log("scattering");
	animating = true;
	 // modal.style.display = "none";
	  // document.getElementById("modal").style.display = "none";
	  newRolls.innerHTML =  "Ball scattered to " + message.target + "</br>" + newRolls.innerHTML;
	  document.getElementById("modalOptions").innerHTML = document.getElementById("modalOptions").innerHTML + "<p> Ball scattered to " + message.target + "</p>";
	  var ballImg = new Image();
	  ballImg.src = "/images/ball.png";
      ballImg.onload = function() { 
       var squareH = canvas.height / 15;
       var p = getPlayerWithBall();
       squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
       var startingX = message.location[0] * squareH + squareH/3;
       var startingY = (14 - message.location[1]) * squareH + squareH/3;
       context.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
       if(p != null){
    	 console.log(p.name);
    	 p.hasBall = false;
      }
      drawPlayers();
      var targetX = message.target[0] * squareH + squareH/3;
      var targetY = (14 - message.target[1]) * squareH + squareH/3;
      var speed = 25;
      xIncrement = (targetX - startingX) / speed;
      yIncrement = (targetY - startingY) / speed;
      var scatterRoute = [message.target];
      console.log("route created: " + scatterRoute);
      ballLocation = message.target;
      animationContext.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
      animateMovement(scatterRoute, 0, ballImg, startingX, startingY, targetX, targetY, squareH, "N", "BALL"); 
      //setTimeout(function(){
      // modal.style.display = "block";
      // modal.style.display = "none";
//        if(taskQueue.length != 0){
//        (taskQueue.shift())();
//        }
//        }, 3000);  
      }
    }
      

function showThrowResult(message){
	inThrow = true;
	if(message.rollOutcome == "intercepted"){
	  return;
	}
	animating = true;
	console.log("showing throw");
	var outcome = " threw the ball";
	if(message.rollOutcome == "success"){
		document.getElementById("modalTitle").innerHTML = "Accurate Throw";
		document.getElementById("modalText").innerHTML = message.playerName + " threw the ball to the target</br></br>" +
        "Needed: " + message.rollNeeded + "  Rolled: " + message.rolled;
		outcome += " accurately";
	} else if(message.rollOutcome == "badly"){
		outcome += " badly";
		document.getElementById("modalText").innerHTML = message.playerName + " threw the ball inaccurately</br></br>"
		document.getElementById("modalTitle").innerHTML = "Bad Throw";
	}
	modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	document.getElementById("modalOptions").innerHTML = "<p>" + message.playerName + outcome + "." + "</p>";

	 var ballImg = new Image();
	  ballImg.src = "/images/ball.png";
     ballImg.onload = function() { 
       var squareH = canvas.height / 15;
       var p = getPlayerById(message.player);
       animation.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
       var startingX = message.location[0] * squareH + squareH/3;
       var startingY = (14 - message.location[1]) * squareH + squareH/3;
       context.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
       if(p != null){
   	   console.log(p.name);
   	   p.hasBall = false;
       }
       drawPlayers();
       var targetX = message.target[0] * squareH + squareH/3;
       var targetY = (14 - message.target[1]) * squareH + squareH/3;
       var speed = 25;
       xIncrement = (targetX - startingX) / speed;
       yIncrement = (targetY - startingY) / speed;
       var throwRoute = [message.target];
     console.log("route created: " + throwRoute);
     ballLocation = message.target;
     if(message.rollOutcome == "success" && message.end == "Y"){
    	 document.getElementById("modalOptions").innerHTML = "";
    	 document.getElementById("closeModal").style.display = "block";
     }
     animationContext.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
     animateMovement(throwRoute, 0, ballImg, startingX, startingY, targetX, targetY, squareH, "N", "BALL"); 
     
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
	inModal = true;
	console.log("showingFailed");
	var shadow = modal.getContext("2d");
   // shadow.clearRect(0, 0, shadow.width, shadow.height);
    shadow.globalAlpha = 0.4;
    shadow.fillStyle = "black";
    shadow.fillRect(0,0, modal.width, modal.height);
    animationContext.clearRect(0,0, animation.width, animation.height);
    var player = getPlayerById(message.player); 
    var column = message.target[0];
	var row = 14 - message.target[1];
	if(message.rollType == "THROW"){
		column = message.location[0];
		row = 14 - message.location[1];
	}
	var squareH = canvas.height / 15;
	shadow.clearRect(column * squareH-5, row * squareH-5, squareH+10,squareH+10);
	var display = document.getElementById("modal");
	display.style.display = "block";
	squareH = canvas.clientHeight/15;
	display.style.left = ""+ (column +3) * squareH-5 + "px";
	display.style.top = "" + (row) * squareH-5 + "px";
	var effect = " fell down.";
	if(message.rollType == "PICKUPBALL" || message.rollType == "CATCH"){
		effect = " dropped the ball";
	}
	if(message.rollType == "THROW"){
		effect = " fumbled the throw";
	}
	if(message.rollType == "INTERCEPT"){
	    effect = " failed to intercept";	
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
	drawPlayer(player);

	 setTimeout(function(){
		     document.getElementById("modal").style.display = "block";
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
    if(inPickUp == false){
      p.location = message.location;
      drawPlayers();
      drawBall();
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
	var chooser = message.teamName;
	if(message.userToChoose == team){
		chooser = "You"
	}
	document.getElementById("modalOptions").innerHTML = "<p>" + chooser + choice + "</p>";
    if(message.rerollChoice == "Team Reroll"){
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
	if(message.rerollChoice != "Don't reroll"){
		 inModal == false;
	}
	setTimeout(function(){
		 console.log("task queue: " + taskQueue.length);
		 if(taskQueue.length != 0){
		    (taskQueue.shift())();
		  }
		}, 800);	
 }

function showBlockSkill(message){
	squares.getContext("2d").clearRect(0, 0, squares.width, squares.height);
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + " used the block skill and remained in place." + "</br>" + newRolls.innerHTML;
	document.getElementById("modalOptions").innerHTML += "<p>" + message.playerName + " used the block skill and remained in place. </p>";
	if(taskQueue.length != 0){
    	(taskQueue.shift())();
    }
}

function showArmourRoll(message){
	console.log("showing armour");
	getPlayerById(message.player).hasBall = false;
	console.log("has ball? " + getPlayerById(message.player).hasBall);
	getPlayerById(message.player).status = "prone";
	drawPlayer(getPlayerById(message.player));
	squares.getContext("2d").clearRect(0, 0, squares.width, squares.height);
	document.getElementById("modal").style.display = "block";
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + "'s "+ message.rollOutcome + ". Armour: "  + message.rollNeeded + " Rolled: " +
	                      message.rolled + "</br>" + newRolls.innerHTML;
	if(inBlock == false){
		document.getElementById("modalOptions").innerHTML = "";
	}
	document.getElementById("modalOptions").innerHTML += "<p>" + message.playerName + "'s " + message.rollOutcome + "." + "</p>";
	document.getElementById("animationCanvas").getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	if(taskQueue.length != 0){
    	(taskQueue.shift())();
    }
}

function showInjuryRoll(message){
	console.log("showing Injury");
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + " was "+ message.rollOutcome + ". " + "Rolled: " + message.rolled + "</br>" + newRolls.innerHTML;
	// var existing = document.getElementById("modalOptions").innerHTML;
	document.getElementById("modalOptions").innerHTML += "<p>" + message.playerName + " is " + message.rollOutcome + "." + "</p>";
	var player = getPlayerById(message.player);
    player.status = message.playerStatus;
    player.hasBall = false;
    if(message.location == null){
    	removePlayer(player);
    	var squareH = canvas.height / 15;
    	context.clearRect(player.location[0] * squareH-5, (14 - player.location[1]) * squareH-5, squareH+10,squareH+10);
    	drawPlayerBorders();
    } else{
        player.location = message.location;
    	drawPlayer(player);
    }
    document.getElementById("animationCanvas").getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
    console.log("injury tasks left: " + taskQueue.length);
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
	document.getElementById("closeModal").style.display = "block";
	setTimeout(function(){
	  if(taskQueue.length != 0){
	    (taskQueue.shift())();
	  }
	}, 500);  
}

function showNewTurn(message){
	turnover = false;
	inModal = false;
	inBlock = false;
	inPickup = false;
	closePlayer1();
	closePlayer2();
	document.getElementById("team1Blitzed").innerHTML = "Not Blitzed This Turn";
	document.getElementById("team2Blitzed").innerHTML = "Not Blitzed This Turn";
	animation.getContext("2d").clearRect(0,0, animation.width, animation.height);
	ballLocation = message.ballLocation;
	modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	selection.getContext("2d").clearRect(0, 0, selection.width, selection.height);
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
	if(team1Reserves == null || message.team1FullDetails.reserves.length != team1Reserves.length){
		team1Reserves = message.team1FullDetails.reserves;
		 populateReserves(1);
	}
	if(team2Reserves == null || message.team2FullDetails.reserves.length != team2Reserves.length){
		team2Reserves = message.team2FullDetails.reserves;
		populateReserves(2);
	}
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
	if(message.alert == true){
		alert(teamName);
	}
	document.getElementById("score").innerHTML = ""+ message.team1Score + " - " + message.team2Score;
	document.getElementById("team2Turn").innerHTML = "Current Turn: " + team2.turn;
	document.getElementById("team1Turn").innerHTML = "Current Turn: " + team1.turn;
	document.getElementById('team1Rerolls').innerHTML = "Team Rerolls: " + team1.remainingTeamRerolls;
	document.getElementById('team2Rerolls').innerHTML = "Team Rerolls: " + team2.remainingTeamRerolls;
	drawPlayers();
    drawBall();
}

function endTurn(){
	if(yourTurn == true && inBlock == false && inModal == false){
	  var result = confirm("Are you sure you want to end your turn?");
		if (result == true) {
		  stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
					    JSON.stringify({"type": "ACTION", action: "ENDTURN"}));
		  squares.getContext("2d").clearRect(0, 0, squares.width, squares.height);
		  closeActions();
		} else {
		  return;
		}
	} else{
		if(inBlock == true || inModal == true){
			alert("In an action");
		} else{
		alert("Not your turn");
		}
	}
}

function showTouchdown(message){
	document.getElementById("score").innerHTML = ""+ message.team1Score + " - " + message.team2Score;
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + " scored a touchdown for Team " + message.teamName + "!</br>" + newRolls.innerHTML;
	alert(message.playerName + " scored a touchdown for Team " + message.teamName + "!");
}

function showBlock(message, blitz){
	 document.getElementById("modalText").innerHTML = "";
	 inBlock = true;
	 showBlockAssists(message);
	 if(blitz == true){
		 document.getElementById("modalTitle").innerHTML = "Blitz Details"; 
	 } else {
     document.getElementById("modalTitle").innerHTML = "Block Details";
	 }
 	 var modalMain = document.getElementById("modalImages");
 	 modalMain.innerHTML = ""; 
 	 var blankDice = new Image();
 	 blankDice.src = "/images/blank_dice.png";
 	 for(i = 0; i<message.numberOfDice; i++){
 		 modalMain.innerHTML += "<img height='50px' class ='dice' src=" + blankDice.src + "/>"; 
 	 }
 	 var toChoose;
 	 var style = "black"
 	 if(message.userToChoose == team){
 		 toChoose = "You choose 1 outcome dice.";
 		 var style = "red";
 	 } else{
 		 toChoose = "Your opponent chooses 1 outcome dice.";
 	 }
 	 document.getElementById("modalOptions").innerHTML = "<p style='font-color:" + style +"'>"+ toChoose +"</p>Follow Up? "; 
 	 var follow = document.createElement("input");
 	 follow.type = "checkbox";
 	 follow.id = "follow";
 	  modalOptions.appendChild(follow);
 	 document.getElementById("modalOptions").innerHTML += "<br><hr>";
 	 var button = document.createElement("BUTTON")
     button.innerHTML = "Cancel";
     button.onclick = function() {cancelBlock(message.player)};
     modalOptions.appendChild(button);
     var button2 = document.createElement("BUTTON")
     button2.innerHTML = "Block";
     if(blitz == true){
    	 button2.innerHTML = "Blitz";
    	 button2.onclick = function() {
        	 followUp = document.getElementById("follow").checked;
        	 sendCarryOutBlitz(message, followUp, route);
        	 document.getElementById("modal").style.display = "none"; 
        	 modalMain.innerHTML = "";
         }; 
     } else{
       button2.onclick = function() {
    	 followUp = document.getElementById("follow").checked;
    	 sendCarryOutBlock(message, followUp)
       };
     }
     modalOptions.appendChild(button2);
 	 squareH = modal.clientHeight/15;
     var display = document.getElementById("modal");
 	 display.style.display = "block";
 	 display.style.left = ""+ (message.location[0] +3) * squareH-5 + "px";
 	 display.style.top = "" + ((14- message.location[1])-5) * squareH-5 + "px";
}

function showBlockAssists(message){
	 var sContext = squares.getContext("2d");
	 sContext.clearRect(0, 0, squares.width, squares.height);
	 sContext.save();
	 var squareH = canvas.height / 15;
	 sContext.globalAlpha = 0.3;
	 sContext.fillStyle = "white";
    sContext.fillRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
    sContext.fillStyle = "red";
    sContext.fillRect(message.target[0] * squareH, (14 - message.target[1]) * squareH, squareH, squareH);
    sContext.fillStyle = "blue";
    message.attAssists.forEach(function(location){
   	 sContext.fillRect(location[0] * squareH, (14 - location[1]) * squareH, squareH, squareH);
    });
    sContext.fillStyle = "orange";
    message.defAssists.forEach(function(location){
   	 sContext.fillRect(location[0] * squareH, (14 - location[1]) * squareH, squareH, squareH);
    });
}

function sendCarryOutBlock(message, follow){
	stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
            JSON.stringify({"type": "ACTION", "action": "BLOCK", "player": message.player,
                "location": message.location, "opponent": message.opponent, "followUp": follow}));
}

function sendCarryOutBlitz(message, follow, route){
	var messageRoute = new Array();
	route.forEach(tile => {
		var t = tile.position;
		messageRoute.push(t);
	});
	stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
            JSON.stringify({"type": "ACTION", "action": "BLITZ", "player": message.player,
                "location": message.location, "opponent": message.opponent, "followUp": follow, 
                "route": messageRoute}));
}

function showBlockResult(message){
	inBlock = true;
	drawPlayer(getPlayerById(message.player));
	showBlockAssists(message);
	document.getElementById("modalTitle").innerHTML = message.playerName + " blocks " + message.opponentName;
	document.getElementById("modalText").innerHTML = "";
	document.getElementById("modalOptions").innerHTML = "";
	var modalMain = document.getElementById("modalImages");
	modalMain.innerHTML = "Results: <br><br>"; 
	var rollText = "";
	for(i = 0; i<message.rolled.length; i++){
      var dice = new Image();
	  dice.src = diceImages[message.rolled[i] -1];
	  modalMain.innerHTML += "<img height='50px' class ='dice' src='" + dice.src + "' title = '" + blockResults[message.rolled[i] -1] + "'/>"; 
	  rollText += " " + blockResults[message.rolled[i] -1];
	}
	newRolls.innerHTML =  message.playerName + " blocked " + message.opponentName + ". Rolled: " + rollText + ".</br>" + newRolls.innerHTML;
	if(message.rerollOptions == null || message.rerollOptions.length == 0){
		  document.getElementById("modalOptions").innerHTML = "<p> No possible rerolls </p>";
	} else if(message.userToChoose != team){
		   document.getElementById("modalOptions").innerHTML = "<p> Awaiting opponent reroll decision </p>" +
		                          "Possible rerolls: " + message.rerollOptions;
	} else{
			rerollRoute = [{position: message.location}, {position: message.target}]; 
			requestReroll(message.rerollOptions);
	}
	var display = document.getElementById("modal");
	squareH = modal.clientHeight/15;
 	display.style.display = "block";
 	display.style.left = ""+ (message.location[0] +3) * squareH-5 + "px";
 	display.style.top = "" + ((14- message.location[1])-5) * squareH-5 + "px";
 	if(taskQueue.length >0){
      (taskQueue.shift())();
 	}
}

function requestBlockDiceChoice(message){
	var dice = document.getElementsByClassName("dice");
	for(i = 0 ; i < dice.length; i++){
		dice[i].style.cursor = "pointer";
		dice[i].id = "" + i;
		dice[i].addEventListener('click', (e) => {
			 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
					    JSON.stringify({"type": "ACTION", action: "BLOCKDICECHOICE", "diceChoice": event.srcElement.id, 
					    	"player": message.player, "followUp": followUp, "opponent": message.opponent}));
	    	});
	}
    document.getElementById("modalOptions").innerHTML = "<p> Please select a dice.</p>";    
}

function showBlockDiceChoice(message){
	var modalMain = document.getElementById("modalImages");
	var chooser = "You chose:";
	if(message.userToChoose != team){
		chooser = message.teamName + " chose:"
	}
	modalMain.innerHTML = chooser + "<br><br>"; 
	var dice = new Image();
	dice.src = diceImages[message.diceChoice -1];
	modalMain.innerHTML += "<img height='50px' class ='dice' src='" + dice.src + "' title = '" + blockResults[message.diceChoice -1] + "'/>"; 
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.teamName + " chose " + blockResults[message.diceChoice -1]+ "</br>" + newRolls.innerHTML;
	document.getElementById("modalOptions").innerHTML = "";
}

function cancelBlock(player){
	document.getElementById("modal").style.display = "none";
	document.getElementById("modalImages").innerHTML = "";
	inModal = false;
	inBlock = false;
	if(actionChoice != "block"){
	  resetMovement();
	}
	stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
             JSON.stringify({"type": "INFO", "action": "ACTIONS", "player": activePlayer.id}));
	
}

function showSkillUsed(message){
	if(message.description == "Block"){
		showBlockSkill(message);
	} else if(message.description == "Side Step"){
		showSideStepSkill(message);
	} else if(message.description == "Dodge In Block"){
		showDodgeInBlock(message);
	} else if(message.description == "Catch"){
	    showCatchSkill(message);
	}
}


function showBlockEnd(message){
	//document.getElementById("modalImages").innerHTML = "";
	document.getElementById("closeModal").style.display = "block";
	//document.getElementById("modalText").innerHTML = "";
	inModal = false;
	inPush = false;
	followUp = false;
	drawPlayers();
	drawPlayerBorders();
	drawBall();
	if(message.description == "BLITZ"){
		var player = getPlayerById(message.player);
		stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
                JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": player.id,
                "location": player.location, "routeMACost": 0}));
	} else{
		if(activePlayer.movement != null){
		  activePlayer.movement.length = 0;
		}
	}
}

function removePlayer(player){
	for(var i = 0; i < players.length; i++){ 
      if ( players[i].id == player.id) {
	    players.splice(i, 1); 
	  }
    }
}

function requestPushChoice(message){
	if(inPush == true){
		//document.getElementById("modalText").innerHTML = "";
	}
	console.log("In request push choice");
	var sContext = squares.getContext("2d");
	sContext.clearRect(0, 0, squares.width, squares.height);
	sContext.save();
	var squareH = canvas.height / 15;
	sContext.globalAlpha = 0.6;
    sContext.fillStyle = "red";
    sContext.fillRect(message.target[0] * squareH, (14 - message.target[1]) * squareH, squareH, squareH);
    sContext.fillStyle = "white";
    message.squares.forEach(function(square){
  	  sContext.fillRect(square.position[0] * squareH, (14 - square.position[1]) * squareH, squareH, squareH);
    });
    pushOptions = message.squares;
    if(message.userToChoose == team){
       inPush = true;
      document.getElementById("modalOptions").innerHTML = "Please select where to push";
    } else{
    	 document.getElementById("modalOptions").innerHTML = "Awaiting opponent's push choice";
    }
    sContext.restore();
}

function validatePush(click){
	var square = determineSquare(click);
	for(var i = 0 ; i < pushOptions.length; i++){
		if(square[0] == pushOptions[i].position[0] && square[1] == pushOptions[i].position[1]){
			 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	                 JSON.stringify({"type": "ACTION", "action": "PUSHCHOICE", "target": square}));
		}
	}
}

function showPushResult(message){
	message.end = "N";
	squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	getPlayerById(message.player).location = message.target;
	var newRolls = document.getElementById("newRolls");
	var modalText = document.getElementById("modalOptions");
	message.route = [{position: message.location}, {position: message.target}];
	if(message.description == "OFFPITCH"){
	  modalText.innerHTML = message.playerName + " was pushed off pitch and beaten by the crowd! </br>";
	  newRolls.innerHTML =  message.playerName + " was pushed off pitch and beaten by the crowd! </br>" + newRolls.innerHTML;
	  removePlayer(getPlayerById(message.player));
	  drawPlayers();
	  drawBall();
	  setTimeout(function(){
	    if(taskQueue.length != 0){
		  (taskQueue.shift())();
		}
	  }, 2000);  
	  return;
	}
	if(message.description == "PUSH"){
	  modalText.innerHTML = message.playerName + " was pushed to " + message.target + "</br>";
	  newRolls.innerHTML =  message.playerName + " was pushed to " + message.target + "</br>" + newRolls.innerHTML;
	} else if (message.description == "FOLLOW"){
	  modalText.innerHTML += message.playerName + " followed up to " + message.target;
	  newRolls.innerHTML =  message.playerName + " followed up to " + message.target + "</br>" + newRolls.innerHTML;	
	}
	  showMoved(message, "PUSH");
}

function showSideStepSkill(message){
	var newRolls = document.getElementById("newRolls");
	var modalText = document.getElementById("modalText");
	var chooser = "your opponent chooses "
	if(message.userToChoose == team){
		chooser = "you choose ";
	}
	modalText.innerHTML = "<br>" +message.playerName + " used the Side Step skill, so " + chooser +  "push direction<br>";
    newRolls.innerHTML =  message.playerName + " used the Side Step skill, so " + chooser +  "push direction<br>" + newRolls.innerHTML;
}

function showDodgeInBlock(message){
	var newRolls = document.getElementById("newRolls");
	var modalText = document.getElementById("modalText");
	modalText.innerHTML = "<br>" +message.playerName + " used the Dodge Skill, so is just pushed back<br>";
    newRolls.innerHTML =  message.playerName + " used the Dodge Skill, so is just pushed back<br>" + newRolls.innerHTML;
}

function showCatchSkill(message){
	var newRolls = document.getElementById("newRolls");
	var modalText = document.getElementById("modalText");
	modalText.innerHTML = "<br>" +message.playerName + " used the Catch Skill<br>";
    newRolls.innerHTML =  message.playerName + " used the Catch Skill to reroll<br>" + newRolls.innerHTML;
    if(inThrow == true){
    	if(taskQueue.length >0){
    		(taskQueue.shift())();
    	}
    }
}

function removeBallFromPlayer(){
	console.log("go away ball");
	players.forEach(player => {
		if(player.hasBall == true){
			console.log("taking away ball");
		 player.hasBall = false;
		 return;
		}
	});
}

function getPlayerWithBall(){
	for(var i = 0; i < players.length; i++){
		if(players[i].hasBall == true){
			return players[i];
		}
	}
}

function findPlayerInSquare(square){
	for(var i = 0; i < players.length; i++){
	   if(players[i].location[0] == square[0] && players[i].location[1] == square[1]) {
		   return players[i];
	   }
    }
	return null;
}

function showBlitzUsed(message){
	if(message.userToChoose == team1.id){
		document.getElementById("team1Blitzed").innerHTML = "Has Blitzed This Turn";
	} else {
		document.getElementById("team2Blitzed").innerHTML = "Has Blitzed This Turn";
	}
}

function closeModal(){
	modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
	document.getElementById("modal").style.display = "none";
	document.getElementById("modalOptions").innerHTML = "";
	document.getElementById("modalText").innerHTML = "";
	document.getElementById("modalOptions").innerHTML = "";
	document.getElementById("closeModal").style.display = "none";
	inBlock = false;
	if(team == team1.id){
		closePlayer2();
	} else{
		closePlayer1();
	}
}

function closeActions(){
	var actions = document.getElementById("actions");
	actions.style.display = "none";
	 document.getElementById("actionsTitle").innerHTML = "Actions";
	resetActions();
	actionChoice = null;
	inThrow = false;
	if(phase == "yourSetup" && actionChoice == null){
		activePlayer = null;
		if(team == team1.id){
			closePlayer1();
		} else{
			closePlayer2();
		}
	}
}

function resetActions(){
	var actions = document.getElementsByClassName("actionImg");
	for(var i = 0 ; i < actions.length; i++){
		actions[i].style.display = "none";
	}
}

function showPossibleActions(message){
	 resetActions();
	 document.getElementById("squaresCanvas").getContext("2d").clearRect(0, 0, canvas.width, canvas.width);
	 console.log("show actions");
	 var actions = document.getElementById("actions");
	 canvas = document.getElementById("canvas");
	 actions.style.display = "block";
	 var squareH = canvas.clientHeight/15;
	 if(message.possibleActions[0] != "None"){
		document.getElementById("actionsTitle").innerHTML = "Actions";
	   for(var i = 0; i < message.possibleActions.length; i++){
		 document.getElementById(message.possibleActions[i]).style.display = "inline";
	   }
	 } else{
		 document.getElementById("actionsTitle").innerHTML = "No Possible Actions";
	 }
	 actions = document.getElementById("actions"); // have to get updated height & width based on number of images shown
	 actions.style.left = ""+ ((message.location[0] * squareH) - actions.offsetWidth/3)  + "px";
	 actions.style.top = "" + (((14- message.location[1]) * squareH) - actions.offsetHeight) + "px";
}

function requestMovement(){
	closeActions();
	actionChoice = "move";
	 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
               JSON.stringify({"type": "INFO", "action": "MOVEMENT", "player": activePlayer.id,
               "location": activePlayer.location, "routeMACost": 0}));
}

function startBlock(){
	closeActions();
	actionChoice = "block";
	var centre = activePlayer.location;
	canvas = document.getElementById("canvas");
	var squareH = canvas.clientHeight/15;
	console.log(centre);
	var column = centre[0] -1;
	var row = 14 - centre[1] - 1;
	var squareH = canvas.height / 15;
    var sContext = squares.getContext("2d");
    sContext.save();
    sContext.fillStyle = "white";
    sContext.globalAlpha = 0.3;
	sContext.fillRect(column * squareH, row * squareH, squareH *3,squareH * 3);
	sContext.restore();
}

function startBlitz(){
	requestMovement();
	actionChoice = "blitz";
}

function showThrowRanges(message){
	var location = message.location;
	var sContext = squares.getContext("2d");
	sContext.clearRect(0, 0, squares.width, squares.height);
	var squareH = canvas.height / 15;
	sContext.save();
	for(var i = 0 ; i < message.squares.length; i++){
		var type = message.squares[i].description;
		if(type == "quick pass"){
			sContext.fillStyle = "blue";
		} else if(type == "short pass"){
			sContext.fillStyle = "yellow";
		} else if(type == "long pass"){
			sContext.fillStyle = "orange";
		} else{
			sContext.fillStyle = "red";
		}
		sContext.globalAlpha = 0.4;
		sContext.fillRect(message.squares[i].position[0] * squareH + 3, (14 - message.squares[i].position[1]) * squareH + 3 , squareH-5, squareH-5);
	}
	context.restore();
}

function startThrow(){
	closeActions();
	actionChoice = "throw";
	 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
               JSON.stringify({"type": "INFO", "action": "THROWRANGES", "player": activePlayer.id,
               "location": activePlayer.location}));
}

function showThrowDetails(message){
	var sContext = squares.getContext("2d");
	sContext.clearRect(0, 0, squares.width, squares.height);
	sContext.save();
	sContext.fillStyle = "red";
	var squareH = canvas.height / 15;
	for(var i = 0; i < message.squares.length; i++){
		sContext.fillRect(message.squares[i].position[0] * squareH, (14 - message.squares[i].position[1]) * squareH, squareH, squareH);
	}
	sContext.fillStyle = "white";
	sContext.fillRect(message.target[0] * squareH, (14 - message.target[1]) * squareH, squareH, squareH);
	sContext.restore();
	var column = message.location[0];
	var row = 14 - message.location[1];
	var aContext = animation.getContext("2d");
	aContext.clearRect(0, 0, canvas.width, canvas.height);
	aContext.save();
	var distance = Math.floor(Math.sqrt(((message.location[0] - message.target[0]) * (message.location[0] - message.target[0]))
			+ ((message.location[1] - message.target[1]) * (message.location[1] - message.target[1]))));
	if(distance < 4){
		aContext.strokeStyle = "blue";
	} else if (distance < 7){
		aContext.strokeStyle = "yellow";
	} else if(distance < 11){
		aContext.strokeStyle = "orange";
	} else {
		aContext.strokeStyle = "red";
	}
	aContext.globalAlpha = 0.8;
	aContext.beginPath();
	aContext.moveTo((column +0.5) * squareH, (row +0.5) * squareH);
	aContext.lineTo((message.target[0]  + 0.5) * squareH, (14 - message.target[1] +0.5) * squareH);
	//sContext.lineTo((message.target[0] + 1) * squareH, (14 - message.target[1] + 1) * squareH);
	//sContext.lineTo((column + 1) * squareH, (row + 1) * squareH);
	aContext.closePath();
	aContext.lineWidth = 10;
	aContext.stroke();
	aContext.restore();
	showThrowModal(message);
}

function showThrowModal(message){
	var player = getPlayerById(message.player);
	document.getElementById("modalText").innerHTML = ""
	document.getElementById("modalOptions").innerHTML = "";
    document.getElementById("modalTitle").innerHTML = "Throw Details"; 
	 var modalMain = document.getElementById("modalImages");
	 modalMain.innerHTML = ""; 
	 document.getElementById("modalText").innerHTML = "<p>Throw Roll Needed: " + message.rollNeeded + "+</p>"; 
	 if(message.secondaryRollNeeded != 0){
		 document.getElementById("modalText").innerHTML += "<p>Catch Roll Needed: " + message.secondaryRollNeeded + "+</p>";
	 }
	 if(message.squares.length > 0){
		 var interceptors = document.getElementById("modalOptions");
		 interceptors.innerHTML = "<p style='color:red'>Possible Interceptors: </p>";
		 for(var i = 0; i < message.squares.length; i++){
			 interceptors.innerHTML += message.squares[i].description + ": " + message.squares[i].catchRoll + "+<br>";
		 }
		 interceptors.innerHTML += "<hr>";
	 }
	var button = document.createElement("BUTTON")
    button.innerHTML = "Cancel";
    button.onclick = function() {cancelThrow(message.player)};
    modalOptions.appendChild(button);
    var button2 = document.createElement("BUTTON")
    button2.innerHTML = "Throw";
     button2.onclick = function() {
   	   sendCarryOutThrow(message);
      };
    modalOptions.appendChild(button2);
	squareH = modal.clientHeight/15;
    var display = document.getElementById("modal");
	 display.style.display = "block";
	 display.style.left = ""+ (message.target[0] +3) * squareH-5 + "px";
	 display.style.top = "" + ((14- message.target[1])-5) * squareH-5 + "px";
}

function cancelThrow(player){
	inThrow = false;
	actionChoice = null;
	document.getElementById("modal").style.display = "none";
	document.getElementById("modalImages").innerHTML = "";
    animation.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
    squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
    stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
    JSON.stringify({"type": "INFO", "action": "ACTIONS", "player": player}));
}

function sendCarryOutThrow(message){
	 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
			    JSON.stringify({"type": "ACTION", "action": "THROW", "player": message.player,
			    	"location": message.location, "target": message.target}));
}

function showCatchResult(message){
	getPlayerById(message.player).hasBall = true;
	ballLocation = null;
	drawPlayers();
	 setTimeout(function(){	   
		if(taskQueue.length != 0){
		  (taskQueue.shift())();
		 }
	}, 300);
}

function showIntercept(message){
	document.getElementById("modalTitle").innerHTML = "Ball Intercept";
	document.getElementById("modalOptions").innerHTML = "<p>" + message.playerName + " intercepted the ball! </p>";
	animating = true;
	 var ballImg = new Image();
	  ballImg.src = "/images/ball.png";
     ballImg.onload = function() { 
       var squareH = canvas.height / 15;
       var p = getPlayerById(message.player);
       animation.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
       var startingX = message.location[0] * squareH + squareH/3;
       var startingY = (14 - message.location[1]) * squareH + squareH/3;
       context.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
       if(p != null){
   	   console.log(p.name);
   	   p.hasBall = false;
   	   drawPlayer(p);
       }
       drawPlayers();
       var targetX = message.target[0] * squareH + squareH/3;
       var targetY = (14 - message.target[1]) * squareH + squareH/3;
       var speed = 25;
       xIncrement = (targetX - startingX) / speed;
       yIncrement = (targetY - startingY) / speed;
       var throwRoute = [message.target];
     console.log("route created: " + throwRoute);
     ballLocation = message.target;
     animationContext.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
     animateMovement(throwRoute, 0, ballImg, startingX, startingY, targetX, targetY, squareH, "N", "BALL"); 
    }    
}


function startHandOff(){
    	closeActions();
    	actionChoice = "handOff";
    	var centre = activePlayer.location;
    	canvas = document.getElementById("canvas");
    	var squareH = canvas.clientHeight/15;
    	console.log(centre);
    	var column = centre[0] -1;
    	var row = 14 - centre[1] - 1;
        var squareH = canvas.height / 15;
    	var sContext = squares.getContext("2d");
    	sContext.save();
    	sContext.fillStyle = "white";
    	sContext.globalAlpha = 0.3;
        sContext.fillRect(column * squareH, row * squareH, squareH *3,squareH * 3);
        sContext.restore();
}

function showHandOffDetails(message){
		var player = getPlayerById(message.player);
		document.getElementById("modalText").innerHTML = ""
		document.getElementById("modalOptions").innerHTML = "";
	    document.getElementById("modalTitle").innerHTML = "Handoff Details"; 
		 var modalMain = document.getElementById("modalImages");
		 modalMain.innerHTML = ""; 
		 document.getElementById("modalText").innerHTML = "<p>Ball is passed automatically, no roll needed</p>"; 
	     document.getElementById("modalText").innerHTML += "<p>Catch Roll Needed: " + message.rollNeeded + "+</p>";
		var button = document.createElement("BUTTON")
	    button.innerHTML = "Cancel";
	    button.onclick = function() {cancelThrow(message.player)}; 
	    modalOptions.appendChild(button);
	    var button2 = document.createElement("BUTTON")
	    button2.innerHTML = "Hand Off";
	     button2.onclick = function() {
	   	   sendCarryOutHandOff(message);
	      };
	    modalOptions.appendChild(button2);
		squareH = modal.clientHeight/15;
	    var display = document.getElementById("modal");
		 display.style.display = "block";
		 display.style.left = ""+ (message.target[0] +3) * squareH-5 + "px";
		 display.style.top = "" + ((14- message.target[1])-5) * squareH-5 + "px";
}

function sendCarryOutHandOff(message){
		 stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
				    JSON.stringify({"type": "ACTION", "action": "HANDOFF", "player": message.player,
				    	"location": message.location, "target": message.target, "opponent": message.opponent}));
		 document.getElementById("modalOptions").innerHTML = "";
}

function showHandOff(message){
	inThrow = true;
	animating = true;
	console.log("showing throw");
	var outcome = " threw the ball";
	document.getElementById("modalText").innerHTML = message.playerName + " handed off the ball to " + message.opponentName +"</br></br>"
	document.getElementById("modalTitle").innerHTML = "Hand Off";
	modal.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
  document.getElementById("newRolls").innerHTML =  message.playerName + " handed off the ball to " + message.opponentName + "</br>" + newRolls.innerHTML;
	 var ballImg = new Image();
	  ballImg.src = "/images/ball.png";
     ballImg.onload = function() { 
       var squareH = canvas.height / 15;
       var p = getPlayerById(message.player);
       animation.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
       var startingX = message.location[0] * squareH + squareH/3;
       var startingY = (14 - message.location[1]) * squareH + squareH/3;
       context.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
       if(p != null){
   	   console.log(p.name);
   	   p.hasBall = false;
       }
       drawPlayers();
       var targetX = message.target[0] * squareH + squareH/3;
       var targetY = (14 - message.target[1]) * squareH + squareH/3;
       var speed = 25;
       xIncrement = (targetX - startingX) / speed;
       yIncrement = (targetY - startingY) / speed;
       var throwRoute = [message.target];
     console.log("route created: " + throwRoute);
     ballLocation = message.target;
     if(message.rollOutcome == "success" && message.end == "Y"){
    	 document.getElementById("modalOptions").innerHTML = "";
    	 document.getElementById("closeModal").style.display = "block";
     }
     animationContext.clearRect(message.location[0] * squareH, (14 - message.location[1]) * squareH, squareH, squareH);
     animateMovement(throwRoute, 0, ballImg, startingX, startingY, targetX, targetY, squareH, "N", "BALL"); 
     }
}

function requestStandUp(){
	closeActions();
	stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
            JSON.stringify({"type": "ACTION", "action": "STANDUP", "player": activePlayer.id}));
}

function showStandUp(message){
	var p = getPlayerById(message.player);
	p.status = "standing";
	var newRolls = document.getElementById("newRolls");
	newRolls.innerHTML =  message.playerName + " stood up </br>" + newRolls.innerHTML;
	if(message.end == "Y"){
		drawPlayer(p);
		stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
	             JSON.stringify({"type": "INFO", "action": "ACTIONS", "player": activePlayer.id}));
	}
}

function showReserves(team){
	var id = "team1Reserves";
	var title = "reserves1Title";
	if(team != 1){
		id = "team2Reserves";
		title = "reserves2Title"
	}
	var element = document.getElementById(id);
	if(element.style.display == "none"){
			element.style.display = "block";
			document.getElementById(title).innerHTML = "Reserves -";
	} else{
		element.style.display = "none";
		document.getElementById(title).innerHTML = "Reserves +";
	}
}

function populateReserves(team){
	var target;
	var details;
	if(team == 1){
		target = document.getElementById("team1Reserves");
		details = team1Reserves;
	} else{
		target = document.getElementById("team2Reserves");
		details = team2Reserves;
	}
	target.innerHTML = "";
	if(details != null && details.length >0){
	  for(var i = 0; i < details.length; i++){
		var img = document.createElement("img");
		img.src = details[i].imgUrl;
		img.height = "35";
		img.id =  team+"player"+details[i].id;
		img.className = "reserve";
		img.setAttribute('onclick','showReservePlayer(this)');
	    target.appendChild(img);
	  }
	} else{
		target.innerHTML += "<p>None</p>";
	}
}

function showPlayerOnPitch(player){
	showPlayerDetails(player);
}

function showReservePlayer(element){
	document.getElementById("actions").style.display = "none";
	var id = element.id;
	var localTeam;
	var teamNumber = id.charAt(0);
	if(teamNumber == team1.id){
		localTeam = team1Reserves;
	} else{
		localTeam = team2Reserves;
	}
	var playerId = id.slice(-1);
	console.log(playerId);
	var player;
	for(var i = 0; i < localTeam.length; i++){
		if(localTeam[i].id == playerId){
			player = localTeam[i];
		}
	}
	console.log(player.name);
	if(phase == "yourSetup" && teamNumber == team){
		actionChoice = "place";
		activePlayer = player;
		var possible = squares.getContext("2d");
		possible.save();
		possible.clearRect(0, 0, canvas.width, canvas.height);
		var squareH = canvas.height/15;
		possible.globalAlpha = 0.3;
		if(team == team1.id){
			possible.fillStyle = "blue";
			possible.fillRect(0, 0, 13 * squareH, 16*squareH);
		}else {
			possible.fillStyle = "red";
			possible.fillRect(13 * squareH, 16 * squareH, 26 * squareH, 16*squareH);
		}
		possible.restore();
	}
    showPlayerDetails(player); 
}

function showPlayerDetails(player){
	var team = 1;
	if(player.team != team1.id){
		team = 2;
	}
	document.getElementById("player"+team+"Name").innerHTML = player.name;
	document.getElementById("player"+team+"Type").innerHTML = player.type;
	document.getElementById("player"+team+"MA").innerHTML = player.ma;
	document.getElementById("player"+team+"ST").innerHTML = player.st;
	document.getElementById("player"+team+"AG").innerHTML = player.ag;
	document.getElementById("player"+team+"AV").innerHTML = player.av;
	document.getElementById("player"+team+"AG").innerHTML = player.ag;
	document.getElementById("player"+team+"Img").src = player.imgUrl;
	document.getElementById("player"+team+"Skills").innerHTML ="";
	var skills = player.skills;
	for(var i = 0; i < skills.length; i++){
		var text = skills[i].name;
		if(i != skills.length -1){
			text+=", ";
		}
		document.getElementById("player"+team+"Skills").innerHTML += text;
	}
	document.getElementById("team"+team+"Player").style.display="block";
}

function closePlayer1(){
	document.getElementById("team1Player").style.display="none";
	if(phase == "yourSetup"){
		squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
		if(team == 1){
			activePlayer = null;
		}
	}
}

function closePlayer2(){
	document.getElementById("team2Player").style.display="none";
	if(phase == "yourSetup"){
		squares.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
		if(team == 2){
			activePlayer = null;
		}
	}
}

function actOnsetupClick(click){
	var square = determineSquare(click);
	if(activePlayer != null && actionChoice == "place"){
		stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
                JSON.stringify({"type": "ACTION", "action": "PLACEMENT", "player": activePlayer.id,
	                 "target": square}));
		  activePlayer = null;
		  actionChoice = null;
	}else{
		for(var i = 0 ; i < players.length; i++){
			var player = players[i];
			 if(player.location[0] == square[0] && player.location[1] == square[1]) {
				showPlayerDetails(player);
				activePlayer = player;
				showSetupActions(player)
				return;
			 }
		}			
	}			 
}
		
function showSetupActions(player){
	 resetActions();
	 var actions = document.getElementById("actions");
	 canvas = document.getElementById("canvas");
	 actions.style.display = "block";
	 var squareH = canvas.clientHeight/15;
	 document.getElementById("actionsTitle").innerHTML = "Actions";
     document.getElementById("place").style.display = "inline";
     document.getElementById("remove").style.display = "inline";
	 actions = document.getElementById("actions"); // have to get updated height & width based on number of images shown
	 actions.style.left = ""+ ((player.location[0] * squareH) - actions.offsetWidth/3)  + "px";
	 actions.style.top = "" + (((14- player.location[1]) * squareH) - actions.offsetHeight) + "px";
}

function updateSetup(message){
	 squares.getContext("2d").clearRect(0,0, canvas.width, canvas.height);
	  var squareH = canvas.height/15;
	  console.log("updating setup");
	if(message.description == "1"){
	  team1Reserves = message.team1FullDetails.reserves;
	  populateReserves(1);
	  canvas.getContext("2d").clearRect(0, 0, 13 * squareH, 16*squareH);
	  selection.getContext("2d").clearRect(0, 0, 13 * squareH, 16*squareH);
	  closePlayer1();
	  team1PlayersOnPitch = message.team1FullDetails.playersOnPitch;
	  players.length = 0;
	  for(var i = 0; i < team1PlayersOnPitch.length; i++){
		  drawPlayer(team1PlayersOnPitch[i]);
	  }
	} else{
	  team2Reserves = message.team2FullDetails.reserves;
	  populateReserves(2);
	  canvas.getContext("2d").clearRect(13 * squareH, 16 * squareH, 26 * squareH, 16*squareH);
	  selection.getContext("2d").clearRect(13 * squareH, 16 * squareH, 26 * squareH, 16*squareH);
	  closePlayer2();
	  team2PlayersOnPitch = message.team1FullDetails.playersOnPitch;
	  players.length = 0;
	  for(var i = 0; i < team2PlayersOnPitch.length; i++){
		  drawPlayer(team2PlayersOnPitch[i]);
	  }
	}
	players = team1PlayersOnPitch.concat(team2PlayersOnPitch);
}

function placePlayer(){
	actionChoice = "place";
	var squareH = canvas.height/15;
	document.getElementById("actions").style.display = "none";
	var possible = squares.getContext("2d");
	possible.save();
	possible.globalAlpha = 0.3;
	if(team == team1.id){
		possible.fillStyle = "blue";
		possible.fillRect(0, 0, 13 * squareH, 16*squareH);
	}else {
		possible.fillStyle = "red";
		possible.fillRect(13 * squareH, 16 * squareH, 26 * squareH, 16*squareH);
	}
	possible.restore();
}

function moveToReserves(){
	actionChoice = "bench";
	document.getElementById("actions").style.display = "none";
	stompClient.send("/app/game/gameplay/" + game + "/" + team, {}, 
            JSON.stringify({"type": "ACTION", "action": "BENCH", "player": activePlayer.id}));
	closePlayer1();
	closePlayer2();
	activePlayer = null;
}