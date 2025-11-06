function openProfileEditPopup() {
  const popupWidth = 450;
  const popupHeight = 600;

  const left = (window.screen.width / 2) - (popupWidth / 2);
  const top = (window.screen.height / 2) - (popupHeight / 2);

  window.open(
    '/mypage/profileEditPopup',
    '프로필수정팝업',
    `width=${popupWidth},height=${popupHeight},left=${left},top=${top},resizable=no`
  );
}

document.addEventListener("DOMContentLoaded", function () {

});