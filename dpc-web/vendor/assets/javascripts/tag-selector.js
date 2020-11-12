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