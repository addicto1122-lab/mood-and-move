package com.moodandmove.mail.service;

import com.moodandmove.report.dto.MonthlyReport;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationCode(String toEmail, String code) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Mood&Move] 이메일 인증번호 안내");

        message.setText(
                "안녕하세요. Mood&Move입니다.\n\n"
                        + "회원가입 인증번호는 [" + code + "] 입니다.\n\n"
                        + "인증번호는 발급 후 5분 동안 유효합니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시해 주세요."
        );

        mailSender.send(message);
    }

    public void sendMonthlyReport(
            String toEmail,
            String nickname,
            MonthlyReport report
    ) {

        MimeMessage message =
                mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);

            helper.setSubject(
                    "[Mood&Move] "
                            + report.month()
                            + "월 마음 리포트가 도착했어요 🌱"
            );

            helper.setText(
                    buildMonthlyReportHtml(
                            nickname,
                            report
                    ),
                    true
            );

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "월간 리포트 이메일 발송에 실패했습니다.",
                    e
            );
        }
    }

    private String buildMonthlyReportHtml(
            String nickname,
            MonthlyReport report
    ) {

        String safeNickname =
                HtmlUtils.htmlEscape(nickname);

        String emotion =
                report.topEmotionName() == null
                        ? "기록 없음"
                        : HtmlUtils.htmlEscape(
                        report.topEmotionEmoji()
                        + " "
                        + report.topEmotionName()
                );

        String bestAction =
                report.bestActionName() == null
                        ? "아직 재측정된 행동이 없어요."
                        : HtmlUtils.htmlEscape(
                        report.bestActionName()
                )
                          + " <strong style=\"color:#3f765b;\">+"
                          + report.bestActionDelta()
                          + "점</strong>";

        String encouragement =
                HtmlUtils.htmlEscape(
                        report.encouragementMessage()
                );

        return """
        <!DOCTYPE html>
        <html lang="ko">
        <body style="
            margin:0;
            padding:0;
            background:#f4f7f5;
            font-family:Arial, sans-serif;
            color:#23332b;
        ">

        <table width="100%%"
               cellpadding="0"
               cellspacing="0"
               style="padding:40px 16px;">
            <tr>
                <td align="center">

                    <table width="100%%"
                           cellpadding="0"
                           cellspacing="0"
                           style="
                               max-width:560px;
                               background:#ffffff;
                               border-radius:24px;
                               overflow:hidden;
                               box-shadow:0 8px 30px rgba(0,0,0,0.06);
                           ">

                        <tr>
                            <td style="
                                padding:36px 32px;
                                background:#eaf4ed;
                            ">
                                <div style="
                                    font-size:12px;
                                    font-weight:bold;
                                    letter-spacing:2px;
                                    color:#5b806b;
                                ">
                                    MOOD REPORT
                                </div>

                                <h1 style="
                                    margin:10px 0 8px;
                                    font-size:28px;
                                ">
                                    %d년 %d월<br>
                                    마음 리포트
                                </h1>

                                <p style="
                                    margin:0;
                                    color:#6f7d75;
                                    line-height:1.6;
                                ">
                                    %s님의 한 달을 함께 돌아봤어요.
                                </p>
                            </td>
                        </tr>


                        <tr>
                            <td style="padding:28px 32px 10px;">

                                <h2 style="
                                    margin:0 0 16px;
                                    font-size:18px;
                                ">
                                    📝 감정 기록
                                </h2>

                                <table width="100%%">
                                    <tr>
                                        <td style="
                                            padding:16px;
                                            background:#f6faf7;
                                            border-radius:14px;
                                        ">
                                            <div style="color:#839087;font-size:12px;">
                                                작성한 일기
                                            </div>
                                            <strong style="font-size:22px;">
                                                %d일
                                            </strong>
                                        </td>

                                        <td width="12"></td>

                                        <td style="
                                            padding:16px;
                                            background:#f6faf7;
                                            border-radius:14px;
                                        ">
                                            <div style="color:#839087;font-size:12px;">
                                                평균 기분
                                            </div>
                                            <strong style="font-size:22px;">
                                                %s점
                                            </strong>
                                        </td>
                                    </tr>
                                </table>

                                <div style="
                                    margin-top:12px;
                                    padding:16px;
                                    border-radius:14px;
                                    background:#f6faf7;
                                ">
                                    가장 많이 기록한 감정
                                    <strong style="float:right;">
                                        %s
                                    </strong>
                                </div>
                            </td>
                        </tr>


                        <tr>
                            <td style="padding:18px 32px;">

                                <h2 style="
                                    margin:0 0 16px;
                                    font-size:18px;
                                ">
                                    🌿 행동 기록
                                </h2>

                                <div style="
                                    padding:18px;
                                    border-radius:16px;
                                    background:#f6faf7;
                                    line-height:2;
                                ">
                                    실행한 행동
                                    <strong style="float:right;">
                                        %d회
                                    </strong>
                                    <br>

                                    추천 실행률
                                    <strong style="float:right;">
                                        %s%%
                                    </strong>
                                    <br>

                                    긍정적 기분 변화
                                    <strong style="float:right;">
                                        %s%%
                                    </strong>
                                    <br>

                                    평균 기분 변화
                                    <strong style="float:right;color:#3f765b;">
                                        %s점
                                    </strong>
                                </div>
                            </td>
                        </tr>


                        <tr>
                            <td style="padding:10px 32px 20px;">

                                <div style="
                                    padding:20px;
                                    border-radius:18px;
                                    background:#edf7ef;
                                ">
                                    <div style="
                                        font-size:12px;
                                        font-weight:bold;
                                        color:#5b806b;
                                    ">
                                        🏆 BEST ACTION
                                    </div>

                                    <div style="
                                        margin-top:8px;
                                        font-size:18px;
                                        font-weight:bold;
                                    ">
                                        %s
                                    </div>
                                </div>

                            </td>
                        </tr>


                        <tr>
                            <td style="padding:10px 32px 36px;">

                                <div style="
                                    padding:22px;
                                    border-radius:18px;
                                    background:#3f765b;
                                    color:white;
                                ">
                                    <div style="
                                        font-size:12px;
                                        opacity:0.8;
                                        margin-bottom:8px;
                                    ">
                                        🌱 MOOD & MOVE의 한마디
                                    </div>

                                    <div style="
                                        font-size:16px;
                                        line-height:1.7;
                                        font-weight:600;
                                    ">
                                        %s
                                    </div>
                                </div>

                            </td>
                        </tr>


                        <tr>
                            <td style="
                                padding:22px 32px;
                                text-align:center;
                                font-size:11px;
                                color:#9aa39e;
                                background:#fafcfb;
                            ">
                                Mood & Move<br>
                                작은 행동으로 만드는 마음의 변화
                            </td>
                        </tr>

                    </table>

                </td>
            </tr>
        </table>

        </body>
        </html>
        """.formatted(
                report.year(),
                report.month(),
                safeNickname,

                report.diaryCount(),
                report.averageMoodScore(),
                emotion,

                report.executionCount(),
                report.executionRate(),
                report.positiveRate(),
                report.averageDelta(),

                bestAction,

                encouragement
        );
    }
}