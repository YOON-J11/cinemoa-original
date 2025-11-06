document.addEventListener("DOMContentLoaded", function () {
  const reasonRadios = document.querySelectorAll('input[name="reason"]');
  const otherTextarea = document.getElementById("otherReason");
  const form = document.querySelector("form");
  const passwordInput = document.getElementById("password");

  // 기타 선택 시 textarea 활성화
  reasonRadios.forEach(radio => {
    radio.addEventListener("change", function () {
      if (this.value === "기타") {
        otherTextarea.disabled = false;
        otherTextarea.focus();
      } else {
        otherTextarea.disabled = true;
        otherTextarea.value = ""; // 입력 초기화
      }
    });
  });

  // 폼 제출 시 검사
  form.addEventListener("submit", function (e) {
    const selectedReason = document.querySelector('input[name="reason"]:checked');
    const passwordValue = passwordInput.value.trim();

    if (!selectedReason) {
      alert("탈퇴 사유를 선택해주세요.");
      e.preventDefault();
      return;
    }

    if (passwordValue === "") {
      alert("비밀번호를 입력해주세요.");
      passwordInput.focus();
      e.preventDefault();
      return;
    }
  });
});
