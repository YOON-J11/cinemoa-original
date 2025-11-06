document.addEventListener("DOMContentLoaded", function () {
  const passwordField = document.getElementById("reservationPassword");
  const confirmField = document.getElementById("confirmPassword");
  const form = document.querySelector(".guest-login-form");
  const loginBtn = document.getElementById("guestLoginBtn");
  const modal = document.getElementById("guestModal");
  const cancelBtn = document.getElementById("modalCancel");
  const confirmBtn = document.getElementById("modalConfirm");
  const inputs = document.querySelectorAll(".guest-login-form input");

  //생년월일입력받기
  function formatWithZero(value, length = 2) {
    return value.padStart(length, '0');
  }

  function updateBirthDate() {
    const year = document.getElementById("birthYear").value.trim();
    const month = document.getElementById("birthMonth").value.trim();
    const day = document.getElementById("birthDay").value.trim();
    const hidden = document.getElementById("birthDate");

    if (year.length === 4 && month.length >= 1 && day.length >= 1) {
      hidden.value = `${year}-${formatWithZero(month)}-${formatWithZero(day)}`;
    } else {
      hidden.value = "";
    }
  }

  // 숫자만 허용하고 자리수 제한
  ["birthYear", "birthMonth", "birthDay"].forEach(id => {
    const input = document.getElementById(id);
    input.addEventListener("input", function () {
      this.value = this.value.replace(/\D/g, '');
      updateBirthDate();
      checkInputs(); // 기존 함수 재사용
    });
  });


  // 🔒 비밀번호, 비밀번호 확인 - 숫자만 4자리 입력
  [passwordField, confirmField].forEach(input => {
    input.addEventListener("input", function () {
      this.value = this.value.replace(/\D/g, '').slice(0, 4);
    });
  });

  // ❌ 비밀번호 불일치 시 제출 막기
  form.addEventListener("submit", function (e) {
    const pw = passwordField.value;
    const confirmPw = confirmField.value;

    if (pw !== confirmPw) {
      e.preventDefault();
      alert("예매 비밀번호가 일치하지 않습니다.");
      confirmField.focus();
    }
  });

  // ✅ 입력값 모두 채워졌는지 확인하는 함수
  function checkInputs() {
    let allFilled = true;
    inputs.forEach(i => {
      if (i.type === "hidden") return;
      if (i.name === "birthDate" && !i.value) allFilled = false;
    });

    loginBtn.disabled = !allFilled;
    loginBtn.classList.toggle("disabled", !allFilled);
  }

  // ⌨️ 입력값이 바뀔 때마다 확인
  inputs.forEach(input => {
    input.addEventListener("input", checkInputs);
  });

  // 🟢 페이지 진입 시 초기 상태 점검
  checkInputs();

  // 📤 로그인 버튼 클릭 → 모달창 표시
  loginBtn.addEventListener("click", function () {
    const name = form.name.value.trim();
    const phone = form.phone.value.trim();
    const password = form.reservationPassword.value.trim();
    const confirm = form.confirmPassword.value.trim();

    if (password !== confirm) {
      alert("예매 비밀번호가 일치하지 않습니다.");
      confirmField.focus();
      return;
    }

    document.getElementById("modalName").textContent = name;
    document.getElementById("modalPhone").textContent = phone;
    document.getElementById("modalPassword").textContent = password;

    modal.style.display = "flex";
  });

  // ❌ 모달 닫기
  cancelBtn.addEventListener("click", function () {
    modal.style.display = "none";
  });

  // ✅ 모달 확인 → 폼 제출
  confirmBtn.addEventListener("click", function () {
    form.submit();
  });
});
