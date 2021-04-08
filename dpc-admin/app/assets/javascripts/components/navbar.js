var close_mobile_nav_button = document.getElementById("topnav__close-button"),
    open_mobile_nav_button = document.getElementById("topnav__show-button"),
    mobile_nav = document.getElementById("topnav-wrap"),
    navbar = document.getElementById("navbar"),
    overlay = document.createElement("div"),
    nav_open_class = "nav-is-open",
    visible_class = "is-visible",
    overlay_class = "site-overlay";

function removeElementsByClass(className){
  var elements = document.getElementsByClassName(className);
  while(elements.length > 0){
      elements[0].parentNode.removeChild(elements[0]);
  }
}



if(typeof(close_mobile_nav_button) != 'undefined' && close_mobile_nav_button != null){
  close_mobile_nav_button.addEventListener("click", function(e) {
    document.body.classList.remove(nav_open_class);
    mobile_nav.classList.remove(visible_class);
    open_mobile_nav_button.focus();
    removeElementsByClass(overlay_class);
  });
}

if(typeof(open_mobile_nav_button) != 'undefined' && open_mobile_nav_button != null){
  open_mobile_nav_button.addEventListener("click", function(e) {
    document.body.classList.add(nav_open_class);
    mobile_nav.classList.add(visible_class);
    close_mobile_nav_button.focus();
    navbar.insertBefore(overlay, mobile_nav);
    overlay.classList.add(overlay_class);

    setTimeout(function(){
      overlay.classList.add(visible_class);
    }, 100);

    trapFocus(mobile_nav);
  })
}