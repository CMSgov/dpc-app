function formToggle (command) {
  var user_info, user_form;
  user_info = document.getElementById('user-info')
  user_form = document.getElementById('user-edit-form')

  if (command == 'edit') {
    user_info.style.display = 'none';
    user_form.style.display = '';
  } else {
    user_info.style.display = '';
    user_form.style.display = 'none';
  }
}