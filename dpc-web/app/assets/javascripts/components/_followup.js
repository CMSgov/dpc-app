var select_with_follow_up = document.querySelectorAll('[data-hide-follow-up]');

if(typeof(select_with_follow_up) != 'undefined' && select_with_follow_up != null){
  for (var i = 0; i < select_with_follow_up.length; i++) {
    select_with_follow_up[i].onchange = function() {
      var index = this.selectedIndex,
          inputValue = this.children[index].value.trim(),
          sibling = this.nextElementSibling,
          hide_options = this.getAttribute("data-hide-follow-up"),
          selected_hide_option = (hide_options.indexOf(inputValue) > -1);

      // If hidden option was selected
      if (inputValue == "" || selected_hide_option) {
        sibling.setAttribute("hidden", "");
      }
      else {
        sibling.removeAttribute("hidden");
      }
    }

    if(select_with_follow_up[i] != select_with_follow_up[i].getAttribute("data-hide-follow-up")) {
      var event = document.createEvent("HTMLEvents");
      event.initEvent('change');
      select_with_follow_up[i].dispatchEvent(event);
    }
  }
}
