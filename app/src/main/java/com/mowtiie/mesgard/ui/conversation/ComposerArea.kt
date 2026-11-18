package com.mowtiie.mesgard.ui.conversation

import androidx.compose.runtime.Composable
import com.mowtiie.mesgard.domain.model.MentionCandidate
import com.mowtiie.mesgard.ui.common.readOnlyMessageResource

/** Under the messages: the edit bar while a message is changed, otherwise the composer or why there is none. */
@Composable
internal fun ComposerArea(
    uiState: ConversationUiState,
    edit: MessageEditUiState,
    editActions: EditActions,
    reply: ReplyTarget?,
    onReplyDropped: () -> Unit,
    mentions: MentionUiState,
    onMentionSelected: (MentionCandidate) -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onAttachmentsPicked: (List<PickedAttachment>) -> Unit,
    onAttachmentRemoved: (String) -> Unit,
) {
    if (edit.isEditing) {
        EditComposer(edit, editActions)
    } else if (uiState.canPost) {
        MentionSuggestions(mentions.suggestions, onMentionSelected)
        reply?.let { ReplyBar(it, onDrop = onReplyDropped) }
        MessageComposer(
            draft = uiState.draft,
            attachments = uiState.attachments,
            canSend = uiState.canSend,
            onDraftChanged = onDraftChanged,
            onSendClicked = onSendClicked,
            onAttachmentsPicked = onAttachmentsPicked,
            onAttachmentRemoved = onAttachmentRemoved,
        )
    } else {
        ReadOnlyNotice(uiState.type.readOnlyMessageResource())
    }
}
