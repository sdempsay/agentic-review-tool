package org.dempsay.codereview.review;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.dempsay.codereview.ingest.ChangedFile;
import org.dempsay.codereview.model.ChatModelFactory;
import org.dempsay.codereview.rules.Rule;
public final class ReviewChatOrchestrator {

  private final ReviewSessionContext session;
  private final ChatModel chatModel;
  private final List<ChatMessage> conversation;
  private final ReviewPromptSupplements supplements;

  public ReviewChatOrchestrator(final ReviewSessionContext session, final ReviewPromptSupplements supplements) {
    this.session = session;
    this.chatModel = ChatModelFactory.create(session.config().model(), session.config().maxTokens());
    this.supplements = supplements;
    this.conversation = new ArrayList<>();
    this.conversation.add(SystemMessage.from(
        ReviewChatPromptBuilder.buildOrchestratorSystemPrompt(session.reportText(), supplements.guardrails())
    ));
  }

  public String respond(final String userMessage) {
    final Optional<DelegationTarget> delegation = resolveDelegation(userMessage);
    if (delegation.isPresent()) {
      return delegate(delegation.get(), userMessage);
    }
    return answerFromReport(userMessage);
  }

  static Optional<DelegationTarget> resolveDelegation(
      final String userMessage,
      final List<ChangedFile> changedFiles,
      final java.util.Map<String, List<Rule>> classification
  ) {
    return FileReferenceMatcher.findReferencedFile(userMessage, changedFiles)
        .map(file -> toDelegationTarget(file, classification));
  }

  private Optional<DelegationTarget> resolveDelegation(final String userMessage) {
    return resolveDelegation(userMessage, session.changedFiles(), session.classification());
  }

  private static DelegationTarget toDelegationTarget(
      final ChangedFile file,
      final java.util.Map<String, List<Rule>> classification
  ) {
    final List<Rule> matchedRules = classification.getOrDefault(file.path(), List.of());
    if (matchedRules.isEmpty()) {
      return DelegationTarget.general(file);
    }
    return DelegationTarget.forRule(matchedRules.get(0), file);
  }

  private String delegate(final DelegationTarget target, final String userMessage) {
    final String prompt = target.generalFallback()
        ? ReviewPromptBuilder.buildGeneralFollowUp(target.file(), userMessage, session.reportText(), supplements)
        : ReviewPromptBuilder.buildFollowUp(target.rule(), target.file(), userMessage, session.reportText(), supplements);
    final String response = chatModel.chat(prompt);
    return "[Delegated to " + target.agentName() + " — `" + target.file().path() + "`]"
        + System.lineSeparator()
        + System.lineSeparator()
        + response.trim();
  }

  private String answerFromReport(final String userMessage) {
    conversation.add(UserMessage.from(userMessage));
    final String response = ChatResponseText.extract(chatModel.chat(conversation));
    conversation.add(AiMessage.from(response));
    return response.trim();
  }
}