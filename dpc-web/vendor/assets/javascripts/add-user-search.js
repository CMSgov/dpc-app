function orgUserSearch() {
  var input, filter, ul, li, a, i, txtValue;
  input = document.getElementById('orgUserSearch');
  filter = input.value;
  filter = filter.toUpperCase();
  ul = document.getElementById("orgUserList");
  li = ul.getElementsByTagName('li');

  if (filter.length < 3) return;

  for (let i = 0; i < li.length; i++) {
    div = li[i].getElementsByTagName("div")[0];

    txtValue = div.textContent || div.innerText;
    if (txtValue.toUpperCase().indexOf(filter) > -1) {
      li[i].style.display = "";
    } else {
      li[i].style.display = "none";
    }
  }
}