window.addEventListener("DOMContentLoaded", function () {
  const form = document.querySelector("form");
  const resetBtn = document.getElementById("resetDefaultImgBtn");
  const previewImg = document.querySelector(".profile-preview");
  const fileInput = document.getElementById("profileImg");

  // 기본이미지 버튼 클릭 시 이미지 초기화
  resetBtn.addEventListener("click", function () {
    previewImg.src = "/images/profile/default-profile.png";
    fileInput.value = "";
  });

  // 프로필 이미지 미리보기
  fileInput.addEventListener("change", function () {
    const file = this.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = function (e) {
        previewImg.src = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  });

  // AJAX로 폼 전송 후 부모창 새로고침 + 팝업 닫기
  form.addEventListener("submit", function (e) {
    e.preventDefault(); // 기본 제출 막기

    const formData = new FormData(form);

    fetch("/mypage/information/profileEdit", {
      method: "POST",
      body: formData,
    })
      .then((response) => {
        if (!response.ok) throw new Error("서버 오류 발생");
        return response.text(); // text() 또는 json()으로 받을 수 있음
      })
      .then(() => {
        // 응답 받은 뒤 부모창 새로고침하고 팝업 닫기
        if (window.opener && !window.opener.closed) {
          window.opener.location.reload();
        }
        window.close();
      })
      .catch((error) => {
        console.error("프로필 수정 실패:", error);
        alert("프로필 수정 중 오류가 발생했습니다.");
      });
  });
});
