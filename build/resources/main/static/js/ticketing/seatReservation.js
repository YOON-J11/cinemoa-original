document.addEventListener("DOMContentLoaded", function () {

    const personButtons = document.querySelectorAll(".person-count button");
    const seatElements = document.querySelectorAll(".seat");
    const selectedSeatsList = document.querySelector(".selected-seats ul");
    const priceDisplay = document.querySelector(".price-info p span");

    let selectedSeats = [];
    let selectedPeople = {
        성인: 0,
        청소년: 0,
        경로: 0,
        우대: 0,
    };

    const seatSection = document.querySelector('.seat-selection');
    const isMorning = seatSection.dataset.morning === 'true';
    const isSpecialTheater = seatSection.dataset.special === 'true';

    // 1. 인원 수 증감
    personButtons.forEach((btn) => {
        btn.addEventListener("click", function () {
            const input = this.parentElement.querySelector("input");
            const label = this.closest(".person-selection-inner").querySelector("span").innerText;
            let value = parseInt(input.value);
            if (this.innerText === "+" && value < 10) value++;
            else if (this.innerText === "-" && value > 0) value--;

            input.value = value;
            selectedPeople[label] = value;

            updateSeatLimit();
            updatePrice();
        });
    });

    function getTotalPeopleCount() {
        return Object.values(selectedPeople).reduce((a, b) => a + b, 0);
    }

    // 2. 좌석 선택
    seatElements.forEach((seat) => {
        seat.addEventListener("click", function () {
            if (seat.classList.contains("reserved") || seat.classList.contains("disabled")) return;

            if (seat.classList.contains("accessible") && selectedPeople["우대"] === 0) {
                alert("장애인석은 우대 인원만 선택할 수 있습니다.");
                return;
            }

            const seatLabel = getSeatLabel(seat);
            if (seat.classList.contains("selected")) {
                seat.classList.remove("selected");
                selectedSeats = selectedSeats.filter((s) => s.label !== seatLabel);
            } else {
                if (selectedSeats.length >= getTotalPeopleCount()) {
                    alert("선택한 인원 수보다 많은 좌석을 선택할 수 없습니다.");
                    return;
                }
                selectedSeats.push({
                    label: seatLabel,
                    price: parseInt(seat.dataset.price),
                    isVip: seat.classList.contains("vip")
                });
                seat.classList.add("selected");
            }

            renderSelectedSeats();
            updatePrice();
        });
    });

    function renderSelectedSeats() {
        selectedSeatsList.innerHTML = "";
        selectedSeats.forEach((seat) => {
            const li = document.createElement("li");
            li.textContent = seat.label;
            selectedSeatsList.appendChild(li);
        });
    }

    renderSelectedSeats();

    function getSeatLabel(seat) {
        const row = seat.closest(".row").dataset.row;
        const number = seat.textContent.trim();
        return `${row}${number}`;
    }

    // 3. 가격 계산
    function updatePrice() {
        let total = 0;
        let count = 0;

        const isMorning = document.querySelector(".seat-selection").dataset.isMorning === "true";
        const screenType = document.querySelector(".seat-selection").dataset.screenType;

        for (const type in selectedPeople) {
            const peopleCount = selectedPeople[type];

            for (let i = 0; i < peopleCount; i++) {
                if (count >= selectedSeats.length) break;

                const seatObj = selectedSeats[count];
                const basePrice = seatObj.price;
                let price = basePrice;

                if (isMorning) {
                    price -= 5000; // 조조 할인 (중복불가)
                } else {
                    if (type === "청소년" || type === "경로" || type === "우대") price -= 3000;
                }

                if (screenType === "IMAX") {
                    price += 5000; // 특별관 추가 요금
                }

                total += price;
                count++;
            }
        }

        priceDisplay.textContent = total.toLocaleString();
        sessionStorage.setItem("selectedSeats", JSON.stringify(selectedSeats));
        sessionStorage.setItem("selectedPeople", JSON.stringify(selectedPeople));
        sessionStorage.setItem("totalPrice", total);
    }

    function updateSeatLimit() {
        if (selectedSeats.length > getTotalPeopleCount()) {
            alert("인원 수가 줄어들어 좌석 일부가 선택 해제됩니다.");
            while (selectedSeats.length > getTotalPeopleCount()) {
                const seatObj = selectedSeats.pop();
                const seat = Array.from(seatElements).find((s) => getSeatLabel(s) === seatObj.label);
                seat.classList.remove("selected");
            }
            renderSelectedSeats();
            updatePrice();
        }
    }

    // 좌석 예매 완료 표시
    document.querySelectorAll('.seat.disabled').forEach(seat => {
        seat.innerHTML = "✕";
        seat.style.color = "#999";
        seat.style.fontWeight = "bold";
        seat.style.fontSize = "16px";
    });

    // 초기화
    document.querySelector('.reset-btn').addEventListener('click', () => {
        document.querySelectorAll('.person-count input').forEach(input => input.value = 0);
        for (let key in selectedPeople) selectedPeople[key] = 0;

        document.querySelectorAll('.seat.selected').forEach(seat => seat.classList.remove('selected'));
        selectedSeats = [];
        renderSelectedSeats();
        updatePrice();
    });

    function getSelectedSeatIdList() {
        return selectedSeats.map(seatObj => {
            const seatEl = Array.from(seatElements).find(el => {
                const row = el.closest('.row').dataset.row;
                const number = el.textContent.trim();
                return `${row}${number}` === seatObj.label;
            });
            return seatEl ? Number(seatEl.dataset.seatId) : null;
        }).filter(id => id !== null);
    }

    function redirectToPayment() {
        const totalPrice = sessionStorage.getItem("totalPrice");
        const seatInfo = selectedSeats.map(s => s.label).join(',');
        const seatIds = getSelectedSeatIdList();

        const params = new URLSearchParams();
        params.append("amount", totalPrice);
        params.append("movieId", movieId);
        params.append("showtimeId", showtimeId);
        params.append("screenId", screenId);
        params.append("seatInfo", seatInfo);
        seatIds.forEach(id => params.append("seatIdList", id));

        window.location.href = `/reservation/payment?${params.toString()}`;
    }

    document.querySelector(".navigation .next").addEventListener("click", function () {
        fetch("/checkLogin", {
            method: "GET",
            credentials: "include"
        })
            .then(response => response.json())
            .then(data => {
                if (data.loggedIn) {
                    redirectToPayment();
                } else {
                    document.getElementById("loginRequiredModal").style.display = "flex";
                }
            });
    });

    document.getElementById("loginMemberBtn").addEventListener("click", function () {
        const totalPrice = sessionStorage.getItem("totalPrice");
        const seatInfo = selectedSeats.map(s => s.label).join(',');
        const seatIds = getSelectedSeatIdList();

        const params = new URLSearchParams();
        params.append("amount", totalPrice);
        params.append("movieId", movieId);
        params.append("showtimeId", showtimeId);
        params.append("screenId", screenId);
        params.append("seatInfo", seatInfo);
        seatIds.forEach(id => params.append("seatIdList", id));

        const redirectUrl = `/reservation/payment?${params.toString()}`;
        window.location.href = `/member/login?redirect=${encodeURIComponent(redirectUrl)}`;
    });

    document.getElementById("loginGuestBtn").addEventListener("click", function () {
        const totalPrice = sessionStorage.getItem("totalPrice");
        const seatInfo = selectedSeats.map(s => s.label).join(',');
        const seatIds = getSelectedSeatIdList();

        const params = new URLSearchParams();
        params.append("amount", totalPrice);
        params.append("movieId", movieId);
        params.append("showtimeId", showtimeId);
        params.append("screenId", screenId);
        params.append("seatInfo", seatInfo);
        seatIds.forEach(id => params.append("seatIdList", id));

        const redirectUrl = `/reservation/payment?${params.toString()}`;
        window.location.href = `/guest/login?redirect=${encodeURIComponent(redirectUrl)}`;
    });

    const goLoginBtn = document.getElementById("goLogin");
    if (goLoginBtn) {
        goLoginBtn.addEventListener("click", function () {
            const showtimeId = new URLSearchParams(window.location.search).get("showtimeId");
            const redirectUrl = `/reservation/payment`;

            sessionStorage.setItem("redirectAfterLogin", `/ticketing/seats?showtimeId=${showtimeId}`);
            window.location.href = `/member/login?redirect=${encodeURIComponent(redirectUrl)}`;
        });
    }

});
