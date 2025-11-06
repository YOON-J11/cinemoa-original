document.addEventListener("DOMContentLoaded", () => {
  const IMP = window.IMP;
  if (!IMP) {
    console.error("아임포트 SDK 로딩 실패");
    return;
  }
  IMP.init("imp42813807");

  document.getElementById("pay-button").addEventListener("click", () => {
    IMP.request_pay(
      {
        pg: "html5_inicis",
        pay_method: "card",
        merchant_uid: "order_" + Date.now(),
        name: "cinemoa 영화결제", // 상품명 고정
        amount: 1, // 가격 테스트결제를 위해 1원으로 고정
        buyer_email: buyerEmail || "", // 유저 이메일 (없으면 빈값)
        buyer_name:  buyerName  || "", // 유저 이름 (없으면 빈값)
        buyer_tel: "" // 전화번호 항상 빈 값
      },
      async function (rsp) {
        if (!rsp.success) {
          alert("결제 실패: " + rsp.error_msg);
          return;
        }

        try {
          // 5) 서버 검증 + DB 인서트
          const res = await fetch("/reservation/payment/complete", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              impUid: rsp.imp_uid,
              merchantUid: rsp.merchant_uid,
              paidAmount: rsp.paid_amount, // 아임포트 조회 금액과 대조(=1)
              method: rsp.pay_method,

              movieId,
              showtimeId,
              screenId,
              seatInfo,
              seatIdList
            })
          });

          const text = await res.text();
          if (!res.ok) {
            alert(text || "결제 완료 처리 실패");
            return;
          }

          if (text.startsWith("OK:")) {
            const reservationId = text.split(":")[1];
            alert("결제 완료!");
            location.href = `/reservation/complete?reservationId=${reservationId}`;
          } else {
            console.warn("Unexpected response:", text);
            alert("결제는 완료되었지만 응답 포맷이 예상과 다릅니다.");
          }
        } catch (e) {
          console.error(e);
          alert("서버 통신 오류가 발생했습니다.");
        }
      }
    );
  });
});
