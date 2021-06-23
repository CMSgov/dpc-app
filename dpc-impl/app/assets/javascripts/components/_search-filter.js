function searchFunc() {
  var input, filter, ul, li, i, div, txtValue;
  input = document.getElementById('searchInput');
  filter = input.value.toUpperCase();
  ul = document.getElementById('searchList');
  li = ul.getElementsByTagName('li');

  var liDisplayCount = 0;

  for (i = 0; i < li.length; i++) {
    div = li[i].getElementsByTagName('div')[0];

    txtValue = div.textContent || div.innerText;

    if (txtValue.toUpperCase().indexOf(filter) <= -1) {
      li[i].style.display = 'none';
    } else {
      li[i].style.display = '';
      liDisplayCount++
    }
  }

  var numCount = document.getElementById('searchCount')

  if (liDisplayCount == 0) {
    numCount.innerHTML = "There are no results that match your search query."
  } else if (liDisplayCount > 1) {
    numCount.innerHTML = "There are " + liDisplayCount + " results that match your search query."
  } else if (liDisplayCount == 1) {
    numCount.innerHTML = "There is " + liDisplayCount + " result that matches your search query."
  } else {
    numCount.innerHTML = ""
  }
}