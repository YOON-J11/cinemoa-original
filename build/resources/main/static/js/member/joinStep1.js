document.addEventListener('DOMContentLoaded', function () {
    //잘못된 접근 차단
    const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('error') === 'unauthorized') {
            alert('잘못된 접근입니다. 메일 인증부터 진행해주세요.');
        }
    // === STEP0: 요소 참조 ===
    const startBtn = document.getElementById('startEmailBtn');
    const emailForm = document.getElementById('emailForm');
    const sendCodeBtn = document.getElementById('sendCodeBtn');
    const verifySection = document.getElementById('verifySection');
    const emailInput = document.getElementById('email');
    const timerDisplay = document.getElementById('timer');
    const emailError = document.getElementById('emailError');
    const resendBtn = document.getElementById('resendBtn');
    const authCodeInput = document.getElementById('authCode');
    const confirmBtn = document.querySelector('#verifySection button[type="submit"]');

    let timer;
    let timeLeft = 300; // 5분

    // === STEP1: "이메일 인증하기" 버튼 클릭 시 인증 폼 보여주기 ===
    startBtn.addEventListener('click', () => {
        startBtn.parentElement.classList.add('hidden');
        emailForm.classList.remove('hidden');
    });

    // === STEP2: 이메일 유효성 검사 ===
    emailInput.addEventListener('input', () => {
        const email = emailInput.value.trim();
        const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
        sendCodeBtn.disabled = !isValid;
        emailError.style.display = isValid ? 'none' : 'block';
    });

    // === STEP3: 인증번호 전송 요청 함수 (전송/재전송 공통) ===
    function sendAuthCodeRequest(eventTarget) {
        const email = emailInput.value.trim();
        if (!email) return;

        sendCodeBtn.disabled = true;
        resendBtn.disabled = true;

        // 기존 스피너 제거
        const oldSpinner = document.querySelector('.btn-spinner');
        if (oldSpinner) oldSpinner.remove();

        // 새로운 스피너 생성
        const spinner = document.createElement('span');
        spinner.className = 'btn-spinner';
        spinner.innerHTML = `<img src="/images/icons/loading.gif" alt="loading" style="width:18px; vertical-align: middle; margin-left: 10px;">`;

        eventTarget.parentElement.appendChild(spinner); // 버튼 옆에 추가

        fetch('/member/send-auth-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ email })
        })
        .then(res => res.json())
        .then(data => {
            spinner.remove();
            if (data.status === 'success') {
                resetAuthUI();
                sendCodeBtn.remove(); // 최초 전송 버튼은 제거
            } else {
                alert("인증번호 전송에 실패했습니다.");
            }
            resendBtn.disabled = false;
        })
        .catch(() => {
            alert("에러가 발생했습니다.");
            spinner.remove();
            sendCodeBtn.disabled = false;
            resendBtn.disabled = false;
        });
    }

    // === STEP4: 인증 UI 초기화 및 타이머 시작 ===
    function resetAuthUI() {
        verifySection.classList.remove('hidden');
        authCodeInput.disabled = false;
        confirmBtn.style.display = 'inline-block';
        confirmBtn.disabled = false;
        confirmBtn.textContent = '인증 확인';

        // 타이머 초기화
        timeLeft = 300;
        startTimer();
    }

    // === STEP5: 버튼 이벤트 연결 ===
    sendCodeBtn.addEventListener('click', async () => {
        const email = emailInput.value.trim();

        // 이메일 중복 체크 먼저 실행
        const res = await fetch("/member/check-email", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "email=" + encodeURIComponent(email)
        });

        const isAvailable = await res.json();

        if (!isAvailable) {
            emailError.textContent = "이미 가입된 이메일입니다.";
            emailError.style.display = "block";
            return;
        }

        emailError.style.display = "none";

        // 중복이 아니면 인증 요청 진행
        sendAuthCodeRequest(sendCodeBtn);
    });

    resendBtn.addEventListener('click', () => sendAuthCodeRequest(resendBtn));

    // === STEP6: 인증번호 확인 후 결과 메시지 출력 ===
    confirmBtn.addEventListener('click', () => {
        const code = authCodeInput.value.trim();
        const email = emailInput.value.trim();

        fetch('/member/verify-auth-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ email, code })
        })
        .then(res => res.json())
        .then(data => {
            let resultMsg = document.getElementById('verifyResult');
            if (!resultMsg) {
                resultMsg = document.createElement('div');
                resultMsg.id = 'verifyResult';
                resultMsg.style.marginTop = '8px';
                authCodeInput.parentElement.appendChild(resultMsg);
            }

            if (data.status === 'verified') {
                resultMsg.textContent = '✅ 인증이 완료되었습니다.';
                resultMsg.style.color = 'green';

                confirmBtn.textContent = '다음 단계';
                confirmBtn.disabled = false;

                // 다음단계 버튼이 곧바로 전송되게 하려면 form 제출 처리 추가 가능
                confirmBtn.onclick = () => emailForm.submit();
            } else {
                resultMsg.textContent = '❌ 인증번호가 올바르지 않습니다.';
                resultMsg.style.color = 'red';
            }
        });
    });

    // === STEP7: 타이머 시작 ===
    function startTimer() {
        clearInterval(timer);
        updateTimerDisplay();

        timer = setInterval(() => {
            timeLeft--;
            if (timeLeft <= 0) {
                clearInterval(timer);
                timerDisplay.textContent = '만료됨';
                authCodeInput.disabled = true;
                confirmBtn.disabled = true;
                confirmBtn.textContent = "시간초과";
            } else {
                updateTimerDisplay();
            }
        }, 1000);
    }

    // === STEP8: 타이머 숫자 표시 ===
    function updateTimerDisplay() {
        const min = String(Math.floor(timeLeft / 60)).padStart(2, '0');
        const sec = String(timeLeft % 60).padStart(2, '0');
        timerDisplay.textContent = `${min}:${sec}`;
        timerDisplay.style.color = 'red';
    }
});