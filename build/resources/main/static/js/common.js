document.addEventListener("DOMContentLoaded", () => {
  const menuItems = document.querySelectorAll(".main-nav > ul > li");

  menuItems.forEach((item) => {
    const submenu = item.querySelector(".submenu");

    if (submenu) {
      item.addEventListener("mouseenter", () => {
        submenu.style.display = "block";

        // 전체 서브메뉴를 화면 왼쪽 끝부터 시작하게 조정
        const rect = item.getBoundingClientRect();
        submenu.style.left = `-${rect.left}px`;

        // submenu-inner를 해당 메뉴 항목 가운데로 이동
        const submenuInner = submenu.querySelector(".submenu-inner");
        if (submenuInner) {
          const itemCenter = rect.left + rect.width / 2;
          submenuInner.style.transform = `translateX(calc(${itemCenter}px - 50%))`;
        }
      });

      item.addEventListener("mouseleave", () => {
        submenu.style.display = "none";

        // 리셋
        submenu.style.left = "0";
        const submenuInner = submenu.querySelector(".submenu-inner");
        if (submenuInner) {
          submenuInner.style.transform = "translateX(0)";
        }
      });
    }
  });
});
window.addEventListener("scroll", function () {
  const mainNav = document.getElementById("main-nav");
  const headerTop = document.querySelector(".header-top");
  const triggerPoint = headerTop.offsetHeight; // header-top 높이만큼 스크롤되면 고정

  if (window.scrollY > triggerPoint) {
    mainNav.classList.add("fixed");
  } else {
    mainNav.classList.remove("fixed");
  }
});

// 구현예정 알러트창
  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('a[href="#"]').forEach(link => {
      link.addEventListener('click', function (e) {
        e.preventDefault(); // 기본 동작 막기 (페이지 상단 이동 방지)
        alert('추후 구현 예정입니다.');
      });
    });
  });
