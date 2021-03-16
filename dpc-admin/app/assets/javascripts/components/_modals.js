(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var modalEl = document.getElementById('filter-modal'),
        trigger = document.getElementById('filter-modal-trigger'),
        closeModals = document.getElementsByClassName("ds-c-dialog__close");


if(typeof(trigger) != 'undefined' && trigger != null){
    trigger.addEventListener("click", function(e){
      modalEl.setAttribute('aria-hidden', false);
      closeModals[0].focus();
      trapFocus(modalEl);
    },false);

    for (var i = closeModals.length - 1; i >= 0; --i)
      closeModals[i].addEventListener("click", function(e){
        trigger.focus();
        modalEl.setAttribute('aria-hidden', true);
      });}
    });
}());

function tagSelector(check_box_id, container_id) {
  var checkbox = document.getElementById(check_box_id),
      container = document.getElementById(container_id);

  if (checkbox.checked == false) {
    checkbox.checked = true;
    container.classList.add('site-pills__item--inverse');
  } else {
    checkbox.checked = false;
    container.classList.remove('site-pills__item--inverse');
  }
}