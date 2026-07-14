package org.dempsay.codereview.review;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Guards follow-up chat input before LLM calls.
 *
 * @since 1.0.0
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 */
public final class ChatInputGuard {

  private static final int MAX_LENGTH = 4_000;
  private static final List<Pattern> INJECTION_PATTERNS = List.of(
      Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior)\\s+instructions"),
      Pattern.compile("(?i)disregard\\s+(the\\s+)?(system|above)\\s+(prompt|instructions)"),
      Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an)\\s+"),
      Pattern.compile("(?i)reveal\\s+(the\\s+)?(system|hidden)\\s+prompt"),
      Pattern.compile("(?i)jailbreak")
  );

  private ChatInputGuard() {
  }

  /**
   * Result of checking chat input.
   *
   * @param accepted whether the input may be sent to the LLM
   * @param message user-facing message when rejected, or empty when accepted
   * @since 1.0.0
   */
  public record GuardResult(boolean accepted, String message) {
  }

  /**
   * Checks whether follow-up chat input is allowed.
   *
   * @param input the input
   * @return the guard result
   * @since 1.0.0
   */
  public static GuardResult check(final String input) {
    if (input == null || input.isBlank()) {
      return rejected("Message is empty.");
    }
    if (input.length() > MAX_LENGTH) {
      return rejected("Message is too long (max " + MAX_LENGTH + " characters). "
          + scopeReminder());
    }
    final String lower = input.toLowerCase(Locale.ROOT);
    for (final Pattern pattern : INJECTION_PATTERNS) {
      if (pattern.matcher(lower).find()) {
        return rejected("That input looks like a prompt-injection attempt. "
            + scopeReminder());
      }
    }
    return new GuardResult(true, "");
  }

  /**
   * Reminder shown when input is rejected.
   *
   * @return the reminder
   * @since 1.0.0
   */
  public static String scopeReminder() {
    return "Follow-up chat is limited to this review's findings and changed files.";
  }

  private static GuardResult rejected(final String message) {
    return new GuardResult(false, message);
  }
}