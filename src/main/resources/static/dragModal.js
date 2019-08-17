// adapted from https://www.kirupa.com/html5/drag.htm
var modalBox

var dragging = false;
var initialX;
var initialY;
var currentX;
var currentY;
var xOffset = 0;
var yOffset = 0;


function setDraggable(){
modalBox = document.getElementById("modal");
modalBox.addEventListener("mousedown", dragStart);
modalBox.addEventListener("mouseup", dragEnd);
modalBox.addEventListener("mousemove", drag);
}


function dragStart(e){
	dragging = true;
	initialX = e.clientX - xOffset;
	initialY = e.clientY - yOffset;
}

function drag (e){
	if(dragging == true){
		e.preventDefault();
		currentX = e.clientX - initialX;
		currentY = e.clientY - initialY;
		xOffset = currentX;
		yOffset = currentY;
		setTranslate(currentX, currentY, modalBox);
	}
}

function dragEnd(e){
	initialX = currentX;
	initialY = currentY;
	dragging = false;
}

function setTranslate(xPos, yPos, el) {
	  el.style.transform = "translate3d(" + xPos + "px, " + yPos + "px, 0)";
}

function reset(){
	xOffset = 0;
	yOffset = 0;
	
}