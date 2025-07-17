document.addEventListener("DOMContentLoaded", function () {
  const passwordField = document.getElementById("reservationPassword");
  const confirmField = document.getElementById("confirmPassword");
  const form = document.querySelector(".guest-login-form");

  // 숫자만 입력 (최대 4자리)
  [passwordField, confirmField].forEach(input => {
    input.addEventListener("input", function () {
      this.value = this.value.replace(/\D/g, '').slice(0, 4);
    });
  });

  // 비밀번호 일치 확인
  form.addEventListener("submit", function (e) {
    const pw = passwordField.value;
    const confirmPw = confirmField.value;

    if (pw !== confirmPw) {
      e.preventDefault(); // 제출 막기
      alert("예매 비밀번호가 일치하지 않습니다.");
      confirmField.focus();
    }
  });

  // 비회원 로그인 전, 모달창으로 확인
  const loginBtn = document.getElementById("guestLoginBtn");
  const modal = document.getElementById("guestModal");
  const cancelBtn = document.getElementById("modalCancel");
  const confirmBtn = document.getElementById("modalConfirm");

  loginBtn.addEventListener("click", function () {
      const name = form.name.value.trim();
      const phone = form.phone.value.trim();
      const password = form.reservationPassword.value.trim();
      const confirm = form.confirmPassword.value.trim();
 // 비밀번호 일치 여부
  if (password !== confirm) {
    alert("예매 비밀번호가 일치하지 않습니다.");
    confirmField.focus();
    return;
  }
    // 값 세팅
    document.getElementById("modalName").textContent = name;
    document.getElementById("modalPhone").textContent = phone;
    document.getElementById("modalPassword").textContent = password;

    // 모달 보이기
    modal.style.display = "flex";
  });

  cancelBtn.addEventListener("click", function () {
    modal.style.display = "none";
  });

  confirmBtn.addEventListener("click", function () {
    form.submit();
  });

  // 입력값 없을 경우 버튼 비활성화
  const inputs = document.querySelectorAll(".guest-login-form input");

  inputs.forEach(input => {
    input.addEventListener("input", function () {
      const allFilled = Array.from(inputs).every(i => i.value.trim() !== "");
      loginBtn.disabled = !allFilled;

      if (allFilled) {
        loginBtn.classList.remove("disabled");
      } else {
        loginBtn.classList.add("disabled");
      }
    });
  });
});
