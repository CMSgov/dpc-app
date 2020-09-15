function orgSearch() {
  var input, filter, ul, li, i, txtValue;
  input = document.getElementById('orgSearchInput');
  filter = input.value;
  filter = filter.toUpperCase();
  ul = document.getElementById('searchList');
  li = ul.getElementsByTagName('li');

  var liDisplayCount = 0;

  for (i = 0; i < li.length; i++) {
    div = li[i].getElementsByTagName('div')[0];

    txtValue = div.textContent || div.innerText;

    if (txtValue.toUpperCase().indexOf(filter) > -1) {
      li[i].style.display = '';
      liDisplayCount++;
    } else {
      li[i].style.display = 'none';
    }
  }

  var sendMessage = document.getElementById('searchMessage');

  if (liDisplayCount == 0) {
    sendMessage.innerHTML = "There are no results that match your search query."
  } else if (liDisplayCount > 1) {
    sendMessage.innerHTML = "There are " + liDisplayCount + " results that match your search query."
  } else {
    sendMessage.innerHTML = "There is " + liDisplayCount + " result that matches your search query."
  }

  if (filter.length > 0){
    ul.style.display = "";
  } else {
    ul.style.display = "none";
    sendMessage.innerHTML = "";
  }
}