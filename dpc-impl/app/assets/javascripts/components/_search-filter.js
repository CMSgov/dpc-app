function searchFunc() {
  var input, filter, ul, li, div, txtValue;
  input = document.getElementById('searchInput');
  filter = input.value.toUpperCase();
  ul = document.getElementById('searchList');
  li = ul.getElementsByTagName('li');

  for (i = 0; i < li.length; i++) {
    div = li[i].getElementsByTagName('div')[0];

    txtValue = div.textContent || div.innerText;

    if (txtValue.toUpperCase().indexOf(filter) <= -1) {
      li[i].style.display = 'none';
    } else {
      li[i].style.display = '';
    }
  }
}