(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var modalEl = document.getElementById('filter-modal'),
        trigger = document.getElementById('filter-modal-trigger'),
        closeModals = document.getElementsByClassName("ds-c-dialog__close");


if(typeof(trigger) != 'undefined' && trigger != null){
    trigger.addEventListener("click", function(e){
      modalEl.setAttribute('aria-hidden', false);
    },false);

    for (var i = closeModals.length - 1; i >= 0; --i)
      closeModals[i].addEventListener("click", function(e){
        modalEl.setAttribute('aria-hidden', true);
      });}
    });
}());