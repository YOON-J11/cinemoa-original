document.addEventListener("DOMContentLoaded", function () {
  const tabs = document.querySelectorAll(".tab");
  const panels = document.querySelectorAll(".tab-panel");
  const idFindBtn = document.querySelector('#find-id .btn-find');

  // 탭 전환: 클릭 시 활성 탭과 패널 변경
  tabs.forEach(tab => {
    tab.addEventListener("click", function () {
      tabs.forEach(t => t.classList.remove("active"));
      panels.forEach(p => p.classList.remove("active"));

      this.classList.add("active");
      const targetId = this.getAttribute("data-tab");
      const targetPanel = document.getElementById(targetId);
      if (targetPanel) {
        targetPanel.classList.add("active");
      }
    });
  });

  // 아이디 찾기 버튼 클릭: 이름과 이메일로 아이디 조회
  idFindBtn.addEventListener("click", function () {
    const name = document.querySelector('#find-id input[name="name"]').value.trim();
    const email = document.querySelector('#find-id input[name="email"]').value.trim();

    if (!name || !email) {
      alert("이름과 이메일을 입력해주세요.");
      return;
    }

    // 서버에 POST 요청 (폼 전송 방식)
    fetch('/member/find-id', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ name, email })
    })
    .then(res => {
      if (!res.ok) throw new Error("일치하는 회원 정보를 찾을 수 없습니다.");
      return res.json();
    })
    .then(data => {
      document.getElementById("resultIdText").innerHTML =
        `회원님의 아이디는 [ <span style="color: #004f59; font-weight: bold; font-size: 16px;">${data.memberId}</span> ] 입니다.`;
      document.getElementById("resultJoinDate").textContent = data.regDate;
      document.getElementById("idResultModal").classList.remove("hidden");
    })
    .catch(err => {
      alert(err.message);
    });
  });

  // 아이디 찾기 결과 모달 닫기 버튼 이벤트
  document.querySelector(".modal-close").addEventListener("click", () => {
    document.getElementById("idResultModal").classList.add("hidden");
  });
  document.querySelector(".btn-confirm").addEventListener("click", () => {
    document.getElementById("idResultModal").classList.add("hidden");
  });

  // 비밀번호 찾기 관련 요소
  const memberIdInput = document.querySelector('#pw-memberId');
  const nameInput = document.querySelector('#pw-name');
  const emailInput = document.querySelector('#pw-email');
  const sendCodeBtn = document.querySelector('#pw-send-code');
  const authArea = document.querySelector('#pw-auth-area');
  const resendBtn = document.querySelector('#pw-resendBtn');
  const verifyBtn = document.querySelector('#pw-verifyBtn');
  const timerSpan = document.querySelector('#pw-timer');
  const authCodeInput = document.querySelector('#pw-authCode');
  const modal = document.querySelector('#pwResetModal');
  const modalClose = modal.querySelector('.modal-close');
  const pwResetForm = document.querySelector('#pwResetForm');
  const pwResetMemberIdInput = document.querySelector('#pwResetMemberId');

  let authTimer = null;
  let timeLeft = 300;

  // 입력값 유효성 검사: 아이디, 이름, 이메일 유효할 때 인증번호 전송 버튼 활성화
  function validateInputs() {
    const memberId = memberIdInput.value.trim();
    const name = nameInput.value.trim();
    const email = emailInput.value.trim();
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    sendCodeBtn.disabled = !(memberId && name && emailRegex.test(email));
  }
  [memberIdInput, nameInput, emailInput].forEach(input => {
    input.addEventListener('input', validateInputs);
  });

  // 인증번호 타이머 시작 (5분)
  function startTimer() {
    clearInterval(authTimer);
    timeLeft = 300;
    updateTimerUI();
    authTimer = setInterval(() => {
      timeLeft--;
      updateTimerUI();
      if (timeLeft <= 0) {
        clearInterval(authTimer);
        alert('인증 시간이 만료되었습니다. 재전송해주세요.');
      }
    }, 1000);
  }

  // 타이머 UI 업데이트
  function updateTimerUI() {
    const minutes = String(Math.floor(timeLeft / 60)).padStart(2, '0');
    const seconds = String(timeLeft % 60).padStart(2, '0');
    timerSpan.textContent = `${minutes}:${seconds}`;
  }

  // 인증번호 전송 버튼 클릭 이벤트
  sendCodeBtn.addEventListener('click', function () {
    const data = {
      memberId: memberIdInput.value.trim(),
      name: nameInput.value.trim(),
      email: emailInput.value.trim()
    };

    // 버튼 비활성화 + 로딩 이미지 추가
    sendCodeBtn.disabled = true;
    const loadingImg = document.createElement('img');
    loadingImg.src = '/images/icons/loading.gif';
    loadingImg.alt = 'loading';
    loadingImg.style.width = '18px';
    loadingImg.style.height = '18px';
    loadingImg.style.verticalAlign = 'middle';
    loadingImg.style.marginLeft = '10px';
    loadingImg.classList.add('loading-icon');
    sendCodeBtn.parentNode.insertBefore(loadingImg, sendCodeBtn.nextSibling);

    fetch('/member/email/send-password-auth-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    .then(res => res.text())
    .then(res => {

    // 로딩 이미지 제거
    const loadingIcon = document.querySelector('.loading-icon');
    if (loadingIcon) loadingIcon.remove();

      if (res === 'success') {
        alert('인증번호가 전송되었습니다.');
        authArea.classList.remove('hidden');
        sendCodeBtn.style.display = 'none'; // 전송 버튼 숨김
        resendBtn.style.display = 'inline-block'; // 재전송 버튼 보임
        startTimer();
      } else {
        alert('입력하신 정보가 일치하지 않습니다.');
        sendCodeBtn.disabled = false; // 실패 시 다시 활성화
      }
    });
  });

  // 인증번호 재전송 버튼 클릭 이벤트
  resendBtn.addEventListener('click', () => {
    const data = {
      memberId: memberIdInput.value.trim(),
      name: nameInput.value.trim(),
      email: emailInput.value.trim()
    };

    // 재전송 버튼 비활성화
    resendBtn.disabled = true;

    // GIF 추가
    const loadingImg = document.createElement('img');
    let loadingIcon = document.querySelector('.resend-loading-icon');
    if (!loadingIcon) {
      loadingIcon = document.createElement('img');
      loadingIcon.src = '/images/icons/loading.gif';
      loadingIcon.alt = 'loading';
      loadingIcon.style.width = '18px';
      loadingIcon.style.verticalAlign = 'middle';
      loadingIcon.style.marginLeft = '10px';
      loadingIcon.classList.add('resend-loading-icon');
      verifyBtn.parentNode.insertBefore(loadingIcon, verifyBtn.nextSibling);
    }

    fetch('/member/email/send-password-auth-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    .then(res => res.text())
    .then(res => {
      // GIF 제거
      if (loadingIcon) loadingIcon.remove();

      if (res === 'success') {
        alert('인증번호가 재전송되었습니다.');
        startTimer();
      } else {
        alert('입력하신 정보가 일치하지 않습니다.');
      }

      // 재전송 버튼 다시 활성화
      resendBtn.disabled = false;
    })
    .catch(() => {
      // 에러 발생해도 버튼 활성화 및 GIF 제거
      if (loadingIcon) loadingIcon.remove();
      resendBtn.disabled = false;
    });
  });



  // 인증번호 확인 버튼 클릭 이벤트
  verifyBtn.addEventListener('click', () => {
    const authCode = authCodeInput.value.trim();
    const email = emailInput.value.trim();

    fetch('/member/email/verify-auth-code', {  // URL 반드시 서버 컨트롤러 경로와 맞춰야 함
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, authCode })
    })
    .then(res => res.text())
    .then(res => {
      if (res === 'success') {
        clearInterval(authTimer);
        alert('인증이 완료되었습니다.');
        pwResetMemberIdInput.value = memberIdInput.value.trim();
        modal.classList.remove('hidden');
      } else {
        alert('인증번호가 일치하지 않습니다.');
      }
    });
  });

  // 비밀번호 재설정 모달 닫기 버튼 이벤트
  modalClose.addEventListener('click', () => {
    modal.classList.add('hidden');
  });

  // 비밀번호 재설정 폼 제출 이벤트
  pwResetForm.addEventListener('submit', function (e) {
    e.preventDefault();
    const newPassword = pwResetForm.newPassword.value.trim();
    const confirmPassword = pwResetForm.confirmPassword.value.trim();
    const memberId = pwResetMemberIdInput.value;

    if (newPassword !== confirmPassword) {
      alert('비밀번호가 일치하지 않습니다.');
      return;
    }

    fetch('/api/member/updatePassword', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ memberId, newPassword })
    })
    .then(res => res.text())
    .then(res => {
      if (res === 'success') {
        alert('비밀번호가 변경되었습니다. 로그인 해주세요.');
        modal.classList.add('hidden');
        location.href = '/member/login';
      } else {
        alert('비밀번호 변경에 실패했습니다.');
      }
    });
  });
});
