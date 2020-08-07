function modalPopUp () {
    var modalEl = document.getElementById('filter-modal'),
        trigger = document.getElementById('filter-modal-trigger'),
        closeModals = document.getElementsByClassName("ds-c-dialog__close");

      modalEl.setAttribute('aria-hidden', false);
      closeModals[0].focus();
      trapFocus(modalEl);

    for (var i = closeModals.length - 1; i >= 0; --i)
      closeModals[i].addEventListener("click", function(e){
        trigger.focus();
        modalEl.setAttribute('aria-hidden', true);
      });
};