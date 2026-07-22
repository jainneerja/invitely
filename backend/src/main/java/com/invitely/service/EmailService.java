package com.invitely.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${invitely.notifications.email.sendgrid-key:}")
    private String sendgridKey;

    @Value("${invitely.notifications.email.from}")
    private String fromEmail;

    @Value("${invitely.base-url}")
    private String baseUrl;

    // Finite timeouts so a hung SendGrid call cannot block the Kafka consumer
    // thread indefinitely. See issue #2.
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(15))
            .build();
    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    // -------------------------------------------------------
    // Invite created — host gets confirmation + share link
    // -------------------------------------------------------

    public void sendInviteCreatedEmail(String toEmail, String toName,
                                        String eventTitle, String shareUrl,
                                        String inviteId) {
        if (isEmailDisabled()) return;

        String subject = "Your invitation is ready — " + eventTitle;
        String html = buildInviteCreatedHtml(toName, eventTitle, shareUrl, inviteId);
        send(toEmail, toName, subject, html);
    }

    // -------------------------------------------------------
    // Image ready — host gets notified to review and publish
    // -------------------------------------------------------

    public void sendImageReadyEmail(String toEmail, String toName,
                                     String eventTitle, String imageUrl,
                                     String inviteId) {
        if (isEmailDisabled()) return;

        String subject = "✨ Your invite image is ready — " + eventTitle;
        String html = buildImageReadyHtml(toName, eventTitle, imageUrl, inviteId);
        send(toEmail, toName, subject, html);
    }

    // -------------------------------------------------------
    // RSVP submitted — host gets notified of new response
    // -------------------------------------------------------

    public void sendRsvpNotificationEmail(String toEmail, String toName,
                                           String guestName, String rsvpStatus,
                                           String eventTitle, String dashboardUrl) {
        if (isEmailDisabled()) return;

        String emoji = switch (rsvpStatus) {
            case "ATTENDING"     -> "🎉";
            case "NOT_ATTENDING" -> "😔";
            default              -> "🤔";
        };

        String subject = emoji + " " + guestName + " responded to " + eventTitle;
        String html = buildRsvpNotificationHtml(
                toName, guestName, rsvpStatus, eventTitle, dashboardUrl);
        send(toEmail, toName, subject, html);
    }

    // -------------------------------------------------------
    // Guest invite email — share invite with a guest
    // -------------------------------------------------------

    public void sendGuestInviteEmail(String toEmail, String guestName,
                                      String eventTitle, String hostName,
                                      String shareUrl, String rsvpDeadline) {
        if (isEmailDisabled()) return;

        String subject = "You're invited! " + eventTitle;
        String html = buildGuestInviteHtml(
                guestName, eventTitle, hostName, shareUrl, rsvpDeadline);
        send(toEmail, guestName, subject, html);
    }

    // -------------------------------------------------------
    // Core send via SendGrid REST API
    // -------------------------------------------------------

    private void send(String toEmail, String toName, String subject, String html) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendgridKey);

            Map<String, Object> body = Map.of(
                    "personalizations", List.of(Map.of(
                            "to", List.of(Map.of(
                                    "email", toEmail,
                                    "name", toName)))),
                    "from", Map.of(
                            "email", fromEmail,
                            "name", "Invitely"),
                    "subject", subject,
                    "content", List.of(Map.of(
                            "type", "text/html",
                            "value", html))
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    SENDGRID_URL, request, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent to={} subject={}", toEmail, subject);
            } else {
                log.error("SendGrid returned {}. to={}", response.getStatusCode(), toEmail);
            }

        } catch (Exception e) {
            // Never let email failure break the main flow
            log.error("Failed to send email to={} subject={}", toEmail, subject, e);
        }
    }

    // -------------------------------------------------------
    // HTML email templates — inline styles for email client compat
    // -------------------------------------------------------

    private String buildInviteCreatedHtml(String name, String eventTitle,
                                           String shareUrl, String inviteId) {
        return emailWrapper("""
            <h1 style="color:#7F77DD;font-size:24px;margin:0 0 8px;">
                Your invitation is live! 🎉
            </h1>
            <p style="color:#5F5E5A;font-size:15px;margin:0 0 24px;">
                Hi %s, your <strong>%s</strong> invitation has been created.
                Add your AI scene to bring it to life.
            </p>
            <a href="%s/dashboard/%s"
               style="background:linear-gradient(135deg,#7F77DD,#D4537E);
                      color:#fff;padding:12px 28px;border-radius:8px;
                      text-decoration:none;font-weight:600;display:inline-block;
                      margin-bottom:24px;">
                Open your invite →
            </a>
            <p style="color:#888780;font-size:13px;">
                Share link (available after publishing):<br/>
                <a href="%s" style="color:#7F77DD;">%s</a>
            </p>
            """.formatted(name, eventTitle,
                baseUrl, inviteId, shareUrl, shareUrl));
    }

    private String buildImageReadyHtml(String name, String eventTitle,
                                        String imageUrl, String inviteId) {
        return emailWrapper("""
            <h1 style="color:#7F77DD;font-size:24px;margin:0 0 8px;">
                Your invite image is ready ✨
            </h1>
            <p style="color:#5F5E5A;font-size:15px;margin:0 0 16px;">
                Hi %s, the AI has generated a scene for your
                <strong>%s</strong> invitation.
            </p>
            <img src="%s" alt="Your invite image"
                 style="width:100%%;border-radius:12px;margin-bottom:20px;" />
            <a href="%s/dashboard/%s"
               style="background:linear-gradient(135deg,#7F77DD,#D4537E);
                      color:#fff;padding:12px 28px;border-radius:8px;
                      text-decoration:none;font-weight:600;display:inline-block;">
                Review and publish →
            </a>
            """.formatted(name, eventTitle, imageUrl, baseUrl, inviteId));
    }

    private String buildRsvpNotificationHtml(String hostName, String guestName,
                                              String rsvpStatus, String eventTitle,
                                              String dashboardUrl) {
        String statusLabel = switch (rsvpStatus) {
            case "ATTENDING"     -> "is attending 🎉";
            case "NOT_ATTENDING" -> "can't make it 😔";
            default              -> "responded maybe 🤔";
        };

        String statusColor = switch (rsvpStatus) {
            case "ATTENDING"     -> "#1D9E75";
            case "NOT_ATTENDING" -> "#E53E3E";
            default              -> "#EF9F27";
        };

        return emailWrapper("""
            <h1 style="color:#7F77DD;font-size:24px;margin:0 0 8px;">
                New RSVP for %s
            </h1>
            <p style="color:#5F5E5A;font-size:15px;margin:0 0 16px;">
                Hi %s,
            </p>
            <div style="background:#F9F9F8;border-radius:12px;padding:20px;
                        margin-bottom:20px;border-left:4px solid %s;">
                <strong style="font-size:16px;">%s</strong>
                <span style="color:%s;font-size:15px;"> %s</span>
            </div>
            <a href="%s"
               style="background:linear-gradient(135deg,#7F77DD,#D4537E);
                      color:#fff;padding:12px 28px;border-radius:8px;
                      text-decoration:none;font-weight:600;display:inline-block;">
                View all RSVPs →
            </a>
            """.formatted(eventTitle, hostName,
                statusColor, guestName, statusColor, statusLabel,
                dashboardUrl));
    }

    private String buildGuestInviteHtml(String guestName, String eventTitle,
                                         String hostName, String shareUrl,
                                         String rsvpDeadline) {
        return emailWrapper("""
            <h1 style="color:#7F77DD;font-size:24px;margin:0 0 8px;">
                You're invited! 🎊
            </h1>
            <p style="color:#5F5E5A;font-size:15px;margin:0 0 16px;">
                Hi %s, <strong>%s</strong> has invited you to <strong>%s</strong>.
            </p>
            %s
            <a href="%s"
               style="background:linear-gradient(135deg,#7F77DD,#D4537E);
                      color:#fff;padding:14px 32px;border-radius:8px;
                      text-decoration:none;font-weight:600;font-size:16px;
                      display:inline-block;margin-bottom:20px;">
                View invitation &amp; RSVP →
            </a>
            """.formatted(
                guestName, hostName, eventTitle,
                rsvpDeadline != null
                    ? "<p style=\"color:#888780;font-size:13px;\">RSVP by: <strong>"
                        + rsvpDeadline + "</strong></p>"
                    : "",
                shareUrl));
    }

    // -------------------------------------------------------
    // Shared email wrapper — consistent brand style
    // -------------------------------------------------------

    private String emailWrapper(String content) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background:#F3F3F0;font-family:
                         -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center" style="padding:40px 20px;">
                    <table width="560" cellpadding="0" cellspacing="0"
                           style="background:#fff;border-radius:16px;
                                  overflow:hidden;
                                  box-shadow:0 2px 20px rgba(0,0,0,0.07);">
                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#7F77DD,#D4537E);
                                   padding:24px 32px;">
                          <span style="color:#fff;font-size:20px;font-weight:600;">
                            invitely
                          </span>
                        </td>
                      </tr>
                      <!-- Content -->
                      <tr>
                        <td style="padding:32px;">
                          %s
                        </td>
                      </tr>
                      <!-- Footer -->
                      <tr>
                        <td style="padding:20px 32px;
                                   border-top:1px solid #ECEAE3;
                                   background:#F9F9F8;">
                          <p style="color:#888780;font-size:12px;margin:0;">
                            Sent by Invitely · AI-powered animated invitations<br/>
                            <a href="%s" style="color:#7F77DD;">invitely.app</a>
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(content, baseUrl);
    }

    private boolean isEmailDisabled() {
        if (sendgridKey == null || sendgridKey.isBlank()) {
            log.debug("SendGrid key not configured — skipping email");
            return true;
        }
        return false;
    }
}
