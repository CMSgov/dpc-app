function orgUserSearch() {
  var input, filter, ul, li, a, i, txtValue;
  input = document.getElementById('orgUserSearch');
  filter = input.value;
  filter = filter.toUpperCase();
  ul = document.getElementById("orgUserList");
  li = ul.getElementsByTagName('li');

  var liTotalCount = 0;
  var liDisplayCount = 0;
  var liInvisibleCount = 0;

  for (i = 0; i < li.length; i++) {
    div = li[i].getElementsByTagName("div")[0];

    txtValue = div.textContent || div.innerText;

    liTotalCount++;



    if (txtValue.toUpperCase().indexOf(filter) > -1) {
      li[i].style.display = "";
      liDisplayCount++;
    } else {
      li[i].style.display = "none";
      liInvisibleCount++;
    }
  }

  var sendMessage = document.getElementById('orgUserSearchMessage');

  if (liDisplayCount == 0) {
    sendMessage.innerHTML = "There are no users or ids that match your search."
  } else {
    sendMessage.innerHTML = ""
  }

  if (filter.length > 1) {
    ul.style.display = "";
  } else {
    ul.style.display = "none";
  }
}