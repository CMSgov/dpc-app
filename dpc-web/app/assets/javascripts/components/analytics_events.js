const siteLogo = document.querySelector('a.site-logo');
siteLogo.addEventListener('click', function(e) {
  utag.link({
    'event_name': 'header_click',
    'link_type': 'link_other',
    'text': siteLogo.text,
    'link_url': siteLogo.href,
  });
});

const loginButton = document.querySelector('#login-button');
loginButton.addEventListener('click', function (e) {
  utag.link({
    'event_name': 'login',
    'link_type': 'link_other',
    'link_url': 'login successful',
  });
});
