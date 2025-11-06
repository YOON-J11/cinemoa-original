document.addEventListener("DOMContentLoaded", function () {
  const regionSelectBox = document.getElementById("regionSelectBox");
  const regionOptions = document.getElementById("regionOptions");

  const cinemaSelect = document.getElementById("cinemaSelect");
  const cinemaOptions = document.getElementById("cinemaOptions");
  const cinemaSelectWrapper = document.getElementById("cinemaSelectWrapper");
  const hiddenInput = document.getElementById("preferredCinema");

  const allCinemas = Array.from(document.querySelectorAll(".cinema-option")); // 초기 전체 목록 저장

  // 셀렉트 열기/닫기
  document.querySelectorAll(".custom-select").forEach(select => {
    select.addEventListener("click", function (e) {
      e.stopPropagation();
      closeAllSelects();
      this.parentNode.classList.toggle("open");
    });
  });

  document.addEventListener("click", closeAllSelects);

  function closeAllSelects() {
    document.querySelectorAll(".custom-select-wrapper").forEach(wrapper => {
      wrapper.classList.remove("open");
    });
  }

  // 지역 선택
  document.querySelectorAll(".region-option").forEach(option => {
    option.addEventListener("click", function () {
      const region = this.dataset.id;
      regionSelectBox.textContent = region;
      regionSelectBox.dataset.value = region;

      // 영화관 필터링
      cinemaOptions.innerHTML = ""; // 기존 영화관 목록 제거
      allCinemas.forEach(cinema => {
        if (cinema.dataset.region === region) {
          const cloned = cinema.cloneNode(true);
          cloned.addEventListener("click", function () {
            const name = this.textContent;
            const id = this.dataset.id;
            cinemaSelect.textContent = name;
            hiddenInput.value = id;
            closeAllSelects();
          });
          cinemaOptions.appendChild(cloned);
        }
      });

      cinemaSelect.textContent = "영화관을 선택해주세요";
      hiddenInput.value = "";
      cinemaSelectWrapper.style.display = "block";
      closeAllSelects();
    });
  });

  // 장르 체크박스 제한 (최대 3개)
  document.querySelectorAll('input[name="preferredGenres[]"]').forEach(cb => {
    cb.addEventListener("change", function () {
      const checked = document.querySelectorAll('input[name="preferredGenres[]"]:checked').length;
      if (checked > 3) {
        alert("선호 장르는 최대 3개까지만 선택할 수 있습니다.");
        this.checked = false;
      }
    });
  });

  // 저장 시 확인
  document.querySelector("form").addEventListener("submit", function (e) {
    if (!confirm("변경된 선호관람정보를 저장하시겠습니까?")) {
      e.preventDefault();
    }
  });

  // 초기화 버튼
  document.getElementById("resetBtn").addEventListener("click", function () {
    regionSelectBox.textContent = "지역을 선택해주세요";
    delete regionSelectBox.dataset.value;

    cinemaSelect.textContent = "영화관을 선택해주세요";
    cinemaOptions.innerHTML = "";
    cinemaSelectWrapper.style.display = "none";
    hiddenInput.value = "";

    document.querySelectorAll('input[name="preferredGenres[]"]').forEach(cb => {
      cb.checked = false;
      const label = cb.closest(".genre-item");
      label.classList.remove("checked");
    });
  });


  // 선호 장르 선택 스타일
  document.querySelectorAll(".genre-item input[type='checkbox']").forEach(cb => {
    const label = cb.closest(".genre-item");
    if (cb.checked) label.classList.add("checked");

    cb.addEventListener("change", function () {
      label.classList.toggle("checked", this.checked);
    });
  });


  // 페이지 로드시 기존 선호 영화관 표시
  const savedCinemaId = hiddenInput.value;
  if (savedCinemaId && savedCinemaId.trim() !== "") {
    const matchedCinema = allCinemas.find(c => c.dataset.id === savedCinemaId);
    if (matchedCinema) {
      const region = matchedCinema.dataset.region;
      const cinemaName = matchedCinema.textContent;

      // 지역 세팅
      regionSelectBox.textContent = region;
      regionSelectBox.dataset.value = region;

      // 영화관 목록 초기화 및 이벤트 재부여
      cinemaOptions.innerHTML = "";
      allCinemas.forEach(cinema => {
        if (cinema.dataset.region === region) {
          const cloned = cinema.cloneNode(true);
          cloned.addEventListener("click", function () {
            const name = this.textContent;
            const id = this.dataset.id;
            cinemaSelect.textContent = name;
            hiddenInput.value = id;
            closeAllSelects();
          });
          cinemaOptions.appendChild(cloned);
        }
      });

      // 영화관 선택된 값 세팅
      cinemaSelect.textContent = cinemaName;
      cinemaSelectWrapper.style.display = "block";
    }
  }


});
