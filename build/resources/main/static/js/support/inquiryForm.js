document.addEventListener("DOMContentLoaded", function () {
  const selectBox = document.getElementById("inquiryTypeSelect");
  const optionsBox = document.getElementById("inquiryTypeOptions");
  const hiddenInput = document.getElementById("selectedCategory");
  const agreeCheckbox = document.querySelector('input[name="agree"]');
  const submitBtn = document.getElementById('submitBtn');

  // 열기/닫기
  selectBox.addEventListener("click", function (e) {
    e.stopPropagation();
    optionsBox.parentNode.classList.toggle("open");
  });

  // 옵션 선택
  optionsBox.querySelectorAll(".option").forEach(option => {
    option.addEventListener("click", function () {
      const selected = this.textContent;
      const value = this.getAttribute("data-value");
      selectBox.textContent = selected;
      hiddenInput.value = value;
      optionsBox.parentNode.classList.remove("open");
    });
  });

  // 외부 클릭 시 닫기
  document.addEventListener("click", function () {
    optionsBox.parentNode.classList.remove("open");
  });

  function toggleSubmit() {
    submitBtn.disabled = !agreeCheckbox.checked;
  }
  agreeCheckbox.addEventListener("change", toggleSubmit);
  if (submitBtn) toggleSubmit();
});
