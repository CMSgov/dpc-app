// When dialogs, modals, or any other areas that appear on top
// of the main canvas, we want to trap the focusable areas
// for folks using keyboard navigation so that the focused
// elements are not lost.

function trapFocus(element) {
    var focusableEls = element.querySelectorAll('a[href]:not([disabled]), button:not([disabled]), textarea:not([disabled]), input[type="text"]:not([disabled]), input[type="radio"]:not([disabled]), input[type="checkbox"]:not([disabled]), select:not([disabled]), input[type="submit"]:not([disabled]), iframe'),
        firstFocusableEl = focusableEls[0],
        lastFocusableEl = focusableEls[focusableEls.length - 1],
        KEYCODE_TAB = 9;

    element.addEventListener('keydown', function(e) {
        var isTabPressed = (e.key === 'Tab' || e.keyCode === KEYCODE_TAB);

        if (!isTabPressed) {
            return;
        }

        if ( e.shiftKey ) /* shift + tab */ {
            if (document.activeElement === firstFocusableEl) {
                lastFocusableEl.focus();
                e.preventDefault();
            }
        } else /* tab */ {
            if (document.activeElement === lastFocusableEl) {
                firstFocusableEl.focus();
                e.preventDefault();
            }
        }

    });
}